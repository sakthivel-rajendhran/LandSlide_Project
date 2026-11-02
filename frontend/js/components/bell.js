/**
 * SlopeGuard — Notification Bell Component (bell.js)
 * Implements frozen contract:
 * LW.bell.mount(el)
 * Renders the interactive bell icon, unread counter badge, and dropdown feed.
 */

window.LW = window.LW || {};

window.LW.bell = {
  async mount(el) {
    if (!el) return null;

    el.className = "lw-bell-wrapper";
    el.style.position = "relative";
    el.innerHTML = `
      <button type="button" class="lw-bell-trigger" id="lw-bell-btn" aria-label="Landslide Warning Notifications" aria-expanded="false">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
        </svg>
        <span class="lw-bell-badge hidden" id="lw-bell-count">0</span>
      </button>

      <div class="lw-bell-dropdown" id="lw-bell-menu" role="region" aria-label="Notifications Feed">
        <div class="lw-bell-dropdown__header">
          <h4 class="lw-bell-dropdown__title">Hazard Warnings</h4>
          <button type="button" class="lw-btn lw-btn--ghost lw-btn--sm" id="lw-bell-sync-btn" style="padding: 2px 6px; font-size: 0.75rem;">
            Sync
          </button>
        </div>
        <div class="lw-bell-dropdown__feed" id="lw-bell-list">
          <div style="padding: 16px; text-align: center; color: var(--lw-text-muted); font-size: 0.8125rem;">
            Loading alerts...
          </div>
        </div>
        <div class="lw-bell-dropdown__footer">
          <a href="notifications.html">View All Alerts & History &rarr;</a>
        </div>
      </div>
    `;

    const btn = el.querySelector("#lw-bell-btn");
    const badge = el.querySelector("#lw-bell-count");
    const menu = el.querySelector("#lw-bell-menu");
    const feed = el.querySelector("#lw-bell-list");
    const syncBtn = el.querySelector("#lw-bell-sync-btn");

    async function updateBadge() {
      const count = await window.LW.notifications?.getUnreadCount() || 0;
      if (count > 0) {
        badge.textContent = count > 99 ? "99+" : count;
        badge.classList.remove("hidden");
      } else {
        badge.classList.add("hidden");
      }
    }

    async function renderDropdown() {
      const alerts = await window.LW.notifications?.getLatest(5) || [];
      const format = window.LW.format || {};

      if (alerts.length === 0) {
        feed.innerHTML = `
          <div style="padding: 24px 16px; text-align: center; color: var(--lw-text-muted); font-size: 0.8125rem;">
            No active landslide alerts at this time.
          </div>
        `;
        return;
      }

      feed.innerHTML = alerts.map(a => `
        <div class="lw-bell-item ${!a.is_read ? 'unread' : ''}" data-id="${a.id}">
          <div class="lw-bell-item__top">
            <span class="lw-risk lw-risk--${a.severity || 'high'}">${(a.severity || 'alert').toUpperCase()}</span>
            <span class="lw-bell-item__time">${format.relativeTime(a.published_at || a.received_at)}</span>
          </div>
          <strong class="lw-bell-item__title">${a.title}</strong>
          <span class="lw-bell-item__region">${a.region_name || 'Monitored Sector'}</span>
        </div>
      `).join("");

      feed.querySelectorAll(".lw-bell-item").forEach(item => {
        item.onclick = async () => {
          const id = parseInt(item.dataset.id, 10);
          await window.LW.notifications?.markAsRead(id);
          window.location.href = `notifications.html?alert=${id}`;
        };
      });
    }

    btn.onclick = (e) => {
      e.stopPropagation();
      const isOpen = menu.classList.contains("active");
      if (isOpen) {
        menu.classList.remove("active");
        btn.setAttribute("aria-expanded", "false");
      } else {
        menu.classList.add("active");
        btn.setAttribute("aria-expanded", "true");
        renderDropdown();
      }
    };

    syncBtn.onclick = async (e) => {
      e.stopPropagation();
      syncBtn.textContent = "Syncing...";
      if (window.LW.sync) {
        await window.LW.sync.trigger();
      }
      syncBtn.textContent = "Sync";
      renderDropdown();
    };

    document.addEventListener("click", (e) => {
      if (!el.contains(e.target)) {
        menu.classList.remove("active");
        btn.setAttribute("aria-expanded", "false");
      }
    });

    document.addEventListener("lw:notifications-changed", updateBadge);

    updateBadge();

    return {
      refresh: updateBadge
    };
  }
};
