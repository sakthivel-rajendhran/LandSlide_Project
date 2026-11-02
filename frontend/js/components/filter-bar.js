/**
 * SlopeGuard — Map Filter Bar Component (filter-bar.js)
 * Search, risk level filter chips, telemetry status filter, geolocation button,
 * and accessible table list-view toggle.
 */

window.LW = window.LW || {};

window.LW.filterBar = {
  mount(el, options = {}) {
    if (!el) return null;

    const onFilterChange = options.onFilterChange || (() => {});
    const onLocationRequest = options.onLocationRequest || (() => {});
    const onToggleView = options.onToggleView || (() => {});

    el.className = "lw-filter-bar-container";
    el.innerHTML = `
      <div class="lw-filter-bar">
        <!-- Search Row -->
        <div class="lw-filter-row" style="display: flex; gap: 8px; flex-wrap: wrap; align-items: center;">
          <div class="lw-search" style="flex: 1; min-width: 220px;">
            <svg class="lw-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input type="text" id="lw-filter-search" class="lw-search__input" placeholder="Search district, sector, or village..." />
          </div>

          <!-- Use My Location Button -->
          <button type="button" class="lw-btn lw-btn--secondary" id="lw-btn-location" title="Find monitored slopes near your location">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polygon points="3 11 22 2 13 21 11 13 3 11"></polygon>
            </svg>
            <span>My Location</span>
          </button>

          <!-- Toggle Map / List View -->
          <button type="button" class="lw-btn lw-btn--outline" id="lw-btn-toggle-view" title="Toggle between map and accessible table view">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="8" y1="6" x2="21" y2="6"></line>
              <line x1="8" y1="12" x2="21" y2="12"></line>
              <line x1="8" y1="18" x2="21" y2="18"></line>
              <line x1="3" y1="6" x2="3.01" y2="6"></line>
              <line x1="3" y1="12" x2="3.01" y2="12"></line>
              <line x1="3" y1="18" x2="3.01" y2="18"></line>
            </svg>
            <span id="lw-toggle-label">List View</span>
          </button>
        </div>

        <!-- Filter Chips Row -->
        <div class="lw-filter-chips" style="display: flex; gap: 8px; overflow-x: auto; padding-top: 8px; align-items: center;">
          <span style="font-size: 0.75rem; color: var(--lw-text-muted); font-weight: 600; text-transform: uppercase;">Risk:</span>
          <button type="button" class="lw-btn lw-btn--ghost lw-btn--sm lw-filter-chip active" data-filter-risk="all">All</button>
          <button type="button" class="lw-btn lw-btn--ghost lw-btn--sm lw-filter-chip" data-filter-risk="critical">Critical</button>
          <button type="button" class="lw-btn lw-btn--ghost lw-btn--sm lw-filter-chip" data-filter-risk="high">High</button>
          <button type="button" class="lw-btn lw-btn--ghost lw-btn--sm lw-filter-chip" data-filter-risk="moderate">Moderate</button>
          <button type="button" class="lw-btn lw-btn--ghost lw-btn--sm lw-filter-chip" data-filter-risk="low">Low</button>

          <span style="font-size: 0.75rem; color: var(--lw-text-muted); font-weight: 600; text-transform: uppercase; margin-left: 8px;">Telemetry:</span>
          <select id="lw-filter-status" style="padding: 4px 10px; background: #FFFFFF; border: 1px solid var(--lw-border); border-radius: var(--lw-r-sm); color: var(--lw-text, #2E3A2F); font-size: 0.75rem;">
            <option value="all">All Telemetry</option>
            <option value="current">Current Only</option>
            <option value="delayed">Delayed</option>
            <option value="unavailable">Unavailable</option>
          </select>
        </div>
      </div>
    `;

    const searchInput = el.querySelector("#lw-filter-search");
    const statusSelect = el.querySelector("#lw-filter-status");
    const locationBtn = el.querySelector("#lw-btn-location");
    const toggleBtn = el.querySelector("#lw-btn-toggle-view");
    const toggleLabel = el.querySelector("#lw-toggle-label");
    const riskChips = el.querySelectorAll("[data-filter-risk]");

    let selectedRisk = "all";
    let isListView = false;

    function emitFilters() {
      onFilterChange({
        query: searchInput.value.trim(),
        risk: selectedRisk,
        dataStatus: statusSelect.value
      });
    }

    searchInput.addEventListener("input", emitFilters);
    statusSelect.addEventListener("change", emitFilters);

    riskChips.forEach(chip => {
      chip.addEventListener("click", () => {
        riskChips.forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        selectedRisk = chip.dataset.filterRisk;
        emitFilters();
      });
    });

    locationBtn.addEventListener("click", () => {
      onLocationRequest();
    });

    toggleBtn.addEventListener("click", () => {
      isListView = !isListView;
      toggleLabel.textContent = isListView ? "Map View" : "List View";
      onToggleView(isListView);
    });

    return {
      setQuery(q) {
        searchInput.value = q;
        emitFilters();
      }
    };
  }
};
