/**
 * SlopeGuard — Local Storage Engine (store.js)
 * IndexedDB wrapper for database 'slopeguard' with stores: alerts, meta, outbox
 */

window.LW = window.LW || {};

window.LW.store = (function() {
  const DB_NAME = "slopeguard";
  const DB_VERSION = 1;
  let dbPromise = null;

  function openDB() {
    if (dbPromise) return dbPromise;

    dbPromise = new Promise((resolve, reject) => {
      if (!window.indexedDB) {
        console.warn("[LW.store] IndexedDB not supported; using in-memory fallback");
        resolve(null);
        return;
      }

      const req = indexedDB.open(DB_NAME, DB_VERSION);

      req.onupgradeneeded = (e) => {
        const db = e.target.result;

        // 1. Alerts store (id primary key)
        if (!db.objectStoreNames.contains("alerts")) {
          const alertStore = db.createObjectStore("alerts", { keyPath: "id" });
          alertStore.createIndex("is_read", "is_read", { unique: false });
          alertStore.createIndex("published_at", "published_at", { unique: false });
          alertStore.createIndex("severity", "severity", { unique: false });
          alertStore.createIndex("region_id", "region_id", { unique: false });
        }

        // 2. Metadata store (name primary key)
        if (!db.objectStoreNames.contains("meta")) {
          db.createObjectStore("meta", { keyPath: "name" });
        }

        // 3. Outbox store (auto-incrementing autoId)
        if (!db.objectStoreNames.contains("outbox")) {
          db.createObjectStore("outbox", { keyPath: "autoId", autoIncrement: true });
        }
      };

      req.onsuccess = () => resolve(req.result);
      req.onerror = () => {
        console.error("[LW.store] Failed to open IndexedDB", req.error);
        reject(req.error);
      };
    });

    return dbPromise;
  }

  // Fallback in-memory map if IndexedDB is disabled/unavailable in private browsing
  const memoryStore = {
    alerts: new Map(),
    meta: new Map(),
    outbox: new Map()
  };

  return {
    async get(storeName, key) {
      const db = await openDB();
      if (!db) return memoryStore[storeName]?.get(key) || null;

      return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, "readonly");
        const store = tx.objectStore(storeName);
        const req = store.get(key);
        req.onsuccess = () => resolve(req.result || null);
        req.onerror = () => reject(req.error);
      });
    },

    async put(storeName, record) {
      const db = await openDB();
      record.savedAt = new Date().toISOString();

      if (!db) {
        const key = record.id || record.name || Date.now();
        memoryStore[storeName]?.set(key, record);
        return record;
      }

      return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, "readwrite");
        const store = tx.objectStore(storeName);
        const req = store.put(record);
        req.onsuccess = () => resolve(record);
        req.onerror = () => reject(req.error);
      });
    },

    async delete(storeName, key) {
      const db = await openDB();
      if (!db) {
        memoryStore[storeName]?.delete(key);
        return true;
      }

      return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, "readwrite");
        const store = tx.objectStore(storeName);
        const req = store.delete(key);
        req.onsuccess = () => resolve(true);
        req.onerror = () => reject(req.error);
      });
    },

    async list(storeName) {
      const db = await openDB();
      if (!db) return Array.from(memoryStore[storeName]?.values() || []);

      return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, "readonly");
        const store = tx.objectStore(storeName);
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => reject(req.error);
      });
    },

    async clear(storeName) {
      const db = await openDB();
      if (!db) {
        memoryStore[storeName]?.clear();
        return true;
      }

      return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, "readwrite");
        const store = tx.objectStore(storeName);
        const req = store.clear();
        req.onsuccess = () => resolve(true);
        req.onerror = () => reject(req.error);
      });
    }
  };
})();
