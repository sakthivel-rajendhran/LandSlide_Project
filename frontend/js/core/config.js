/**
 * SlopeGuard — Core Configuration (config.js)
 * Single place for runtime config, API endpoints, tile providers and app state
 */

window.LW = window.LW || {};

window.LW.config = {
  appVersion: "1.0.0",
  // Target live backend on port 8080 dynamically matching current host
  apiBase: (function() {
    if (typeof window === "undefined" || !window.location) return "http://localhost:8080/api";
    // Allow explicit runtime injection via script or meta tag
    if (window.SLOPEGUARD_API_URL) return window.SLOPEGUARD_API_URL;
    if (window.location.port === "8080") return "/api";
    const host = window.location.hostname || "localhost";
    if (host === "localhost" || host === "127.0.0.1") {
      return `http://${host}:8080/api`;
    }
    // Production Cloud default: points to hosted backend service (e.g. Render / Railway)
    return window.location.origin.includes("vercel.app") || window.location.origin.includes("netlify.app")
      ? (window.SLOPEGUARD_API_URL || "https://slopeguard-api.onrender.com/api")
      : `http://${host}:8080/api`;
  })(),
  useMock: false, // Live persistence against Spring Boot & MySQL

  tileProvider: {
    url: "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
    maxZoom: 19,
    attribution: '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap contributors</a>'
  },

  // Limits in minutes for data freshness calculations
  freshnessThresholds: {
    delayedAfterMinutes: 180, // 3 hours
    unavailableAfterMinutes: 720 // 12 hours
  },

  defaultCenter: [25.7500, 92.8000], // North Eastern Region (NER Corridor)
  defaultZoom: 7,
  nerCenter: [25.7500, 92.8000],
  westernGhatsCenter: [11.4102, 76.6950]
};
