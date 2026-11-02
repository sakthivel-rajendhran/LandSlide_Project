/**
 * SlopeGuard — Citizen Landslide & Risk Dashboard Controller (pages/dashboard.js)
 * Clean MVP Controller: Manages authenticated citizen session,
 * official public alerts, personal report lifecycle tracking, and photo inspections.
 */

document.addEventListener("DOMContentLoaded", async () => {
  // 1. Route Protection — Enforce Authentication
  const user = window.LW?.authService?.getCurrentUser() || (function() {
    try {
      const raw = sessionStorage.getItem("sg_user") || localStorage.getItem("sg_user");
      return raw ? JSON.parse(raw) : null;
    } catch (e) { return null; }
  })();

  if (!user) {
    const currentPath = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.href = `login.html?next=${currentPath}`;
    return;
  }

  // 2. Populate User Information in Shell
  populateUserInfo(user);

  // 3. Mount Notification Bell Widget
  const bellSlot = document.getElementById("dashboardBellSlot");
  if (bellSlot && window.LW?.bell) {
    window.LW.bell.mount(bellSlot);
  }

  // 4. State & Caches
  let myPersonalReports = [];
  let publicAlerts = [];
  let activeMyReportsFilter = "all";

  // 5. Initial Data Load
  await loadDashboardData();

  // 6. Setup Tab Routing (?tab=overview|myreports|alerts|profile)
  setupTabRouting();

  // 7. Setup Event Listeners
  setupEventListeners();

  // =========================================================================
  // Core Data Fetching Functions
  // =========================================================================
  async function loadDashboardData() {
    try {
      // Parallel fetch from live backend endpoints
      const [publicAlertsRes, myReportsRes] = await Promise.all([
        fetchJson("/api/public/alerts").catch(() => []),
        window.LW?.incidentsService?.getMyIncidents().catch(() => [])
      ]);

      publicAlerts = Array.isArray(publicAlertsRes) ? publicAlertsRes : [];
      myPersonalReports = Array.isArray(myReportsRes) ? myReportsRes : [];

      // Render Dashboard Overview Components
      renderMetricCards(publicAlerts, myPersonalReports);
      renderOfficialAlerts(publicAlerts);
      renderRecentReportsOverview(myPersonalReports);

      // Render Dedicated Tabs Components
      renderMyReportsFullTable(myPersonalReports);
      renderAlertsTabFeed(publicAlerts);

      // Update Freshness in Topbar
      const freshnessEl = document.getElementById("topbarFreshnessTime");
      if (freshnessEl) freshnessEl.textContent = "Live Telemetry Active";
    } catch (err) {
      console.error("[Dashboard] Error loading dashboard data:", err);
      window.LW?.ui?.toast("Error synchronizing citizen portal data.", "error");
    }
  }

  async function fetchJson(endpoint) {
    if (window.LW?.api?.get) {
      return await window.LW.api.get(endpoint);
    }
    const token = sessionStorage.getItem("sg_token") || localStorage.getItem("sg_token");
    const headers = { "Accept": "application/json" };
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(`http://localhost:8080${endpoint}`, { headers });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  }

  // =========================================================================
  // Render Metric Cards
  // =========================================================================
  function renderMetricCards(alerts, myReports) {
    const totalCount = myReports.length;
    const pendingCount = myReports.filter(r => {
      const s = (r.status || "").toUpperCase();
      const rs = (r.rescueStatus || "").toUpperCase();
      if (s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED") return false;
      return s === "PENDING" || s === "UNDER_REVIEW";
    }).length;
    const verifiedCount = myReports.filter(r => {
      const s = (r.status || "").toUpperCase();
      const rs = (r.rescueStatus || "").toUpperCase();
      return s === "VERIFIED" || s === "ASSIGNED" || s === "RESPONDING" || s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED";
    }).length;
    const activeAlertsCount = alerts.length;

    // Support both new semantic IDs and fallback legacy IDs
    const totalEl = document.getElementById("metricTotalReportsValue") || document.getElementById("metricReportsValue");
    if (totalEl) totalEl.textContent = totalCount;

    const pendingEl = document.getElementById("metricPendingReportsValue") || document.getElementById("metricReviewValue");
    if (pendingEl) pendingEl.textContent = pendingCount;

    const verifiedEl = document.getElementById("metricVerifiedReportsValue") || document.getElementById("metricVerifiedValue");
    if (verifiedEl) verifiedEl.textContent = verifiedCount;

    const alertsEl = document.getElementById("metricActiveAlertsValue") || document.getElementById("metricAlertsValue");
    if (alertsEl) alertsEl.textContent = activeAlertsCount;
  }

  // =========================================================================
  // Render Relevant Official Alerts
  // =========================================================================
  function renderOfficialAlerts(alerts) {
    const container = document.getElementById("citizenAlertsList");
    if (!container) return;

    if (!alerts || alerts.length === 0) {
      container.innerHTML = `
        <div style="background: #F8F6EE; border: 1px dashed #D9C9B2; border-radius: 4px; padding: 12px 16px; display: flex; align-items: center; justify-content: center; gap: 8px;">
          <span style="width: 8px; height: 8px; border-radius: 50%; background: #6B7F5B; display: inline-block;"></span>
          <span style="color: #2E3A2F; font-size: 0.8125rem; font-weight: 600;">No active official alerts at this time. All sectors are clear.</span>
        </div>
      `;
      return;
    }

    container.innerHTML = alerts.map(a => {
      const sev = (a.severity || "info").toLowerCase();
      const timeStr = a.published_at || a.publishedAt || a.timestamp || a.issuedAt || a.created_at || new Date().toISOString();
      const formattedDate = new Date(timeStr).toLocaleDateString(undefined, {
        month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit"
      });
      const area = a.area_name || a.areaName || a.location || a.region || "Sector Notice";
      const title = a.title || "Civil Protection Advisory";
      const desc = escapeHtml(a.description || a.message || "");
      const actions = (a.recommended_actions || a.recommendedActions || "").trim();

      return `
        <div class="citizen-alert-item ${sev}">
          <div class="citizen-alert-top">
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <span class="citizen-alert-badge ${sev}">${escapeHtml(a.severity || "NOTICE").toUpperCase()}</span>
              <strong style="color: #2E3A2F; font-size: 0.875rem;">${escapeHtml(title)}</strong>
            </div>
            <span class="citizen-alert-timestamp">${formattedDate}</span>
          </div>
          <div class="citizen-alert-location">
            <img src="assets/icons/location-pin.png" alt="Location pin" class="location-pin-icon" style="height: 14px; width: 14px; object-fit: contain; vertical-align: -2px; margin-right: 4px;" />
            ${escapeHtml(area)}
          </div>
          ${desc ? `<p class="citizen-alert-msg" style="margin-top: 4px;">${desc}</p>` : ''}
          ${actions ? `
            <div style="margin-top: 6px; padding: 6px 10px; background: rgba(199, 244, 100, 0.2); border-left: 3px solid #2E3A2F; border-radius: 3px; font-size: 0.775rem; color: #1b3815;">
              <strong>Safety:</strong> ${escapeHtml(actions)}
            </div>
          ` : ''}
        </div>
      `;
    }).join("");
  }

  // =========================================================================
  // Render Alerts Tab Feed
  // =========================================================================
  function renderAlertsTabFeed(alerts) {
    const container = document.getElementById("fullAlertsFeedContainer");
    if (!container) return;

    if (!alerts || alerts.length === 0) {
      container.innerHTML = `
        <div style="background: #FFFFFF; border: 1px dashed #D9C9B2; border-radius: 6px; padding: 24px; text-align: center;">
          <span style="color: #2E3A2F; font-weight: 600; font-size: 0.875rem;">No active official alerts at this time. All sectors are clear.</span>
        </div>
      `;
      return;
    }

    container.innerHTML = alerts.map(a => {
      const sev = (a.severity || "info").toLowerCase();
      const timeStr = a.published_at || a.publishedAt || a.timestamp || a.issuedAt || new Date().toISOString();
      const formattedDate = new Date(timeStr).toLocaleDateString(undefined, {
        month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit"
      });
      const area = a.area_name || a.areaName || a.location || "Sector Notice";
      const title = a.title || "Civil Protection Advisory";
      const desc = escapeHtml(a.description || a.message || "");
      const actions = (a.recommended_actions || a.recommendedActions || "").trim();

      return `
        <div class="dashboard-panel-card" style="border-left: 4px solid ${sev === 'critical' ? '#B84B29' : (sev === 'high' ? '#C96F4F' : '#6B7F5B')};">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span class="citizen-alert-badge ${sev}">${escapeHtml(a.severity || "NOTICE").toUpperCase()}</span>
              <strong style="color: #2E3A2F; font-size: 1rem;">${escapeHtml(title)}</strong>
            </div>
            <span style="font-size: 0.75rem; color: #5E6F5C;">${formattedDate}</span>
          </div>
          <div style="font-size: 0.8125rem; font-weight: 600; color: #C96F4F; margin-bottom: 8px;">
            <img src="assets/icons/location-pin.png" alt="Location pin" class="location-pin-icon" style="height: 14px; width: 14px; object-fit: contain; vertical-align: -2px; margin-right: 4px;" />
            ${escapeHtml(area)}
          </div>
          ${desc ? `<p style="font-size: 0.875rem; color: #2E3A2F; line-height: 1.45; margin: 0 0 8px 0;">${desc}</p>` : ''}
          ${actions ? `
            <div style="background: rgba(199, 244, 100, 0.2); border-left: 3px solid #2E3A2F; border-radius: 4px; padding: 8px 12px; font-size: 0.8125rem; color: #1b3815;">
              <strong>Safety:</strong> ${escapeHtml(actions)}
            </div>
          ` : ''}
        </div>
      `;
    }).join("");
  }

  // =========================================================================
  // Render Recent Reports on Overview Tab
  // =========================================================================
  function renderRecentReportsOverview(reports) {
    const tbody = document.getElementById("dashboardReportsTableBody");
    if (!tbody) return;

    if (!reports || reports.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align: center; padding: 24px; color: var(--color-text-muted);">
            No reports submitted yet.
            <div style="margin-top: 8px;">
              <a href="report-incident.html" class="btn-dashboard-primary" style="display: inline-block; text-decoration: none; font-size: 0.8125rem;">
                + Report an Incident
              </a>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    const recent = reports.slice(0, 5);
    tbody.innerHTML = recent.map(r => {
      const ref = r.id ? `#${r.id}` : (r.localId || "INC");
      const loc = (r.location?.roadName || r.locationDescription || "Regional Slope");
      const timeStr = r.submittedAt || r.reportedTimestamp || r.observedAt || new Date().toISOString();
      const formattedDate = new Date(timeStr).toLocaleDateString(undefined, {
        month: "short", day: "numeric", year: "numeric"
      });
      const statusBadge = getStatusBadge(r.status, r.rescueStatus);

      return `
        <tr>
          <td><strong style="font-family: monospace; color: #C96F4F;">${ref}</strong></td>
          <td>
            <div style="display: flex; align-items: center; gap: 6px;">
              <img src="assets/icons/location-pin.png" alt="Location pin" class="location-pin-icon" style="height: 14px; width: 14px; object-fit: contain; flex-shrink: 0;" />
              <span style="max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(loc)}</span>
            </div>
          </td>
          <td>${formattedDate}</td>
          <td>${statusBadge}</td>
          <td style="text-align: right;">
            <button type="button" class="btn-dashboard-primary btn-inspect-report" data-id="${r.id}" style="padding: 4px 10px; font-size: 0.75rem;">
              Inspect
            </button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-inspect-report").forEach(btn => {
      btn.onclick = () => {
        const target = reports.find(r => String(r.id) === String(btn.dataset.id));
        if (target) openReportLifecycleModal(target);
      };
    });
  }

  // =========================================================================
  // Render Full My Reports Tab Table
  // =========================================================================
  function renderMyReportsFullTable(reports) {
    const tbody = document.getElementById("myReportsFullTableBody");
    if (!tbody) return;

    // Update filter counts
    document.getElementById("pillCountAll").textContent = reports.length;
    document.getElementById("pillCountPending").textContent = reports.filter(r => {
      const s = (r.status || "").toUpperCase();
      const rs = (r.rescueStatus || "").toUpperCase();
      if (s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED") return false;
      return s === "PENDING";
    }).length;
    document.getElementById("pillCountReview").textContent = reports.filter(r => {
      const s = (r.status || "").toUpperCase();
      const rs = (r.rescueStatus || "").toUpperCase();
      if (s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED") return false;
      return s === "UNDER_REVIEW";
    }).length;
    document.getElementById("pillCountVerified").textContent = reports.filter(r => (r.status || "").toUpperCase() === "VERIFIED").length;
    document.getElementById("pillCountAssigned").textContent = reports.filter(r => {
      const s = (r.status || "").toUpperCase();
      const rs = (r.rescueStatus || "").toUpperCase();
      if (s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED") return false;
      return s === "ASSIGNED" || s === "RESPONDING" || rs === "ASSIGNED" || rs === "ACCEPTED" || rs === "EN_ROUTE" || rs === "ON_SCENE";
    }).length;
    document.getElementById("pillCountResolved").textContent = reports.filter(r => {
      const s = (r.status || "").toUpperCase();
      const rs = (r.rescueStatus || "").toUpperCase();
      return s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED";
    }).length;
    document.getElementById("pillCountRejected").textContent = reports.filter(r => (r.status || "").toUpperCase() === "REJECTED").length;

    let filtered = reports;
    if (activeMyReportsFilter === "pending") {
      filtered = reports.filter(r => (r.status || "").toUpperCase() === "PENDING");
    } else if (activeMyReportsFilter === "under_review") {
      filtered = reports.filter(r => (r.status || "").toUpperCase() === "UNDER_REVIEW");
    } else if (activeMyReportsFilter === "verified") {
      filtered = reports.filter(r => (r.status || "").toUpperCase() === "VERIFIED");
    } else if (activeMyReportsFilter === "assigned") {
      filtered = reports.filter(r => {
        const s = (r.status || "").toUpperCase();
        return s === "ASSIGNED" || s === "RESPONDING";
      });
    } else if (activeMyReportsFilter === "resolved") {
      filtered = reports.filter(r => {
        const s = (r.status || "").toUpperCase();
        const rs = (r.rescueStatus || "").toUpperCase();
        return s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED";
      });
    } else if (activeMyReportsFilter === "rejected") {
      filtered = reports.filter(r => (r.status || "").toUpperCase() === "REJECTED");
    }

    if (filtered.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align: center; padding: 24px; color: var(--color-text-muted);">
            No incident reports found for this filter.
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = filtered.map(r => {
      const ref = r.id ? `#${r.id}` : (r.localId || "INC");
      const loc = (r.location?.roadName || r.locationDescription || "Regional Slope");
      const timeStr = r.submittedAt || r.reportedTimestamp || r.observedAt || new Date().toISOString();
      const formattedDate = new Date(timeStr).toLocaleDateString(undefined, {
        month: "short", day: "numeric", year: "numeric"
      });
      const statusBadge = getStatusBadge(r.status, r.rescueStatus);
      const notes = r.reviewNotes || r.adminNotes || "—";

      return `
        <tr>
          <td><strong style="font-family: monospace; color: #C96F4F;">${ref}</strong></td>
          <td>
            <div style="display: flex; align-items: center; gap: 6px;">
              <img src="assets/icons/location-pin.png" alt="Location pin" class="location-pin-icon" style="height: 14px; width: 14px; object-fit: contain; flex-shrink: 0;" />
              <span style="max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(loc)}</span>
            </div>
          </td>
          <td>${formattedDate}</td>
          <td>${statusBadge}</td>
          <td style="max-width: 220px; font-size: 0.775rem; color: #4A5D48; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(notes)}</td>
          <td style="text-align: right;">
            <button type="button" class="btn-dashboard-primary btn-inspect-report" data-id="${r.id}" style="padding: 4px 10px; font-size: 0.75rem;">
              Inspect
            </button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-inspect-report").forEach(btn => {
      btn.onclick = () => {
        const target = reports.find(r => String(r.id) === String(btn.dataset.id));
        if (target) openReportLifecycleModal(target);
      };
    });
  }

  // =========================================================================
  // Report Lifecycle Modal
  // =========================================================================
  async function openReportLifecycleModal(report) {
    const modal = document.getElementById("reportLifecycleModal");
    if (!modal) return;

    // Fetch fresh report details from backend to ensure zero stale cache
    if (report && report.id) {
      try {
        const fresh = await fetchJson(`/api/reports/${report.id}`);
        if (fresh && fresh.id) {
          report = window.LW?.incidentsService?.normalizeReport ? window.LW.incidentsService.normalizeReport(fresh) : { ...report, ...fresh };
        }
      } catch (err) {
        console.warn("[Dashboard] Could not fetch fresh report details, using cached copy:", err);
      }
    }

    const ref = report.id ? `#${report.id}` : (report.localId || "INC");
    const category = report.incidentType || report.incidentCategory || "Hazard Observation";
    const loc = (report.location?.roadName || report.locationDescription || "Regional Slope");
    const lat = report.location?.latitude ?? report.latitude;
    const lng = report.location?.longitude ?? report.longitude;
    const coordsStr = (lat != null && lng != null) ? `Lat: ${Number(lat).toFixed(4)}°, Lng: ${Number(lng).toFixed(4)}°` : "Not provided";
    const timeStr = report.submittedAt || report.reportedTimestamp || report.observedAt || new Date().toISOString();

    document.getElementById("modalReportRef").textContent = ref;
    document.getElementById("modalReportCategory").textContent = category;
    document.getElementById("modalReportLocation").textContent = loc;
    document.getElementById("modalReportCoords").textContent = coordsStr;
    document.getElementById("modalReportTime").textContent = new Date(timeStr).toLocaleString();
    document.getElementById("modalReportDescription").textContent = report.description || "No description provided.";

    // Render 6-Stage Timeline with live rescue operation progress
    document.getElementById("modalLifecycleTimeline").innerHTML = buildLifecycleHtml(report.status, report.reviewNotes || report.adminNotes, report);

    // Photos
    const photosContainer = document.getElementById("modalReportPhotos");
    const photos = report.photos || [];
    if (photos.length > 0) {
      photosContainer.innerHTML = photos.map((p, idx) => {
        const url = p.url || p.previewUrl || (p.id ? `/api/reports/${report.id}/photos/${p.id}` : "");
        return `
          <a href="${url}" target="_blank" title="View Photograph ${idx + 1}" style="display: block; width: 88px; height: 72px; border-radius: 4px; overflow: hidden; border: 1px solid #D9C9B2;">
            <img src="${url}" alt="Incident Photo ${idx + 1}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.style.display='none'">
          </a>
        `;
      }).join("");
    } else {
      photosContainer.innerHTML = `<span style="font-size: 0.8125rem; color: #5E6F5C;">No photographs attached to this report.</span>`;
    }

    modal.style.display = "flex";
  }

  function closeReportLifecycleModal() {
    const modal = document.getElementById("reportLifecycleModal");
    if (modal) modal.style.display = "none";
  }

  function getRescueActionText(rStatus) {
    const s = (rStatus || "").toUpperCase();
    if (s === "COMPLETED") return "Rescue operation completed";
    if (s === "ON_SCENE") return "Rescue team has reached the location";
    if (s === "EN_ROUTE") return "Rescue team is on the way";
    if (s === "ACCEPTED") return "Rescue team accepted";
    if (s === "ASSIGNED") return "Rescue team assigned";
    return null;
  }

  function buildLifecycleHtml(status, reviewNotes, report = {}) {
    const norm = (status || "PENDING").toUpperCase().replace(/\s+/g, "_");
    const rStatus = (report.rescueStatus || (report.rescueAssignment && report.rescueAssignment.status) || "").toUpperCase();
    const isCompleted = (norm === "RESOLVED" || norm === "COMPLETED" || rStatus === "COMPLETED");

    // 6 Stages: Submitted -> Under Review -> Verified / Rejected -> Assigned -> Responding -> Completed
    const step1Cls = "completed"; // Submitted

    let step2Cls = "";
    if (isCompleted || norm === "VERIFIED" || norm === "REJECTED" || norm === "ASSIGNED" || norm === "RESPONDING" || rStatus) {
      step2Cls = "completed";
    } else if (norm === "UNDER_REVIEW") {
      step2Cls = "active";
    }

    let step3Cls = "";
    let step3Label = "Verified";
    if (norm === "REJECTED") {
      step3Cls = "needs-info";
      step3Label = "Rejected";
    } else if (isCompleted || norm === "ASSIGNED" || norm === "RESPONDING" || rStatus) {
      step3Cls = "completed";
    } else if (norm === "VERIFIED") {
      step3Cls = "active";
    }

    let step4Cls = "";
    if (isCompleted || norm === "RESPONDING" || rStatus === "EN_ROUTE" || rStatus === "ON_SCENE") {
      step4Cls = "completed";
    } else if (norm === "ASSIGNED" || rStatus === "ASSIGNED" || rStatus === "ACCEPTED") {
      step4Cls = (rStatus === "ACCEPTED") ? "completed" : "active";
    }

    let step5Cls = "";
    if (isCompleted) {
      step5Cls = "completed";
    } else if (norm === "RESPONDING" || rStatus === "EN_ROUTE" || rStatus === "ON_SCENE") {
      step5Cls = (rStatus === "ON_SCENE") ? "completed" : "active";
    }

    let step6Cls = "";
    if (isCompleted) {
      step6Cls = "completed";
    }

    const actionText = getRescueActionText(rStatus);

    return `
      <div class="lifecycle-timeline" aria-label="Incident Lifecycle Progress">
        <div class="lifecycle-step ${step1Cls}">
          <div class="lifecycle-circle">${step1Cls === 'completed' ? '✓' : '1'}</div>
          <div class="lifecycle-label">Submitted</div>
        </div>
        <div class="lifecycle-step ${step2Cls}">
          <div class="lifecycle-circle">${step2Cls === 'completed' ? '✓' : '2'}</div>
          <div class="lifecycle-label">Under Review</div>
        </div>
        <div class="lifecycle-step ${step3Cls}">
          <div class="lifecycle-circle">${step3Cls === 'completed' ? '✓' : (step3Cls === 'needs-info' ? '!' : '3')}</div>
          <div class="lifecycle-label">${step3Label}</div>
        </div>
        <div class="lifecycle-step ${step4Cls}">
          <div class="lifecycle-circle">${step4Cls === 'completed' ? '✓' : '4'}</div>
          <div class="lifecycle-label">Assigned</div>
        </div>
        <div class="lifecycle-step ${step5Cls}">
          <div class="lifecycle-circle">${step5Cls === 'completed' ? '✓' : '5'}</div>
          <div class="lifecycle-label">Responding</div>
        </div>
        <div class="lifecycle-step ${step6Cls}">
          <div class="lifecycle-circle">${step6Cls === 'completed' ? '✓' : '6'}</div>
          <div class="lifecycle-label">Completed</div>
        </div>
      </div>

      ${isCompleted ? `
        <div style="background: #F0FDF4; border: 1px solid #86EFAC; border-left: 4px solid #16A34A; border-radius: 4px; padding: 12px 14px; margin-top: 12px; font-size: 0.8125rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
            <strong style="color: #166534; font-size: 0.875rem;">Status: COMPLETED</strong>
            <span style="font-size: 0.75rem; color: #15803D; font-weight: 700;">✓ Rescue Operation Completed</span>
          </div>
          ${report.rescueTeamName ? `<div style="margin-bottom: 4px; color: #2E3A2F;"><strong>Assigned Rescue Team:</strong> ${escapeHtml(report.rescueTeamName)}</div>` : ''}
          ${report.rescueCompletedAt ? `<div style="margin-bottom: 4px; color: #2E3A2F;"><strong>Rescue Completed At:</strong> ${new Date(report.rescueCompletedAt).toLocaleString()}</div>` : ''}
          ${(report.rescueNotes || reviewNotes) ? `
            <div style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #86EFAC; color: #2E3A2F;">
              <strong>Rescue Completion Remarks:</strong>
              <p style="margin: 4px 0 0 0; line-height: 1.4;">${escapeHtml(report.rescueNotes || reviewNotes)}</p>
            </div>
          ` : ''}
        </div>
      ` : (actionText ? `
        <div style="background: #EFF6FF; border: 1px solid #BFDBFE; border-left: 4px solid #2563EB; border-radius: 4px; padding: 10px 14px; margin-top: 10px; font-size: 0.8125rem;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <strong style="color: #1E40AF;">Rescue Status: ${escapeHtml(actionText)}</strong>
            <span style="font-size: 0.75rem; color: #2563EB; font-weight: 700;">${rStatus}</span>
          </div>
          ${report.rescueTeamName ? `<p style="margin: 4px 0 0 0; color: #1E3A8A;">Assigned Team: ${escapeHtml(report.rescueTeamName)}</p>` : ''}
          ${report.rescueNotes ? `<p style="margin: 4px 0 0 0; color: #1E3A8A;">Field Notes: ${escapeHtml(report.rescueNotes)}</p>` : ''}
        </div>
      ` : '')}

      ${!isCompleted && norm === 'REJECTED' && reviewNotes ? `
        <div style="background: #FFF8F6; border: 1px solid #D9C9B2; border-left: 4px solid #C96F4F; border-radius: 4px; padding: 10px 14px; margin-top: 10px; font-size: 0.8125rem;">
          <strong style="color: #C96F4F; display: block; margin-bottom: 2px;">Authority Review Notes (Rejection):</strong>
          <p style="margin: 0; color: #2E3A2F;">${escapeHtml(reviewNotes)}</p>
        </div>
      ` : ''}
      ${!isCompleted && norm === 'VERIFIED' && reviewNotes ? `
        <div style="background: #F8F6EE; border: 1px solid #D9C9B2; border-left: 4px solid #6B7F5B; border-radius: 4px; padding: 10px 14px; margin-top: 10px; font-size: 0.8125rem;">
          <strong style="color: #6B7F5B; display: block; margin-bottom: 2px;">Verification Notes:</strong>
          <p style="margin: 0; color: #2E3A2F;">${escapeHtml(reviewNotes)}</p>
        </div>
      ` : ''}
    `;
  }

  function getStatusBadge(status, rescueStatus) {
    const s = (status || "PENDING").toUpperCase();
    const rs = (rescueStatus || "").toUpperCase();

    if (s === "RESOLVED" || s === "COMPLETED" || rs === "COMPLETED") {
      return `<span class="citizen-alert-badge" style="background: #15803D; color: #FFFFFF; font-weight: 700;">Completed</span>`;
    } else if (rs === "ON_SCENE") {
      return `<span class="citizen-alert-badge" style="background: #0284C7; color: #FFFFFF; font-weight: 700;">On Scene</span>`;
    } else if (rs === "EN_ROUTE") {
      return `<span class="citizen-alert-badge" style="background: #2563EB; color: #FFFFFF; font-weight: 700;">En Route</span>`;
    } else if (rs === "ACCEPTED") {
      return `<span class="citizen-alert-badge" style="background: #4F46E5; color: #FFFFFF; font-weight: 700;">Team Accepted</span>`;
    } else if (rs === "ASSIGNED" || s === "ASSIGNED" || s === "RESPONDING") {
      return `<span class="citizen-alert-badge" style="background: #2563EB; color: #FFFFFF; font-weight: 700;">Assigned</span>`;
    } else if (s === "VERIFIED") {
      return `<span class="citizen-alert-badge info" style="background: #2E3A2F; color: #FFFFFF; font-weight: 700;">Verified</span>`;
    } else if (s === "UNDER_REVIEW") {
      return `<span class="citizen-alert-badge moderate" style="background: #C58A2B; color: #FFFFFF; font-weight: 700;">Under Review</span>`;
    } else if (s === "REJECTED") {
      return `<span class="citizen-alert-badge critical" style="background: #C96F4F; color: #FFFFFF; font-weight: 700;">Rejected</span>`;
    } else {
      return `<span class="citizen-alert-badge" style="background: #5E6F5C; color: #FFFFFF; font-weight: 700;">Pending</span>`;
    }
  }

    return `<span class="citizen-alert-badge" style="background: #6B7F5B; color: #FFFFFF;">Pending</span>`;
  }

  function escapeHtml(str) {
    if (str == null) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // =========================================================================
  // Tab Routing & Navigation
  // =========================================================================
  function setupTabRouting() {
    const urlParams = new URLSearchParams(window.location.search);
    const initialTab = urlParams.get("tab") || "overview";
    switchTab(initialTab, false);

    document.querySelectorAll("[data-nav-tab]").forEach(item => {
      item.onclick = (e) => {
        e.preventDefault();
        const tab = item.dataset.navTab;
        switchTab(tab, true);
      };
    });

    window.addEventListener("popstate", (e) => {
      const stateTab = e.state?.tab || new URLSearchParams(window.location.search).get("tab") || "overview";
      switchTab(stateTab, false);
    });

    // Quick Action button handlers
    document.getElementById("qaTrackReports")?.addEventListener("click", () => switchTab("myreports", true));
    document.getElementById("qaOfficialAlerts")?.addEventListener("click", () => switchTab("alerts", true));
    document.getElementById("qaCitizenProfile")?.addEventListener("click", () => switchTab("profile", true));
  }

  function switchTab(tabId, pushHistory = true) {
    document.querySelectorAll(".dashboard-tab-view").forEach(v => v.classList.remove("active"));
    document.querySelectorAll("[data-nav-tab]").forEach(b => {
      if (b.dataset.navTab === tabId) b.classList.add("active");
      else b.classList.remove("active");
    });

    const targetView = document.getElementById(`tabView_${tabId}`);
    if (targetView) {
      targetView.classList.add("active");
    } else {
      document.getElementById("tabView_overview")?.classList.add("active");
      tabId = "overview";
    }

    const breadcrumbCurrent = document.getElementById("topbarBreadcrumbCurrent");
    if (breadcrumbCurrent) {
      const tabNames = {
        overview: "Overview",
        myreports: "My Reports",
        alerts: "Official Alerts",
        profile: "Citizen Profile"
      };
      breadcrumbCurrent.textContent = tabNames[tabId] || "Dashboard";
    }

    if (pushHistory) {
      const newUrl = tabId === "overview" ? window.location.pathname : `${window.location.pathname}?tab=${tabId}`;
      window.history.pushState({ tab: tabId }, "", newUrl);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }

    document.getElementById("dashboardSidebar")?.classList.remove("mobile-open");
  }

  // =========================================================================
  // User Profile & Shell Info
  // =========================================================================
  function populateUserInfo(currentUser) {
    const avatar = document.getElementById("sidebarUserAvatar");
    const nameEl = document.getElementById("sidebarUserName");
    const roleEl = document.getElementById("sidebarUserRole");

    const name = currentUser.fullName || currentUser.name || "Citizen Observer";
    const initial = name.charAt(0).toUpperCase();

    if (avatar) avatar.textContent = initial;
    if (nameEl) nameEl.textContent = name;
    if (roleEl) roleEl.innerHTML = `CITIZEN &bull; Registered Monitor`;

    const pAvatar = document.getElementById("profileAvatar");
    const pName = document.getElementById("profileName");
    const pEmail = document.getElementById("profileEmail");
    const pRole = document.getElementById("profileRole");
    const pDistrict = document.getElementById("profileDistrict");

    if (pAvatar) pAvatar.textContent = initial;
    if (pName) pName.textContent = name;
    if (pEmail) pEmail.textContent = currentUser.email || "citizen@slopeguard.org";
    if (pRole) pRole.textContent = "CITIZEN";
    if (pDistrict) pDistrict.textContent = currentUser.district || "North Eastern Region (NER)";
  }

  // =========================================================================
  // Event Listeners
  // =========================================================================
  function setupEventListeners() {
    // Topbar Search Handler
    let searchDebounce = null;
    const searchInput = document.getElementById("topbarSearchInput");
    if (searchInput) {
      searchInput.addEventListener("input", (e) => {
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => {
          const query = (e.target.value || "").trim().toLowerCase();
          filterReports(query);
        }, 150);
      });
    }

    // Mobile Sidebar Toggle
    const mobileToggle = document.getElementById("topbarMobileToggle");
    const sidebar = document.getElementById("dashboardSidebar");
    if (mobileToggle && sidebar) {
      mobileToggle.onclick = () => sidebar.classList.toggle("mobile-open");
    }

    // Sign Out
    document.querySelectorAll("[data-action='logout']").forEach(btn => {
      btn.onclick = async () => {
        await window.LW?.authService?.logout();
        window.location.replace("login.html");
      };
    });

    // My Reports Filter Pills
    document.querySelectorAll("#myReportsFilterBar .filter-pill").forEach(pill => {
      pill.onclick = () => {
        document.querySelectorAll("#myReportsFilterBar .filter-pill").forEach(p => p.classList.remove("active"));
        pill.classList.add("active");
        activeMyReportsFilter = pill.dataset.filter || "all";
        renderMyReportsFullTable(myPersonalReports);
      };
    });

    // Modal Close buttons
    document.getElementById("btnCloseLifecycleModal")?.addEventListener("click", closeReportLifecycleModal);
    document.getElementById("btnModalClose")?.addEventListener("click", closeReportLifecycleModal);
  }

  function filterReports(q) {
    if (!q) {
      renderRecentReportsOverview(myPersonalReports);
      renderMyReportsFullTable(myPersonalReports);
      return;
    }

    const filtered = myPersonalReports.filter(r => {
      const cat = String(r.incidentType || r.incidentCategory || "").toLowerCase();
      const loc = String(r.location?.roadName || r.locationDescription || "").toLowerCase();
      const desc = String(r.description || "").toLowerCase();
      const id = String(r.id || "").toLowerCase();
      return cat.includes(q) || loc.includes(q) || desc.includes(q) || id.includes(q);
    });

    renderRecentReportsOverview(filtered);
    renderMyReportsFullTable(filtered);
  }

  // Cross-tab sync
  if (window.LW?.reportsStorage?.onSync) {
    window.LW.reportsStorage.onSync(() => {
      loadDashboardData();
    });
  }
});
