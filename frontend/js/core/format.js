/**
 * SlopeGuard — Formatting & Internationalization (format.js)
 * Formats timestamps to IST, calculates human relative time, coordinates, and telemetry units
 */

window.LW = window.LW || {};

window.LW.format = {
  /**
   * Format ISO UTC date string into Indian Standard Time (IST)
   * Example: "09 Oct 2026, 14:30 IST"
   */
  dateTimeIST(isoString) {
    if (!isoString) return "N/A";
    try {
      const date = new Date(isoString);
      if (isNaN(date.getTime())) return "Invalid date";

      const options = {
        timeZone: "Asia/Kolkata",
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
      };
      const formatted = new Intl.DateTimeFormat("en-IN", options).format(date);
      return `${formatted} IST`;
    } catch (e) {
      return isoString;
    }
  },

  /**
   * Return relative time (e.g., "5 min ago", "2 h ago", "yesterday")
   */
  relativeTime(isoString) {
    if (!isoString) return "";
    try {
      const date = new Date(isoString);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffSec = Math.floor(diffMs / 1000);
      const diffMin = Math.floor(diffSec / 60);
      const diffHour = Math.floor(diffMin / 60);
      const diffDay = Math.floor(diffHour / 24);

      if (diffSec < 60) return "just now";
      if (diffMin < 60) return `${diffMin} min ago`;
      if (diffHour < 24) return `${diffHour} h ago`;
      if (diffDay === 1) return "yesterday";
      if (diffDay < 7) return `${diffDay} d ago`;
      return this.dateTimeIST(isoString);
    } catch (e) {
      return "";
    }
  },

  /**
   * Format geographic coordinates
   * Example: "11.4102° N, 76.6950° E"
   */
  coordinates(lat, lng) {
    if (lat === undefined || lng === undefined || lat === null || lng === null) return "N/A";
    const latDir = lat >= 0 ? "N" : "S";
    const lngDir = lng >= 0 ? "E" : "W";
    return `${Math.abs(lat).toFixed(4)}° ${latDir}, ${Math.abs(lng).toFixed(4)}° ${lngDir}`;
  },

  /**
   * Format rainfall telemetry
   */
  rainfall(mm) {
    if (mm === undefined || mm === null) return "N/A";
    return `${Number(mm).toFixed(1)} mm`;
  },

  /**
   * Format slope gradient
   */
  slope(degrees) {
    if (degrees === undefined || degrees === null) return "N/A";
    return `${Number(degrees).toFixed(1)}°`;
  },

  /**
   * Format elevation
   */
  elevation(meters) {
    if (meters === undefined || meters === null) return "N/A";
    return `${Math.round(meters)} m AMSL`;
  }
};
