/**
 * SlopeGuard — Unified Authentication Service (core/auth.js)
 * Implements Ragul's Module 1 authentication interface:
 * - LW.auth.login(email, password, remember)   -> Promise<user>
 * - LW.auth.register({ fullName, email, password, district }) -> Promise<user>
 * - LW.auth.logout()                           -> clears session, redirects
 * - LW.auth.getUser()                          -> { id, name, email, role, district } | null
 * - LW.auth.hasRole(...roles)                  -> boolean (user | officer | admin)
 * - LW.auth.onChange(callback)                 -> login, logout, session expiry
 */

window.LW = window.LW || {};

window.LW.auth = (function() {
  const STORAGE_KEY = "sg_user";
  const listeners = new Set();

  function notify(event, user) {
    listeners.forEach(cb => {
      try {
        cb(event, user);
      } catch (e) {
        console.error("[LW.auth] Listener error:", e);
      }
    });
  }

  function getStoredUser() {
    try {
      const stored = sessionStorage.getItem(STORAGE_KEY) || localStorage.getItem(STORAGE_KEY);
      if (!stored) return null;
      const user = JSON.parse(stored);
      if (user && user.role) user.role = String(user.role).toUpperCase();
      return user;
    } catch (e) {
      return null;
    }
  }

  function setStoredUser(user, remember = true) {
    if (user && user.role) user.role = String(user.role).toUpperCase();
    const raw = JSON.stringify(user);
    if (remember) {
      localStorage.setItem(STORAGE_KEY, raw);
      sessionStorage.removeItem(STORAGE_KEY);
    } else {
      sessionStorage.setItem(STORAGE_KEY, raw);
      localStorage.removeItem(STORAGE_KEY);
    }
    notify("login", user);
  }

  function clearStoredUser() {
    const prev = getStoredUser();
    sessionStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(STORAGE_KEY);
    if (typeof localStorage !== "undefined") {
      localStorage.removeItem("sg_token");
    }
    notify("logout", prev);
  }

  return {
    async login(email, password, remember = true) {
      if (!email || !password) {
        throw new Error("Please enter both email and password.");
      }

      if (!window.LW.api) {
        throw new Error("Authentication service is unavailable.");
      }

      const res = await window.LW.api.post("/api/auth/login", {
        email: email.trim(),
        username: email.trim(),
        password,
        remember
      });

      if (res && (res.token || res.data?.token || res.userId || res.data?.userId)) {
        const u = res.token ? res : (res.data || res);
        const normalizedUser = {
          ...u,
          id: u.userId || u.id,
          userId: u.userId || u.id,
          name: u.fullName || u.name,
          fullName: u.fullName || u.name,
          email: u.email,
          role: (u.role || "CITIZEN").toUpperCase(),
          roles: [ (u.role || "CITIZEN").toUpperCase(), (u.role || "CITIZEN").toLowerCase() ],
          district: u.district || "NER Central Command",
          token: u.token || res.token
        };
        if (normalizedUser.token && typeof localStorage !== "undefined") {
          localStorage.setItem("sg_token", normalizedUser.token);
        }
        setStoredUser(normalizedUser, remember);
        return normalizedUser;
      }

      throw new Error("Invalid credentials or unauthorized station. Please verify your details.");
    },

    async register({ fullName, email, password, district = "NER Command", location = null, phoneNumber = null }) {
      if (!fullName || !fullName.trim()) {
        throw new Error("Please enter your full name.");
      }
      if (fullName.trim().length < 2) {
        throw new Error("Full name must be at least 2 characters.");
      }
      if (!email || !email.trim()) {
        throw new Error("Please enter a valid official email address.");
      }
      if (!password || password.length < 8) {
        throw new Error("Security password must be at least 8 characters long.");
      }

      if (!window.LW.api) {
        throw new Error("Authentication service is unavailable.");
      }

      const payload = {
        fullName: fullName.trim(),
        email: email.trim(),
        password: password
      };
      if (phoneNumber) payload.phoneNumber = phoneNumber.trim();

      const res = await window.LW.api.post("/api/auth/register", payload);

      if (res && (res.token || res.data?.token)) {
        const token = res.token || res.data?.token;
        const u = res.token ? res : (res.data || res);
        const normalizedUser = {
          ...u,
          id: u.userId || u.id,
          userId: u.userId || u.id,
          name: u.fullName || u.name || fullName.trim(),
          fullName: u.fullName || u.name || fullName.trim(),
          email: u.email || email.trim(),
          role: (u.role || "CITIZEN").toLowerCase(),
          roles: [ (u.role || "CITIZEN").toLowerCase() ],
          district: district || "NER Command",
          location: location || null,
          token: token
        };
        if (typeof localStorage !== "undefined") {
          localStorage.setItem("sg_token", token);
        }
        if (typeof sessionStorage !== "undefined") {
          sessionStorage.setItem("sg_token", token);
        }
        setStoredUser(normalizedUser, true);
        return normalizedUser;
      }

      // If response did not include a token, perform auto-login to obtain genuine JWT Bearer token
      return await this.login(email.trim(), password, true);
    },

    async fetchOAuthSession() {
      if (!window.LW.api) {
        throw new Error("API service is unavailable.");
      }
      const res = await window.LW.api.get("/api/auth/oauth/session");
      if (res && (res.token || res.data?.token || res.userId || res.data?.userId)) {
        const u = res.token ? res : (res.data || res);
        const normalizedUser = {
          ...u,
          id: u.userId || u.id,
          userId: u.userId || u.id,
          name: u.fullName || u.name,
          fullName: u.fullName || u.name,
          email: u.email,
          role: (u.role || "CITIZEN").toUpperCase(),
          roles: [ (u.role || "CITIZEN").toUpperCase(), (u.role || "CITIZEN").toLowerCase() ],
          district: u.district || "NER Central Command",
          token: u.token || res.token
        };
        if (normalizedUser.token && typeof localStorage !== "undefined") {
          localStorage.setItem("sg_token", normalizedUser.token);
        }
        if (normalizedUser.token && typeof sessionStorage !== "undefined") {
          sessionStorage.setItem("sg_token", normalizedUser.token);
        }
        setStoredUser(normalizedUser, true);
        return normalizedUser;
      }
      throw new Error("Unable to establish authenticated OAuth session.");
    },

    initiateGoogleOAuth(returnUrl = null) {
      const config = window.LW.config || {};
      let apiBase = (config.apiBase || "/api").replace(/\/+$/, "");
      let backendBase = apiBase;
      if (backendBase.endsWith("/api")) {
        backendBase = backendBase.substring(0, backendBase.length - 4);
      }
      let url = `${backendBase}/oauth2/authorization/google`;
      if (returnUrl) {
        url += `?redirect_uri=${encodeURIComponent(returnUrl)}`;
      }
      window.location.href = url;
    },

    async loginWithGoogle(credentialOrProfile) {
      if (!window.LW.api) {
        throw new Error("Authentication service is unavailable.");
      }

      let payload = {};
      if (typeof credentialOrProfile === "string") {
        payload.idToken = credentialOrProfile;
        payload.token = credentialOrProfile;
      } else if (typeof credentialOrProfile === "object" && credentialOrProfile !== null) {
        payload = {
          idToken: credentialOrProfile.idToken || credentialOrProfile.token || null,
          token: credentialOrProfile.token || credentialOrProfile.idToken || null,
          sub: credentialOrProfile.sub || null,
          email: credentialOrProfile.email || null,
          name: credentialOrProfile.name || null,
          picture: credentialOrProfile.picture || null
        };
      }

      const res = await window.LW.api.post("/api/auth/google", payload);

      if (res && (res.token || res.data?.token || res.userId || res.data?.userId)) {
        const u = res.token ? res : (res.data || res);
        const normalizedUser = {
          ...u,
          id: u.userId || u.id,
          userId: u.userId || u.id,
          name: u.fullName || u.name,
          fullName: u.fullName || u.name,
          email: u.email,
          role: (u.role || "CITIZEN").toUpperCase(),
          roles: [ (u.role || "CITIZEN").toUpperCase(), (u.role || "CITIZEN").toLowerCase() ],
          district: u.district || "NER Central Command",
          token: u.token || res.token
        };
        if (normalizedUser.token && typeof localStorage !== "undefined") {
          localStorage.setItem("sg_token", normalizedUser.token);
        }
        if (normalizedUser.token && typeof sessionStorage !== "undefined") {
          sessionStorage.setItem("sg_token", normalizedUser.token);
        }
        setStoredUser(normalizedUser, true);
        return normalizedUser;
      }

      throw new Error("External OAuth verification failed. Please sign in with your registered email and password.");
    },

    async logout() {
      try {
        if (window.LW.api) {
          await window.LW.api.post("/auth/logout").catch(() => {});
        }
      } catch (e) {}

      clearStoredUser();
      return true;
    },

    getUser() {
      return getStoredUser();
    },

    // Alias for existing guard calls
    getCurrentUser() {
      return getStoredUser();
    },

    hasRole(...roles) {
      const user = getStoredUser();
      if (!user) return false;
      const userRole = (user.role || "").toLowerCase();
      const userRoles = Array.isArray(user.roles) ? user.roles.map(r => r.toLowerCase()) : [userRole];
      return roles.some(req => userRoles.includes(req.toLowerCase()));
    },

    onChange(callback) {
      if (typeof callback === "function") {
        listeners.add(callback);
        return () => listeners.delete(callback);
      }
      return () => {};
    },

    async forgotPassword(email) {
      if (!email || !email.trim()) {
        throw new Error("Please enter your registered email address.");
      }
      if (!window.LW.api) {
        throw new Error("API service is unavailable.");
      }
      return await window.LW.api.post("/api/auth/forgot-password", {
        email: email.trim().toLowerCase()
      });
    },

    async verifyResetToken(token) {
      if (!token) {
        throw new Error("Reset token is required.");
      }
      if (!window.LW.api) {
        throw new Error("API service is unavailable.");
      }
      return await window.LW.api.get(`/api/auth/verify-reset-token?token=${encodeURIComponent(token)}`);
    },

    async resetPassword(token, password, confirmPassword) {
      if (!token) {
        throw new Error("Reset token is required.");
      }
      if (!password || password.length < 8) {
        throw new Error("Password must be at least 8 characters long.");
      }
      if (password !== confirmPassword) {
        throw new Error("Passwords do not match.");
      }
      if (!window.LW.api) {
        throw new Error("API service is unavailable.");
      }
      return await window.LW.api.post("/api/auth/reset-password", {
        token: token.trim(),
        password,
        confirmPassword
      });
    },

    async updateLocation(location) {
      if (!location || typeof location.latitude !== "number" || typeof location.longitude !== "number") {
        throw new Error("Valid coordinates are required to update monitoring location.");
      }
      if (!window.LW.api) {
        throw new Error("API service is unavailable.");
      }
      const res = await window.LW.api.post("/api/user/location", { location });
      if (res && res.data && res.data.user) {
        setStoredUser(res.data.user, true);
      }
      return res.data || res;
    }
  };
})();

// Alias for complete backward compatibility across previous scripts
window.LW.authService = window.LW.auth;
