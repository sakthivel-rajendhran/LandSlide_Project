/**
 * SlopeGuard — Shared UI Component Manager (ui.js)
 * Toasts, confirmation modals, slide-out drawer, banners, and state renderers
 */

window.LW = window.LW || {};

window.LW.ui = (function() {
  // Ensure Toast Container Exists
  function getToastContainer() {
    let container = document.getElementById("lw-toast-container");
    if (!container) {
      container = document.createElement("div");
      container.id = "lw-toast-container";
      container.className = "lw-toast-container";
      document.body.appendChild(container);
    }
    return container;
  }

  return {
    dismissToasts() {
      const container = document.getElementById("lw-toast-container");
      if (container) {
        container.querySelectorAll(".lw-toast").forEach(t => t.remove());
      }
    },

    toast(message, type = "info", duration = 3500) {
      const container = getToastContainer();

      // When showing success or error, dismiss in-progress info toasts to avoid duplicate stacked popups
      if (type === "success" || type === "error") {
        container.querySelectorAll(".lw-toast--info").forEach(t => {
          t.classList.remove("visible");
          setTimeout(() => t.remove(), 100);
        });
      }

      // Prevent exact duplicate toasts showing simultaneously
      const existing = Array.from(container.querySelectorAll(".lw-toast span")).find(s => s.textContent === message);
      if (existing) return;

      const toast = document.createElement("div");
      toast.className = `lw-toast lw-toast--${type}`;
      toast.innerHTML = `
        <span>${message}</span>
        <button type="button" aria-label="Close" style="background:none;border:none;color:var(--lw-text-muted);cursor:pointer;padding:2px;font-size:16px;line-height:1;">&times;</button>
      `;

      const closeBtn = toast.querySelector("button");
      closeBtn.onclick = () => removeToast();

      container.appendChild(toast);
      requestAnimationFrame(() => toast.classList.add("visible"));

      const timeout = setTimeout(removeToast, duration);

      function removeToast() {
        clearTimeout(timeout);
        toast.classList.remove("visible");
        setTimeout(() => toast.remove(), 250);
      }
    },

    confirmAction(opts) {
      return new Promise((resolve) => {
        // Prevent duplicate confirmation dialogs
        const existing = document.getElementById("lw-confirm-dialog-backdrop");
        if (existing) existing.remove();

        const title = opts.title || "Confirm Action";
        const message = opts.message || "Are you sure you want to proceed?";
        const confirmText = opts.confirmText || "Confirm";
        const cancelText = opts.cancelText || "Cancel";
        const variant = opts.variant || "primary";

        let iconSvg = '';
        if (variant === 'danger') {
          iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;
        } else if (variant === 'success' || variant === 'forest') {
          iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`;
        } else if (variant === 'warning') {
          iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
        } else {
          iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
        }

        const modal = document.createElement("div");
        modal.id = "lw-confirm-dialog-backdrop";
        modal.className = "lw-confirm-backdrop active";
        modal.innerHTML = `
          <div class="lw-confirm-card" role="dialog" aria-modal="true" aria-labelledby="lw-confirm-title">
            <div class="lw-confirm-header">
              <div class="lw-confirm-icon lw-confirm-icon--${variant}">${iconSvg}</div>
              <h3 class="lw-confirm-title" id="lw-confirm-title">${title}</h3>
            </div>
            <p class="lw-confirm-message">${message}</p>
            <div class="lw-confirm-footer">
              <button type="button" class="lw-btn lw-btn--outline" id="lw-confirm-btn-cancel">${cancelText}</button>
              <button type="button" class="lw-btn lw-btn--${variant}" id="lw-confirm-btn-ok">${confirmText}</button>
            </div>
          </div>
        `;

        document.body.appendChild(modal);

        const cancelBtn = modal.querySelector("#lw-confirm-btn-cancel");
        const confirmBtn = modal.querySelector("#lw-confirm-btn-ok");

        function cleanup() {
          modal.classList.remove("active");
          setTimeout(() => modal.remove(), 180);
        }

        cancelBtn.onclick = () => {
          cleanup();
          resolve(false);
        };

        modal.onclick = (e) => {
          if (e.target === modal) {
            cleanup();
            resolve(false);
          }
        };

        confirmBtn.onclick = async () => {
          confirmBtn.disabled = true;
          cancelBtn.disabled = true;
          const orig = confirmBtn.innerHTML;
          confirmBtn.innerHTML = `<span class="lw-btn-spinner"></span> Processing...`;

          if (typeof opts.onConfirm === "function") {
            try {
              await opts.onConfirm();
              cleanup();
              resolve(true);
            } catch (err) {
              confirmBtn.disabled = false;
              cancelBtn.disabled = false;
              confirmBtn.innerHTML = orig;
            }
          } else {
            cleanup();
            resolve(true);
          }
        };

        confirmBtn.focus();
      });
    },

    promptAction(opts) {
      return new Promise((resolve) => {
        const existing = document.getElementById("lw-confirm-dialog-backdrop");
        if (existing) existing.remove();

        const title = opts.title || "Input Required";
        const message = opts.message || "Please enter details:";
        const placeholder = opts.placeholder || "";
        const confirmText = opts.confirmText || "Submit";
        const cancelText = opts.cancelText || "Cancel";
        const variant = opts.variant || "danger";
        const required = opts.required !== false;

        const modal = document.createElement("div");
        modal.id = "lw-confirm-dialog-backdrop";
        modal.className = "lw-confirm-backdrop active";
        modal.innerHTML = `
          <div class="lw-confirm-card" role="dialog" aria-modal="true">
            <div class="lw-confirm-header">
              <div class="lw-confirm-icon lw-confirm-icon--${variant}">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              </div>
              <h3 class="lw-confirm-title">${title}</h3>
            </div>
            <p class="lw-confirm-message">${message}</p>
            <div class="lw-confirm-input-wrap">
              <textarea class="lw-confirm-input" id="lw-prompt-input" placeholder="${placeholder}"></textarea>
              <div class="lw-confirm-error" id="lw-prompt-error">This field cannot be empty.</div>
            </div>
            <div class="lw-confirm-footer">
              <button type="button" class="lw-btn lw-btn--outline" id="lw-prompt-btn-cancel">${cancelText}</button>
              <button type="button" class="lw-btn lw-btn--${variant}" id="lw-prompt-btn-ok">${confirmText}</button>
            </div>
          </div>
        `;

        document.body.appendChild(modal);

        const input = modal.querySelector("#lw-prompt-input");
        const errorEl = modal.querySelector("#lw-prompt-error");
        const cancelBtn = modal.querySelector("#lw-prompt-btn-cancel");
        const confirmBtn = modal.querySelector("#lw-prompt-btn-ok");

        function cleanup() {
          modal.classList.remove("active");
          setTimeout(() => modal.remove(), 180);
        }

        cancelBtn.onclick = () => {
          cleanup();
          resolve(null);
        };

        modal.onclick = (e) => {
          if (e.target === modal) {
            cleanup();
            resolve(null);
          }
        };

        confirmBtn.onclick = () => {
          const val = input.value.trim();
          if (required && !val) {
            errorEl.style.display = "block";
            input.focus();
            return;
          }
          confirmBtn.disabled = true;
          cancelBtn.disabled = true;
          confirmBtn.innerHTML = `<span class="lw-btn-spinner"></span> Processing...`;
          cleanup();
          resolve(val);
        };

        setTimeout(() => input.focus(), 50);
      });
    },

    confirm(title, message, onConfirm) {
      return this.confirmAction({
        title,
        message,
        onConfirm
      });
    },

    state: {
      render(el, stateType, opts = {}) {
        if (!el) return;
        const content = window.LW.content || {};

        if (stateType === "loading") {
          el.innerHTML = `
            <div class="lw-state">
              <div class="lw-skeleton" style="width: 48px; height: 48px; border-radius: 50%;"></div>
              <p class="lw-state__title">${opts.message || content.loadingRegions || "Loading..."}</p>
            </div>
          `;
        } else if (stateType === "empty") {
          el.innerHTML = `
            <div class="lw-state">
              <svg class="lw-state__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              <h4 class="lw-state__title">${opts.title || "No data available"}</h4>
              <p class="lw-state__desc">${opts.message || content.noRegionsAvailable}</p>
              ${opts.action ? `<button class="lw-btn lw-btn--outline lw-btn--sm" id="lw-state-action">${opts.action.label}</button>` : ""}
            </div>
          `;
          if (opts.action?.onClick) {
            el.querySelector("#lw-state-action")?.addEventListener("click", opts.action.onClick);
          }
        } else if (stateType === "error") {
          el.innerHTML = `
            <div class="lw-state">
              <svg class="lw-state__icon" style="color: var(--lw-risk-crit);" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"></polygon>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              <h4 class="lw-state__title">${opts.title || "Data Unavailable"}</h4>
              <p class="lw-state__desc">${opts.message || content.dataFailed}</p>
              ${opts.onRetry ? `<button class="lw-btn lw-btn--primary lw-btn--sm" id="lw-state-retry">Retry</button>` : ""}
            </div>
          `;
          if (opts.onRetry) {
            el.querySelector("#lw-state-retry")?.addEventListener("click", opts.onRetry);
          }
        } else if (stateType === "offline") {
          el.innerHTML = `
            <div class="lw-state">
              <svg class="lw-state__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="1" y1="1" x2="23" y2="23"></line>
                <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"></path>
                <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"></path>
                <line x1="12" y1="20" x2="12.01" y2="20"></line>
              </svg>
              <h4 class="lw-state__title">Working Offline</h4>
              <p class="lw-state__desc">${opts.message || content.cachedDataWarning}</p>
            </div>
          `;
        }
      }
    },

    scrollToElement(el, smooth = true) {
      if (!el) return;
      const target = typeof el === "string" ? document.querySelector(el) : el;
      if (!target) return;

      // Locate destination heading or title element
      const headingEl = (target.matches && target.matches('h1, h2, h3, .panel-title, .card-title, .section-title, .panel-header, .dashboard-card-header'))
        ? target
        : (target.querySelector?.('.panel-title, .card-title, .section-title, .panel-header, .dashboard-card-header, .section-header, h1, h2, h3') || target);

      // Measure effective navbar bottom edge
      const header = document.querySelector(".dashboard-topbar, .site-header, .public-header, header");
      let navBottom = 68;
      if (header) {
        const rect = header.getBoundingClientRect();
        const subContainer = header.querySelector('.dashboard-topbar, .header-container, .nav-bar') || header;
        const subRect = subContainer.getBoundingClientRect();
        navBottom = Math.max(Math.round(rect.bottom), Math.round(subRect.bottom));
        if (navBottom <= 0) navBottom = 68;
      }

      const rect = headingEl.getBoundingClientRect();
      const docY = window.pageYOffset || document.documentElement.scrollTop;

      // Target heading position = navbar bottom + 12px (within 8-16px range)
      const targetY = Math.max(0, Math.round(rect.top + docY - navBottom - 12));
      const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      window.scrollTo({
        top: targetY,
        behavior: (smooth && !prefersReduced) ? "smooth" : "auto"
      });
    }
  };
})();

// Automatic Page Scroll Stabilization across portals and admin
(function() {
  if ("scrollRestoration" in history) {
    history.scrollRestoration = "manual";
  }

  function handleInitialScroll() {
    if (window.location.hash) {
      try {
        const target = document.querySelector(window.location.hash);
        if (target) {
          setTimeout(() => {
            window.LW?.ui?.scrollToElement(target, true);
          }, 120);
        }
      } catch (_) {}
    } else {
      window.scrollTo(0, 0);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", handleInitialScroll);
  } else {
    handleInitialScroll();
  }

  document.addEventListener("click", (e) => {
    // If public navigation exists, main.js manages it to prevent duplicate scrolling
    if (document.querySelector('.site-header .nav-links')) return;

    const link = e.target.closest('a[href^="#"]');
    if (!link) return;
    const href = link.getAttribute("href");
    if (!href || href === "#" || href === "#!") return;
    try {
      const target = document.querySelector(href);
      if (target) {
        e.preventDefault();
        window.LW?.ui?.scrollToElement(target, true);
        if (window.location.hash !== href) {
          history.pushState(null, "", href);
        }
      }
    } catch (_) {}
  });
})();
