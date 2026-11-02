/**
 * SlopeGuard — Multi-Shell Layout Injector (shell.js)
 * Manages layouts for data-shell="public|user|admin", mounts skip link, bell slot, and footer disclaimers.
 */

window.LW = window.LW || {};

window.LW.shell = (function() {
  function init() {
    const shellType = document.body.dataset.shell || "public";
    injectSkipLink();
    injectFooterDisclaimer();

    // Auto-mount bell if slot exists and bell component is loaded
    const bellSlot = document.getElementById("lw-bell-slot");
    if (bellSlot && window.LW.bell && typeof window.LW.bell.mount === "function") {
      window.LW.bell.mount(bellSlot);
    }
  }

  function injectSkipLink() {
    if (document.getElementById("lw-skip-link")) return;
    const skipLink = document.createElement("a");
    skipLink.id = "lw-skip-link";
    skipLink.href = "#mainContent";
    skipLink.className = "lw-skip-link";
    skipLink.textContent = "Skip to main content";
    document.body.prepend(skipLink);
  }

  function injectFooterDisclaimer() {
    const disclaimerContainers = document.querySelectorAll("[data-lw-disclaimer]");
    disclaimerContainers.forEach(container => {
      container.textContent = window.LW.content?.safetyDisclaimer || "";
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }

  return { init };
})();
