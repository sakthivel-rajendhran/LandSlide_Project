/**
 * SlopeGuard — Client Route & Role Guard (guard.js)
 * Protects authenticated routes and enforces role-based dashboard routing:
 * - CITIZEN -> dashboard.html
 * - RESCUE_FORCE / OFFICER -> rescue-dashboard.html
 * - ADMIN -> admin/dashboard.html
 */

window.LW = window.LW || {};

window.LW.guard = (function() {
  function getCurrentUser() {
    try {
      const stored = sessionStorage.getItem("sg_user") || localStorage.getItem("sg_user");
      if (!stored) return null;
      const user = JSON.parse(stored);
      if (user && user.role) user.role = String(user.role).toUpperCase();
      return user;
    } catch (e) {
      return null;
    }
  }

  function isInAdmin() {
    return window.location.pathname.includes("/admin/") || window.location.pathname.startsWith("admin/");
  }

  function getTargetDestination() {
    const filename = window.location.pathname.split("/").pop() || "dashboard.html";
    const target = isInAdmin() ? `admin/${filename}` : filename;
    return encodeURIComponent(target + window.location.search);
  }

  function getLoginUrl() {
    return isInAdmin() ? "../login.html" : "login.html";
  }

  function getDashboardUrl() {
    const user = getCurrentUser();
    const role = (user?.role || "").toUpperCase();
    if (role === "ADMIN") {
      return isInAdmin() ? "dashboard.html" : "admin/dashboard.html";
    }
    if (role === "OFFICER" || role === "RESCUE_FORCE") {
      return isInAdmin() ? "../rescue-dashboard.html" : "rescue-dashboard.html";
    }
    // Default: Citizen dashboard
    return isInAdmin() ? "../dashboard.html" : "dashboard.html";
  }

  // Setup bfcache/pageshow listener to recheck authentication when user navigates back
  if (typeof window !== "undefined") {
    window.addEventListener("pageshow", function(event) {
      if (event.persisted) {
        const user = getCurrentUser();
        if (!user) {
          window.location.replace(`${getLoginUrl()}?next=${getTargetDestination()}`);
        }
      }
    });
  }

  return {
    getUser: getCurrentUser,
    getDashboardUrl: getDashboardUrl,

    requireAuth() {
      const user = getCurrentUser();
      if (!user) {
        window.location.replace(`${getLoginUrl()}?next=${getTargetDestination()}`);
        return false;
      }
      return true;
    },

    requireRole(allowedRoles = []) {
      if (!this.requireAuth()) return false;
      const user = getCurrentUser();
      const userRole = (user.role || "CITIZEN").toUpperCase();
      const rolesUpper = allowedRoles.map(r => r.toUpperCase());

      // Compatibility between RESCUE_FORCE and legacy OFFICER
      const isRescueMatch = (userRole === "OFFICER" || userRole === "RESCUE_FORCE") &&
                            (rolesUpper.includes("OFFICER") || rolesUpper.includes("RESCUE_FORCE"));
      const isDirectMatch = rolesUpper.includes(userRole);

      if (!isDirectMatch && !isRescueMatch) {
        if (window.LW.ui?.toast) {
          window.LW.ui.toast("Access restricted: Redirecting to your assigned role dashboard.", "warning");
        }
        setTimeout(() => {
          window.location.replace(getDashboardUrl());
        }, 120);
        return false;
      }
      return true;
    }
  };
})();
