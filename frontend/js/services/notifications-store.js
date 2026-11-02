/**
 * SlopeGuard — Offline Notifications Store (notifications-store.js)
 * Manages cached alerts in IndexedDB 'alerts' store, deduplication by (id, version),
 * and calculates live unread count.
 */

window.LW = window.LW || {};

window.LW.notifications = (function() {
  async function upsertAlert(alert) {
    if (!alert || !alert.id) return;
    const store = window.LW.store;
    if (!store) return;

    const existing = await store.get("alerts", alert.id);
    if (!existing || (alert.version && alert.version > (existing.version || 0))) {
      await store.put("alerts", {
        ...alert,
        is_read: existing ? existing.is_read : (alert.is_read || false),
        received_at: new Date().toISOString()
      });
    }
  }

  async function getUnreadCount() {
    const store = window.LW.store;
    if (!store) return 0;
    const alerts = await store.list("alerts");
    return alerts.filter(a => !a.is_read).length;
  }

  async function getLatest(limit = 10) {
    const store = window.LW.store;
    if (!store) return [];
    const alerts = await store.list("alerts");
    return alerts
      .sort((a, b) => new Date(b.published_at || b.received_at) - new Date(a.published_at || a.received_at))
      .slice(0, limit);
  }

  async function markAsRead(alertId) {
    const store = window.LW.store;
    if (!store) return;
    const alert = await store.get("alerts", alertId);
    if (alert) {
      alert.is_read = true;
      await store.put("alerts", alert);

      // Queue in outbox
      await store.put("outbox", {
        type: "read",
        alert_id: alertId,
        at: new Date().toISOString()
      });

      const unread = await getUnreadCount();
      document.dispatchEvent(new CustomEvent("lw:notifications-changed", { detail: { unread } }));
    }
  }

  async function sync() {
    if (window.LW.sync && typeof window.LW.sync.trigger === "function") {
      return window.LW.sync.trigger();
    }
  }

  return {
    upsertAlert,
    getUnreadCount,
    getLatest,
    markAsRead,
    sync
  };
})();
