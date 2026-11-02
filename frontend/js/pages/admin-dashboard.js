/**
 * SlopeGuard — Simplified Admin / DMO Dashboard Page Controller (admin-dashboard.js)
 * Incident review & verification, official alerts, and rescue team coordination.
 */

(function () {
  'use strict';

  // State Management
  let currentUser = null;
  let allReports = [];
  let allAlerts = [];
  let allMissions = [];
  let allTeams = [];

  let activeTab = 'overview'; // 'overview', 'incidents', 'alerts', 'rescue', 'teams'
  let incidentFilterStatus = 'all';
  let incidentSearchQuery = '';
  let alertFilterStatus = 'all';
  let alertSearchQuery = '';
  let rescueFilterStatus = 'all';
  let rescueSearchQuery = '';
  let teamFilterStatus = 'all';
  let teamSearchQuery = '';

  let selectedReport = null;
  let selectedMission = null;
  let selectedTeam = null;
  let editingAlertId = null;

  // 1. Authentication & Route Guard
  function initAuthGuard() {
    try {
      const raw = sessionStorage.getItem('sg_user') || localStorage.getItem('sg_user');
      currentUser = raw ? JSON.parse(raw) : null;
    } catch (e) {
      currentUser = null;
    }

    if (!currentUser) {
      window.location.replace('../login.html?next=' + encodeURIComponent('admin/dashboard.html'));
      return false;
    }

    const role = (currentUser.role || '').toUpperCase();
    if (role === 'OFFICER' || role === 'RESCUE_FORCE') {
      window.location.replace('../rescue-dashboard.html');
      return false;
    }
    if (role !== 'ADMIN') {
      window.location.replace('../dashboard.html');
      return false;
    }

    // Populate topbar profile
    const nameEl = document.getElementById('adminUserName');
    const avatarEl = document.getElementById('adminUserAvatar');
    const adminName = currentUser.name || currentUser.fullName || 'System Administrator';
    if (nameEl) nameEl.textContent = adminName;
    if (avatarEl) avatarEl.textContent = adminName.charAt(0).toUpperCase();

    // Sign Out
    const btnSignOut = document.getElementById('btnSignOut');
    if (btnSignOut) {
      btnSignOut.addEventListener('click', () => {
        sessionStorage.removeItem('sg_user');
        sessionStorage.removeItem('sg_token');
        localStorage.removeItem('sg_user');
        localStorage.removeItem('sg_token');
        window.location.replace('../login.html');
      });
    }

    return true;
  }

  // 2. Data Loading & Synchronization
  async function loadData() {
    await Promise.all([loadReports(), loadAlerts(), loadMissions(), loadTeams()]);
    renderSummaryCards();
    renderActiveView();
  }

  async function loadReports() {
    try {
      if (window.LW?.api) {
        // Try admin endpoint first
        const res = await window.LW.api.get('/api/admin/reports').catch(() => null);
        if (Array.isArray(res)) {
          allReports = res;
          return;
        }
      }
      if (window.LW?.reportsStorage?.getAllReports) {
        allReports = await window.LW.reportsStorage.getAllReports();
      } else {
        allReports = [];
      }
    } catch (err) {
      console.warn('[AdminDashboard] Falling back for reports:', err);
      allReports = [];
    }
  }

  async function loadAlerts() {
    try {
      if (window.LW?.api) {
        const res = await window.LW.api.get('/api/admin/alerts').catch(() => null);
        if (res && res.content && Array.isArray(res.content)) {
          allAlerts = res.content;
          return;
        } else if (Array.isArray(res)) {
          allAlerts = res;
          return;
        }

        // Fallback to public alerts
        const pub = await window.LW.api.get('/api/public/alerts').catch(() => null);
        if (Array.isArray(pub)) {
          allAlerts = pub;
          return;
        }
      }
      allAlerts = [];
    } catch (err) {
      console.warn('[AdminDashboard] Falling back for alerts:', err);
      allAlerts = [];
    }
  }

  async function loadMissions() {
    try {
      if (window.LW?.api) {
        const res = await window.LW.api.get('/api/rescue/missions').catch(() => null);
        if (Array.isArray(res)) {
          allMissions = res;
          return;
        }
      }
      if (window.LW?.reportsStorage?.getRescueMissions) {
        allMissions = await window.LW.reportsStorage.getRescueMissions();
      } else {
        allMissions = [];
      }
    } catch (err) {
      console.warn('[AdminDashboard] Falling back for missions:', err);
      allMissions = [];
    }
  }

  async function loadTeams() {
    try {
      if (window.LW?.api) {
        const res = await window.LW.api.get('/api/admin/rescue-teams').catch(() => null);
        if (Array.isArray(res)) {
          allTeams = res;
          return;
        }
      }
      allTeams = [];
    } catch (err) {
      console.warn('[AdminDashboard] Falling back for teams:', err);
      allTeams = [];
    }
  }

  // 3. Render 5 Summary Metric Cards
  function renderSummaryCards() {
    const totalReports = allReports.length;

    const pendingReviews = allReports.filter(r => {
      const s = (r.status || '').toUpperCase();
      return s === 'PENDING' || s === 'SUBMITTED' || s === 'UNDER_REVIEW' || s.includes('PENDING');
    }).length;

    const verifiedReports = allReports.filter(r => {
      const s = (r.status || '').toUpperCase();
      return s === 'VERIFIED' || s === 'RESOLVED';
    }).length;

    const activeAlerts = allAlerts.filter(a => {
      const s = (a.status || '').toUpperCase();
      return s === 'PUBLISHED' || s === 'ACTIVE';
    }).length;

    const activeAssignments = allMissions.filter(m => {
      const stage = (m.operationalStage || '').toUpperCase();
      const status = (m.status || '').toUpperCase();
      return stage === 'ASSIGNED' || stage === 'ACCEPTED' || stage === 'EN_ROUTE' || stage === 'ON_SCENE' || stage === 'IN_PROGRESS' || status === 'ASSIGNED' || status === 'RESPONDING';
    }).length;

    // Rescue Team Metrics
    const totalTeams = allTeams.length;
    const pendingTeams = allTeams.filter(t => (t.verificationStatus || '').toUpperCase() === 'PENDING').length;
    const approvedTeams = allTeams.filter(t => (t.verificationStatus || '').toUpperCase() === 'APPROVED').length;
    const activeTeams = allTeams.filter(t => (t.operationalStatus || '').toUpperCase() === 'ACTIVE' && (t.availabilityStatus || '').toUpperCase() === 'AVAILABLE').length;

    const elTotal = document.getElementById('statTotalReports');
    const elPending = document.getElementById('statPendingReviews');
    const elVerified = document.getElementById('statVerifiedReports');
    const elAlerts = document.getElementById('statActiveAlerts');
    const elRescue = document.getElementById('statActiveAssignments');

    const elTotalTeams = document.getElementById('statTotalTeams');
    const elPendingTeams = document.getElementById('statPendingTeams');
    const elApprovedTeams = document.getElementById('statApprovedTeams');
    const elActiveTeams = document.getElementById('statActiveTeams');

    if (elTotal) elTotal.textContent = totalReports;
    if (elPending) elPending.textContent = pendingReviews;
    if (elVerified) elVerified.textContent = verifiedReports;
    if (elAlerts) elAlerts.textContent = activeAlerts;
    if (elRescue) elRescue.textContent = activeAssignments;

    if (elTotalTeams) elTotalTeams.textContent = totalTeams;
    if (elPendingTeams) elPendingTeams.textContent = pendingTeams;
    if (elApprovedTeams) elApprovedTeams.textContent = approvedTeams;
    if (elActiveTeams) elActiveTeams.textContent = activeTeams;

    // Update tab badges
    const badgePendingTab = document.getElementById('badgePendingTab');
    if (badgePendingTab) badgePendingTab.textContent = pendingReviews;

    const badgeAlertsTab = document.getElementById('badgeAlertsTab');
    if (badgeAlertsTab) badgeAlertsTab.textContent = activeAlerts;

    const badgeRescueTab = document.getElementById('badgeRescueTab');
    if (badgeRescueTab) badgeRescueTab.textContent = activeAssignments;

    const badgeTeamsTab = document.getElementById('badgeTeamsTab');
    if (badgeTeamsTab) badgeTeamsTab.textContent = pendingTeams > 0 ? pendingTeams : totalTeams;

    const badgeTeamsCount = document.getElementById('badgeTeamsCount');
    if (badgeTeamsCount) badgeTeamsCount.textContent = `${totalTeams} Teams`;
  }

  // 4. Render Active View Based on Selected Tab
  function renderActiveView() {
    const secOverview = document.getElementById('secOverview');
    const secIncidents = document.getElementById('secIncidents');
    const secAlerts = document.getElementById('secAlerts');
    const secRescue = document.getElementById('secRescue');
    const secTeams = document.getElementById('secTeams');

    if (secOverview) secOverview.style.display = activeTab === 'overview' ? 'block' : 'none';
    if (secIncidents) secIncidents.style.display = activeTab === 'incidents' ? 'block' : 'none';
    if (secAlerts) secAlerts.style.display = activeTab === 'alerts' ? 'block' : 'none';
    if (secRescue) secRescue.style.display = activeTab === 'rescue' ? 'block' : 'none';
    if (secTeams) secTeams.style.display = activeTab === 'teams' ? 'block' : 'none';

    if (activeTab === 'overview') {
      renderOverviewTables();
    } else if (activeTab === 'incidents') {
      renderIncidentsTable();
    } else if (activeTab === 'alerts') {
      renderAlertsTable();
    } else if (activeTab === 'rescue') {
      renderRescueTable();
    } else if (activeTab === 'teams') {
      renderTeamsTable();
    }
  }

  // Helpers
  function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function formatCategory(cat) {
    if (!cat) return 'Incident';
    const c = String(cat).replace(/_/g, ' ').toLowerCase();
    return c.charAt(0).toUpperCase() + c.slice(1);
  }

  function formatDate(dtStr) {
    if (!dtStr) return '—';
    try {
      const d = new Date(dtStr);
      if (isNaN(d.getTime())) return dtStr;
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
      return dtStr;
    }
  }

  function getStatusBadge(status) {
    const s = (status || 'PENDING').toUpperCase();
    if (s.includes('VERIFIED')) return '<span class="badge-status badge-verified">Verified</span>';
    if (s.includes('RESOLVED')) return '<span class="badge-status badge-resolved">Resolved</span>';
    if (s.includes('REJECTED')) return '<span class="badge-status badge-rejected">Rejected</span>';
    if (s.includes('ASSIGNED')) return '<span class="badge-status badge-assigned">Assigned</span>';
    if (s.includes('RESPONDING') || s.includes('SCENE') || s.includes('ROUTE')) return '<span class="badge-status badge-responding">Responding</span>';
    return '<span class="badge-status badge-pending">Pending Review</span>';
  }

  function getSeverityBadge(sev) {
    const s = (sev || 'MEDIUM').toUpperCase();
    if (s === 'HIGH' || s === 'CRITICAL' || s === 'SEVERE') return `<span class="badge-sev badge-sev-high">${s}</span>`;
    if (s === 'LOW') return `<span class="badge-sev badge-sev-low">LOW</span>`;
    return `<span class="badge-sev badge-sev-medium">MEDIUM</span>`;
  }

  // 5. Render Overview Tables
  function renderOverviewTables() {
    // Recent Reports (top 5)
    const tbodyRecent = document.getElementById('tbodyRecentReports');
    if (tbodyRecent) {
      const sorted = [...allReports].sort((a, b) => new Date(b.reportedTimestamp || b.timestamp || 0) - new Date(a.reportedTimestamp || a.timestamp || 0)).slice(0, 5);
      if (sorted.length === 0) {
        tbodyRecent.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 24px; color: var(--admin-text-muted);">No reports found.</td></tr>`;
      } else {
        tbodyRecent.innerHTML = sorted.map(r => renderReportRow(r, false)).join('');
      }
    }

    // Reports Awaiting Verification (top 5 pending)
    const tbodyPending = document.getElementById('tbodyPendingQueue');
    if (tbodyPending) {
      const pendingList = allReports.filter(r => {
        const s = (r.status || '').toUpperCase();
        return s === 'PENDING' || s === 'SUBMITTED' || s === 'UNDER_REVIEW' || s.includes('PENDING');
      }).slice(0, 5);

      if (pendingList.length === 0) {
        tbodyPending.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 24px; color: var(--admin-text-muted);">No pending reviews. All clear!</td></tr>`;
      } else {
        tbodyPending.innerHTML = pendingList.map(r => renderReportRow(r, true)).join('');
      }
    }

    // Active Official Alerts (top 5)
    const tbodyAlertsOverview = document.getElementById('tbodyAlertsOverview');
    if (tbodyAlertsOverview) {
      const activeAlertsList = allAlerts.filter(a => {
        const s = (a.status || '').toUpperCase();
        return s === 'PUBLISHED' || s === 'ACTIVE';
      }).slice(0, 5);

      if (activeAlertsList.length === 0) {
        tbodyAlertsOverview.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 24px; color: var(--admin-text-muted);">No active published hazard alerts.</td></tr>`;
      } else {
        tbodyAlertsOverview.innerHTML = activeAlertsList.map(a => renderAlertRow(a, false)).join('');
      }
    }

    // Current Rescue Assignments (top 5)
    const tbodyRescueOverview = document.getElementById('tbodyRescueOverview');
    if (tbodyRescueOverview) {
      const activeMissions = allMissions.filter(m => (m.operationalStage || '').toUpperCase() !== 'REVOKED').slice(0, 5);
      if (activeMissions.length === 0) {
        tbodyRescueOverview.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 24px; color: var(--admin-text-muted);">No active rescue missions.</td></tr>`;
      } else {
        tbodyRescueOverview.innerHTML = activeMissions.map(m => renderRescueRow(m, false)).join('');
      }
    }
  }

  // 6. Render Full Incidents Management Table
  function renderIncidentsTable() {
    const tbody = document.getElementById('tbodyIncidentReports');
    const emptyState = document.getElementById('emptyIncidentsState');
    const badge = document.getElementById('badgeIncidentsCount');
    if (!tbody) return;

    const filtered = allReports.filter(r => {
      const s = (r.status || '').toUpperCase();
      if (incidentFilterStatus === 'pending' && !(s === 'PENDING' || s === 'SUBMITTED' || s === 'UNDER_REVIEW' || s.includes('PENDING'))) return false;
      if (incidentFilterStatus === 'verified' && s !== 'VERIFIED') return false;
      if (incidentFilterStatus === 'assigned' && s !== 'ASSIGNED' && s !== 'RESPONDING') return false;
      if (incidentFilterStatus === 'resolved' && s !== 'RESOLVED') return false;
      if (incidentFilterStatus === 'rejected' && s !== 'REJECTED') return false;

      if (incidentSearchQuery) {
        const q = incidentSearchQuery.toLowerCase();
        const idMatch = String(r.id || '').toLowerCase().includes(q);
        const catMatch = String(r.incidentCategory || '').toLowerCase().includes(q);
        const locMatch = String(r.locationDescription || '').toLowerCase().includes(q);
        const descMatch = String(r.description || '').toLowerCase().includes(q);
        if (!idMatch && !catMatch && !locMatch && !descMatch) return false;
      }
      return true;
    });

    if (badge) badge.textContent = `${filtered.length} Reports`;

    if (filtered.length === 0) {
      tbody.innerHTML = '';
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';
    tbody.innerHTML = filtered.map(r => renderReportRow(r, false)).join('');
  }

  function renderReportRow(r, isPriorityQueue = false) {
    const lat = r.latitude != null ? Number(r.latitude) : null;
    const lng = r.longitude != null ? Number(r.longitude) : null;
    const coords = (lat != null && lng != null) ? `${lat.toFixed(4)}, ${lng.toFixed(4)}` : '—';
    const loc = r.locationDescription || 'Regional Sector';
    const date = formatDate(r.reportedTimestamp || r.timestamp);
    const sev = r.severity || r.riskSeverity || 'MEDIUM';

    return `
      <tr>
        <td class="cell-id">#SG-${r.id}</td>
        <td><strong>${escapeHtml(formatCategory(r.incidentCategory))}</strong></td>
        <td>
          <div class="cell-location" title="${escapeHtml(loc)}">
            <img src="../assets/icons/location-pin.png" alt="Location pin" class="location-pin-img">
            <span class="location-text">${escapeHtml(loc)}</span>
          </div>
        </td>
        <td class="cell-coords">${coords}</td>
        <td>${date}</td>
        <td>${getSeverityBadge(sev)}</td>
        <td>${getStatusBadge(r.status)}</td>
        <td style="text-align: right; white-space: nowrap;">
          <button type="button" class="btn-compact" data-action="view-report" data-id="${r.id}">
            Review &amp; Details
          </button>
        </td>
      </tr>
    `;
  }

  // 7. Render Full Official Alerts Table
  function renderAlertsTable() {
    const tbody = document.getElementById('tbodyOfficialAlerts');
    const emptyState = document.getElementById('emptyAlertsState');
    const badge = document.getElementById('badgeAlertsCount');
    if (!tbody) return;

    const filtered = allAlerts.filter(a => {
      const s = (a.status || 'PUBLISHED').toUpperCase();
      if (alertFilterStatus === 'published' && s !== 'PUBLISHED') return false;
      if (alertFilterStatus === 'draft' && s !== 'DRAFT' && s !== 'PENDING_REVIEW') return false;
      if (alertFilterStatus === 'resolved' && s !== 'RESOLVED' && s !== 'WITHDRAWN') return false;

      if (alertSearchQuery) {
        const q = alertSearchQuery.toLowerCase();
        const idMatch = String(a.id || '').toLowerCase().includes(q);
        const titleMatch = String(a.title || '').toLowerCase().includes(q);
        const locMatch = String(a.areaName || a.location || '').toLowerCase().includes(q);
        if (!idMatch && !titleMatch && !locMatch) return false;
      }
      return true;
    });

    if (badge) badge.textContent = `${filtered.length} Alerts`;

    if (filtered.length === 0) {
      tbody.innerHTML = '';
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';
    tbody.innerHTML = filtered.map(a => renderAlertRow(a, true)).join('');
  }

  function renderAlertRow(a, showActions = true) {
    const s = (a.status || 'PUBLISHED').toUpperCase();
    let statusBadge = '<span class="badge-status badge-published">Published</span>';
    if (s === 'DRAFT') statusBadge = '<span class="badge-status badge-draft">Draft</span>';
    else if (s === 'RESOLVED') statusBadge = '<span class="badge-status badge-resolved">Resolved</span>';
    else if (s === 'WITHDRAWN') statusBadge = '<span class="badge-status badge-withdrawn">Withdrawn</span>';

    const sev = a.severity || 'HIGH';
    const loc = a.areaName || a.location || 'North Eastern Region';
    const date = formatDate(a.publishedAt || a.createdAt);

    let actionButtons = '';
    if (showActions) {
      if (s === 'PUBLISHED') {
        actionButtons = `
          <button type="button" class="btn-compact" data-action="resolve-alert" data-id="${a.id}">Resolve</button>
          <button type="button" class="btn-compact" data-action="withdraw-alert" data-id="${a.id}">Withdraw</button>
        `;
      } else if (s === 'DRAFT' || s === 'PENDING_REVIEW') {
        actionButtons = `
          <button type="button" class="btn-compact" style="background:var(--admin-forest);color:#fff;" data-action="publish-alert" data-id="${a.id}">Publish</button>
          <button type="button" class="btn-compact" data-action="edit-alert" data-id="${a.id}">Edit</button>
        `;
      } else {
        actionButtons = `<button type="button" class="btn-compact" data-action="view-alert" data-id="${a.id}">View</button>`;
      }
    }

    return `
      <tr>
        <td class="cell-id">#ALT-${a.id}</td>
        <td><strong>${escapeHtml(a.title)}</strong></td>
        <td>${getSeverityBadge(sev)}</td>
        <td>
          <div class="cell-location">
            <img src="../assets/icons/location-pin.png" alt="Location pin" class="location-pin-img">
            <span class="location-text">${escapeHtml(loc)}</span>
          </div>
        </td>
        <td>${statusBadge}</td>
        <td>${date}</td>
        ${showActions ? `<td style="text-align: right; white-space: nowrap;"><div style="display:inline-flex;gap:6px;">${actionButtons}</div></td>` : ''}
      </tr>
    `;
  }

  // 8. Render Full Rescue Assignments Table
  function renderRescueTable() {
    const tbody = document.getElementById('tbodyRescueAssignments');
    const emptyState = document.getElementById('emptyRescueState');
    const badge = document.getElementById('badgeRescueCount');
    if (!tbody) return;

    const filtered = allMissions.filter(m => {
      const stage = (m.operationalStage || '').toUpperCase();
      if (stage === 'REVOKED') return false;

      if (rescueFilterStatus === 'new' && stage !== 'ASSIGNED') return false;
      if (rescueFilterStatus === 'accepted' && stage !== 'ACCEPTED') return false;
      if (rescueFilterStatus === 'in_progress' && stage !== 'EN_ROUTE' && stage !== 'ON_SCENE' && stage !== 'IN_PROGRESS') return false;
      if (rescueFilterStatus === 'completed' && stage !== 'COMPLETED' && stage !== 'AWAITING_VERIFICATION') return false;

      if (rescueSearchQuery) {
        const q = rescueSearchQuery.toLowerCase();
        const idMatch = String(m.id || '').toLowerCase().includes(q);
        const teamMatch = String(m.assignedTeam || '').toLowerCase().includes(q);
        const locMatch = String(m.locationDescription || '').toLowerCase().includes(q);
        if (!idMatch && !teamMatch && !locMatch) return false;
      }
      return true;
    });

    if (badge) badge.textContent = `${filtered.length} Missions`;

    if (filtered.length === 0) {
      tbody.innerHTML = '';
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';
    tbody.innerHTML = filtered.map(m => renderRescueRow(m, true)).join('');
  }

  function renderRescueRow(m, showActions = true) {
    const stage = (m.operationalStage || 'ASSIGNED').toUpperCase();
    let stageBadge = '<span class="badge-status badge-assigned">Assigned</span>';
    if (stage === 'ACCEPTED') stageBadge = '<span class="badge-status badge-assigned">Accepted</span>';
    else if (stage === 'EN_ROUTE') stageBadge = '<span class="badge-status badge-responding">En Route</span>';
    else if (stage === 'ON_SCENE' || stage === 'IN_PROGRESS') stageBadge = '<span class="badge-status badge-responding">On Scene</span>';
    else if (stage === 'AWAITING_VERIFICATION' || stage === 'COMPLETED') stageBadge = '<span class="badge-status badge-resolved">Completed</span>';

    const priority = (m.assignedPriority || m.priority || 'HIGH').toUpperCase();
    const loc = m.locationDescription || 'Regional Corridor';
    const team = m.assignedTeam || 'Rescue Force Unit Alpha (NER)';

    // Summary of latest progress note
    let noteSnippet = 'Awaiting initial team response.';
    if (m.progressHistory && m.progressHistory.length > 0) {
      const last = m.progressHistory[m.progressHistory.length - 1];
      noteSnippet = `${last.siteAssessment || 'Assessment logged'}${last.roadPassability ? ` (${last.roadPassability})` : ''}`;
    } else if (m.outcomeReport && m.outcomeReport.resolutionSummary) {
      noteSnippet = m.outcomeReport.resolutionSummary;
    }

    let actionBtn = `<button type="button" class="btn-compact" data-action="view-mission" data-id="${m.id}">Details & Notes</button>`;
    if (stage === 'AWAITING_VERIFICATION' || stage === 'COMPLETED') {
      actionBtn += ` <button type="button" class="btn-compact" style="background:#059669;color:#fff;" data-action="close-incident" data-id="${m.id}">Close Incident</button>`;
    }

    return `
      <tr>
        <td class="cell-id">#SG-${m.id}</td>
        <td><strong>${escapeHtml(formatCategory(m.incidentCategory))}</strong></td>
        <td>${escapeHtml(team)}</td>
        <td>${getSeverityBadge(priority)}</td>
        <td>
          <div class="cell-location">
            <img src="../assets/icons/location-pin.png" alt="Location pin" class="location-pin-img">
            <span class="location-text">${escapeHtml(loc)}</span>
          </div>
        </td>
        <td>${stageBadge}</td>
        <td style="max-width:240px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${escapeHtml(noteSnippet)}">
          ${escapeHtml(noteSnippet)}
        </td>
        ${showActions ? `<td style="text-align: right; white-space: nowrap;"><div style="display:inline-flex;gap:6px;">${actionBtn}</div></td>` : ''}
      </tr>
    `;
  }

  // 9. Incident Review Modal Logic
  async function openIncidentDetails(reportId) {
    const report = allReports.find(r => String(r.id) === String(reportId));
    if (!report) return;
    selectedReport = report;

    const modal = document.getElementById('incidentModal');
    const modalTitle = document.getElementById('incidentModalTitle');
    const modalBody = document.getElementById('incidentModalBody');
    const modalFooter = document.getElementById('incidentModalFooter');

    if (!modal || !modalBody) return;

    modalTitle.innerHTML = `Incident Review: <span style="font-family:'JetBrains Mono',monospace;">#SG-${report.id}</span>`;

    const lat = report.latitude != null ? Number(report.latitude) : null;
    const lng = report.longitude != null ? Number(report.longitude) : null;
    const coords = (lat != null && lng != null) ? `${lat.toFixed(5)}, ${lng.toFixed(5)}` : 'Not specified';
    const loc = report.locationDescription || 'Regional Sector';
    const date = formatDate(report.reportedTimestamp || report.timestamp);
    const sev = (report.severity || report.riskSeverity || 'MEDIUM').toUpperCase();

    // Photographic Evidence
    let photosHtml = '<div style="font-size:0.8125rem;color:var(--admin-text-muted);">No incident photographs attached.</div>';
    let photosList = report.photos || [];
    if (window.LW?.api && photosList.length === 0) {
      try {
        const fetchedPhotos = await window.LW.api.get(`/api/reports/${report.id}/photos`).catch(() => []);
        if (Array.isArray(fetchedPhotos) && fetchedPhotos.length > 0) {
          photosList = fetchedPhotos;
        }
      } catch (e) {}
    }

    if (photosList.length > 0) {
      photosHtml = `
        <div class="modal-gallery">
          ${photosList.map(p => {
            const url = p.downloadUrl ? `http://localhost:8080${p.downloadUrl}` : (p.dataUrl || p.url);
            return `<img src="${escapeHtml(url)}" alt="Incident evidence photo" class="modal-thumb" onclick="window.open('${escapeHtml(url)}', '_blank')">`;
          }).join('')}
        </div>
      `;
    }

    modalBody.innerHTML = `
      <div class="modal-grid-2">
        <div class="modal-field">
          <span class="modal-field-label">Incident ID & Type</span>
          <span class="modal-field-value">#SG-${report.id} — ${escapeHtml(formatCategory(report.incidentCategory))}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Current Status</span>
          <div style="margin-top:2px;">${getStatusBadge(report.status)}</div>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Incident Location</span>
          <div style="display:flex;align-items:center;gap:6px;margin-top:2px;">
            <img src="../assets/icons/location-pin.png" alt="Location pin" class="location-pin-img">
            <span class="modal-field-value">${escapeHtml(loc)}</span>
          </div>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">GPS Coordinates</span>
          <span class="modal-field-value" style="font-family:'JetBrains Mono',monospace;">${coords}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Reported Date</span>
          <span class="modal-field-value">${date}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Current Severity</span>
          <div style="margin-top:2px;">${getSeverityBadge(sev)}</div>
        </div>
      </div>

      <div class="admin-form-group">
        <span class="admin-label">Citizen Description</span>
        <div class="modal-box">
          ${escapeHtml(report.description || 'No description provided by citizen.')}
        </div>
      </div>

      <div class="admin-form-group">
        <span class="admin-label">Attached Photographic Evidence</span>
        ${photosHtml}
      </div>

      <div class="admin-form-group" style="background:#FAF8F2;padding:12px;border:1px solid var(--admin-border);border-radius:6px;">
        <span class="admin-label" style="margin-bottom:6px;">Administrative Review & Severity Override</span>
        
        <div class="modal-grid-2" style="margin-bottom:8px;">
          <div class="admin-form-group">
            <label class="admin-label" for="selectReportSeverity">Manual Risk Severity</label>
            <select id="selectReportSeverity" class="admin-select">
              <option value="LOW" ${sev === 'LOW' ? 'selected' : ''}>Low — Minor hazard</option>
              <option value="MEDIUM" ${sev === 'MEDIUM' ? 'selected' : ''}>Medium — Active attention</option>
              <option value="HIGH" ${sev === 'HIGH' || sev === 'CRITICAL' ? 'selected' : ''}>High — Immediate danger</option>
            </select>
          </div>

          <div class="admin-form-group">
            <label class="admin-label" for="inputReviewNotes">Administrative Notes / Instructions</label>
            <input type="text" id="inputReviewNotes" class="admin-input" placeholder="e.g., Verified via local patrol..." value="${escapeHtml(report.reviewNotes || '')}">
          </div>
        </div>
      </div>
    `;

    // Footer actions
    const currentStatus = (report.status || '').toUpperCase();
    let actionButtons = `
      <button type="button" class="btn-danger" id="btnRejectReport">Reject Report</button>
      <button type="button" class="btn-success" id="btnVerifyReport">Verify Report</button>
    `;

    if (currentStatus === 'VERIFIED') {
      actionButtons = `
        <button type="button" class="btn-primary-admin" id="btnFindNearestModal" style="background:#059669;border-color:#059669;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="3 11 22 2 13 21 11 13 3 11"></polygon></svg>
          Find Nearest Rescue Teams
        </button>
        <button type="button" class="btn-compact" id="btnAssignFromModal">Direct Assign</button>
      `;
    } else if (currentStatus === 'RESOLVED') {
      actionButtons = `
        <span class="badge-status badge-resolved" style="margin-right:auto;">Incident Resolved & Closed</span>
      `;
    }

    modalFooter.innerHTML = `
      ${actionButtons}
      <button type="button" class="btn-compact" data-close-modal="incidentModal">Close</button>
    `;

    // Attach click listeners
    const btnVerify = document.getElementById('btnVerifyReport');
    if (btnVerify) btnVerify.onclick = () => handleAdjudicateReport(report.id, 'VERIFIED');

    const btnReject = document.getElementById('btnRejectReport');
    if (btnReject) btnReject.onclick = () => handleAdjudicateReport(report.id, 'REJECTED');

    const btnFindNearest = document.getElementById('btnFindNearestModal');
    if (btnFindNearest) {
      btnFindNearest.onclick = () => {
        closeModal('incidentModal');
        openNearestTeamsModal(report.id);
      };
    }

    const btnAssignFromModal = document.getElementById('btnAssignFromModal');
    if (btnAssignFromModal) {
      btnAssignFromModal.onclick = () => {
        closeModal('incidentModal');
        openAssignModal(report.id);
      };
    }

    openModal('incidentModal');
  }

  // Update Report Status with clean confirmation
  async function handleAdjudicateReport(reportId, newStatus) {
    const isVerify = newStatus === 'VERIFIED';
    const confirmed = await window.LW.ui.confirmAction({
      title: isVerify ? 'Confirm Report Verification' : 'Confirm Report Rejection',
      message: isVerify
        ? `Are you sure you want to verify Incident #SG-${reportId}? This marks the incident as an authentic hazard requiring response.`
        : `Are you sure you want to reject Incident #SG-${reportId}?`,
      confirmText: isVerify ? 'Verify Report' : 'Reject Report',
      variant: isVerify ? 'success' : 'danger'
    });
    if (!confirmed) return;

    const sevSelect = document.getElementById('selectReportSeverity');
    const notesInput = document.getElementById('inputReviewNotes');
    const notes = notesInput ? notesInput.value.trim() : '';

    const btnVerify = document.getElementById('btnVerifyReport');
    const btnReject = document.getElementById('btnRejectReport');
    if (btnVerify) btnVerify.disabled = true;
    if (btnReject) btnReject.disabled = true;

    try {
      if (window.LW?.api) {
        await window.LW.api.patch(`/api/admin/reports/${reportId}/status`, {
          status: newStatus,
          reviewNotes: notes || (isVerify ? 'Verified by DMO Administrator' : 'Rejected after inspection')
        });
      } else if (window.LW?.reportsStorage?.updateReportStatus) {
        await window.LW.reportsStorage.updateReportStatus(reportId, newStatus, notes);
      }

      closeModal('incidentModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(`Incident #SG-${reportId} marked as ${newStatus}.`, 'success');
      await loadData();
    } catch (err) {
      if (btnVerify) btnVerify.disabled = false;
      if (btnReject) btnReject.disabled = false;
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to update report', 'error');
    }
  }

  // 10. Official Alert Management Modals
  function openCreateAlertModal(alertToEdit = null) {
    editingAlertId = alertToEdit ? alertToEdit.id : null;
    const modal = document.getElementById('alertModal');
    const titleEl = document.getElementById('alertFormTitle');
    const inputTitle = document.getElementById('inputAlertTitle');
    const inputArea = document.getElementById('inputAlertArea');
    const selectSev = document.getElementById('selectAlertSeverity');
    const inputDesc = document.getElementById('inputAlertDesc');
    const inputActions = document.getElementById('inputAlertActions');

    if (alertToEdit) {
      if (titleEl) titleEl.textContent = 'Edit Official Alert';
      if (inputTitle) inputTitle.value = alertToEdit.title || '';
      if (inputArea) inputArea.value = alertToEdit.areaName || alertToEdit.location || '';
      if (selectSev) selectSev.value = alertToEdit.severity || 'HIGH';
      if (inputDesc) inputDesc.value = alertToEdit.description || '';
      if (inputActions) inputActions.value = alertToEdit.recommendedActions || alertToEdit.safetyInstructions || '';
    } else {
      if (titleEl) titleEl.textContent = 'Create Official Hazard Alert';
      if (inputTitle) inputTitle.value = '';
      if (inputArea) inputArea.value = '';
      if (selectSev) selectSev.value = 'HIGH';
      if (inputDesc) inputDesc.value = '';
      if (inputActions) inputActions.value = '';
    }

    openModal('alertModal');
  }

  let isSavingAlert = false;
  async function handleSaveAlert(publishImmediately = false) {
    if (isSavingAlert) return;
    const inputTitle = document.getElementById('inputAlertTitle');
    const inputArea = document.getElementById('inputAlertArea');
    const selectSev = document.getElementById('selectAlertSeverity');
    const inputDesc = document.getElementById('inputAlertDesc');
    const inputActions = document.getElementById('inputAlertActions');

    const title = inputTitle ? inputTitle.value.trim() : '';
    const areaName = inputArea ? inputArea.value.trim() : '';
    const severity = selectSev ? selectSev.value : 'HIGH';
    const description = inputDesc ? inputDesc.value.trim() : '';
    const recommendedActions = inputActions ? inputActions.value.trim() : '';

    if (!title || !areaName || !description) {
      if (window.LW?.ui?.toast) window.LW.ui.toast('Title, affected area, and description are required.', 'warning');
      return;
    }

    const btnSave = document.getElementById('btnSaveAlertDraft');
    const btnPublish = document.getElementById('btnPublishAlertNow');
    const activeBtn = publishImmediately ? btnPublish : btnSave;
    const origHtml = activeBtn ? activeBtn.innerHTML : '';

    isSavingAlert = true;
    if (btnSave) btnSave.disabled = true;
    if (btnPublish) btnPublish.disabled = true;
    if (activeBtn) activeBtn.innerHTML = `<span class="lw-btn-spinner"></span> ${publishImmediately ? 'Publishing...' : 'Saving...'}`;

    try {
      if (window.LW?.api) {
        let alertId = editingAlertId;
        if (alertId) {
          await window.LW.api.put(`/api/admin/alerts/${alertId}`, {
            title, areaName, severity, description, recommendedActions
          });
        } else {
          const created = await window.LW.api.post('/api/admin/alerts', {
            title, areaName, severity, description, recommendedActions
          });
          alertId = created?.id;
        }

        if (publishImmediately && alertId) {
          await window.LW.api.post(`/api/admin/alerts/${alertId}/publish`);
        }
      }

      closeModal('alertModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(publishImmediately ? 'Official alert published to public channels.' : 'Alert draft saved.', 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to save alert', 'error');
    } finally {
      if (btnSave) btnSave.disabled = false;
      if (btnPublish) btnPublish.disabled = false;
      if (activeBtn) activeBtn.innerHTML = origHtml;
      isSavingAlert = false;
    }
  }

  async function handlePublishAlert(alertId) {
    const alertObj = allAlerts.find(a => String(a.id) === String(alertId));
    const title = alertObj ? alertObj.title : `Alert #${alertId}`;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Broadcast Official Alert',
      message: `Broadcast official alert '${title}' immediately to all public channels and response units?`,
      confirmText: 'Broadcast Alert',
      variant: 'warning'
    });
    if (!confirmed) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.post(`/api/admin/alerts/${alertId}/publish`);
      }
      if (window.LW?.ui?.toast) window.LW.ui.toast('Official alert published successfully.', 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to publish alert', 'error');
    }
  }

  async function handleResolveAlert(alertId) {
    const alertObj = allAlerts.find(a => String(a.id) === String(alertId));
    const title = alertObj ? alertObj.title : `Alert #${alertId}`;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Resolve Official Alert',
      message: `Mark alert '${title}' as Resolved? Active warnings will be archived.`,
      confirmText: 'Resolve Alert',
      variant: 'success'
    });
    if (!confirmed) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.post(`/api/admin/alerts/${alertId}/resolve`, { reason: 'Hazard condition normalized' });
      }
      if (window.LW?.ui?.toast) window.LW.ui.toast('Alert marked as Resolved.', 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to resolve alert', 'error');
    }
  }

  async function handleWithdrawAlert(alertId) {
    const alertObj = allAlerts.find(a => String(a.id) === String(alertId));
    const title = alertObj ? alertObj.title : `Alert #${alertId}`;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Withdraw Official Alert',
      message: `Withdraw and deactivate official alert '${title}'?`,
      confirmText: 'Withdraw Alert',
      variant: 'danger'
    });
    if (!confirmed) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.post(`/api/admin/alerts/${alertId}/withdraw`, { reason: 'Withdrawn by DMO staff' });
      }
      if (window.LW?.ui?.toast) window.LW.ui.toast('Alert withdrawn.', 'info');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to withdraw alert', 'error');
    }
  }

  // 11. Rescue Team Assignment Modals
  function openAssignModal(reportId, preselectedTeamId = null, distanceKm = null) {
    const report = allReports.find(r => String(r.id) === String(reportId));
    if (!report) return;
    selectedReport = report;

    const modal = document.getElementById('assignModal');
    const headerTitle = document.getElementById('assignModalHeaderTitle');
    const inputId = document.getElementById('assignReportId');
    const inputLoc = document.getElementById('assignReportLoc');
    const distanceBadge = document.getElementById('assignDistanceBadge');
    const selectTeam = document.getElementById('selectAssignTeam');
    const inputInstructions = document.getElementById('assignInstructions');

    if (headerTitle) headerTitle.textContent = `Assign Incident #SG-${report.id} to Rescue Force`;
    if (inputId) inputId.textContent = `#SG-${report.id} — ${formatCategory(report.incidentCategory)}`;
    if (inputLoc) inputLoc.textContent = report.locationDescription || 'Regional Sector';

    if (distanceBadge) {
      if (distanceKm != null && distanceKm !== '') {
        distanceBadge.style.display = 'inline-flex';
        distanceBadge.className = 'distance-badge nearest-rank-1';
        distanceBadge.textContent = `Calculated GPS Distance: ${distanceKm} km away`;
      } else {
        distanceBadge.style.display = 'none';
      }
    }

    if (selectTeam) {
      const approvedTeams = allTeams.filter(t => (t.verificationStatus || '').toUpperCase() === 'APPROVED');
      if (approvedTeams.length > 0) {
        selectTeam.innerHTML = approvedTeams.map(t => {
          const isSelected = preselectedTeamId != null && String(t.id) === String(preselectedTeamId);
          const avail = (t.availabilityStatus || 'AVAILABLE').toUpperCase();
          const tag = avail !== 'AVAILABLE' ? ` (${avail})` : '';
          return `<option value="${t.id}" ${isSelected ? 'selected' : ''}>${escapeHtml(t.teamName)}${tag}</option>`;
        }).join('');
      } else {
        selectTeam.innerHTML = `<option value="1">Rescue Force Unit Alpha (NER)</option>`;
      }
    }

    if (inputInstructions) {
      inputInstructions.value = 'Deploy immediately to coordinate site, assess slope stability, establish security cordons, and coordinate traffic or evacuation.';
    }

    openModal('assignModal');
  }

  let isAssigning = false;
  async function handleConfirmAssign() {
    if (!selectedReport || isAssigning) return;
    const btn = document.getElementById('btnConfirmAssign');
    const selectTeam = document.getElementById('selectAssignTeam');
    const selectPriority = document.getElementById('selectAssignPriority');
    const inputInstructions = document.getElementById('assignInstructions');

    const teamIdVal = selectTeam ? selectTeam.value : null;
    const teamNameVal = selectTeam && selectTeam.selectedIndex >= 0 ? selectTeam.options[selectTeam.selectedIndex].text : 'Rescue Team';
    const priority = selectPriority ? selectPriority.value : 'HIGH';
    const instructions = inputInstructions ? inputInstructions.value.trim() : '';

    if (!teamIdVal) {
      if (window.LW?.ui?.toast) window.LW.ui.toast('Please select a Rescue Team for assignment.', 'warning');
      return;
    }

    isAssigning = true;
    const origHtml = btn ? btn.innerHTML : 'Confirm Assignment';
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Assigning Team...';
    }

    try {
      // 1. Authoritative assignment via Nearest Rescue Team Management endpoint
      if (window.LW?.api && teamIdVal && !isNaN(Number(teamIdVal))) {
        await window.LW.api.post('/api/admin/rescue-teams/assign', {
          reportId: selectedReport.id,
          teamId: Number(teamIdVal),
          priority: priority,
          instructions: instructions
        });
      }

      // 2. Also keep legacy mission state synchronized
      if (window.LW?.api) {
        await window.LW.api.post(`/api/rescue/missions/${selectedReport.id}/assign`, {
          teamId: String(teamIdVal),
          teamName: teamNameVal,
          priority,
          instructions,
          responseWindow: 'STANDARD'
        }).catch(() => null);
      }

      closeModal('assignModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(`Incident #SG-${selectedReport.id} successfully assigned to ${teamNameVal}.`, 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to assign rescue mission', 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = origHtml;
      }
      isAssigning = false;
    }
  }

  // 12. Nearest Rescue Teams Calculation & Modal
  async function openNearestTeamsModal(reportId) {
    const report = allReports.find(r => String(r.id) === String(reportId));
    if (!report) return;
    selectedReport = report;

    const modal = document.getElementById('nearestTeamsModal');
    const title = document.getElementById('nearestTeamsTitle');
    const banner = document.getElementById('nearestIncidentBanner');
    const countBadge = document.getElementById('nearestTeamsCountBadge');
    const list = document.getElementById('nearestTeamsList');

    if (!modal || !list) return;

    if (title) title.textContent = `Nearest Rescue Teams for Incident #SG-${report.id}`;

    const lat = report.latitude != null ? report.latitude.toFixed(5) : 'N/A';
    const lng = report.longitude != null ? report.longitude.toFixed(5) : 'N/A';

    if (banner) {
      banner.innerHTML = `
        <div>
          <div style="font-weight:700;color:var(--admin-forest);">Incident #SG-${report.id} — ${escapeHtml(formatCategory(report.incidentCategory))}</div>
          <div style="font-size:0.8125rem;color:var(--admin-text-secondary);margin-top:2px;">Location: <strong>${escapeHtml(report.locationDescription || 'Sector')}</strong></div>
        </div>
        <div style="text-align:right;">
          <div style="font-size:0.6875rem;color:var(--admin-text-muted);text-transform:uppercase;font-weight:700;">Incident GPS Coordinates</div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:0.875rem;font-weight:700;color:var(--admin-forest);">${lat}, ${lng}</div>
        </div>
      `;
    }

    list.innerHTML = `<div style="text-align:center;padding:32px;color:var(--admin-text-muted);">Computing distance to registered rescue teams using Haversine algorithm...</div>`;
    openModal('nearestTeamsModal');

    try {
      let teams = [];
      if (window.LW?.api) {
        teams = await window.LW.api.get(`/api/admin/rescue-teams/nearest?reportId=${report.id}`);
      }

      if (!Array.isArray(teams) || teams.length === 0) {
        list.innerHTML = `
          <div class="admin-empty-state" style="display:block;padding:24px;">
            <h3 class="admin-empty-title">No rescue teams found</h3>
            <p style="font-size:0.8125rem;margin:0;">No registered rescue teams are available in the system.</p>
          </div>
        `;
        if (countBadge) countBadge.textContent = '0 Teams';
        return;
      }

      if (countBadge) countBadge.textContent = `${teams.length} ${teams.length === 1 ? 'Team' : 'Teams'} Ranked`;

      list.innerHTML = teams.map((team, idx) => {
        const isEligible = team.eligible !== false;
        const rankClass = idx === 0 && isEligible ? 'nearest-rank-1' : '';
        const distanceText = team.distanceKm != null ? `${team.distanceKm.toFixed(1)} km` : 'GPS N/A';
        const cardClass = isEligible ? 'nearest-team-card' : 'nearest-team-card ineligible';
        const teamIdVal = team.teamId || team.id;
        const orgType = team.teamCategory || team.organizationType || 'Rescue';
        const address = team.baseAddress || team.baseLocationAddress || 'Regional Base';
        const rawLat = team.latitude != null ? team.latitude : team.baseLatitude;
        const rawLng = team.longitude != null ? team.longitude : team.baseLongitude;

        const caps = Array.isArray(team.capabilities) && team.capabilities.length > 0
          ? team.capabilities.map(c => `<span class="badge-cap-tag">${escapeHtml(c.replace(/_/g, ' '))}</span>`).join('')
          : '';

        let eligibilityReason = team.ineligibilityReason || '';
        if (!isEligible && !eligibilityReason) {
          const avail = team.availability || team.availabilityStatus;
          if (team.verificationStatus && team.verificationStatus !== 'APPROVED') eligibilityReason = 'Pending Verification';
          else if (team.operationalStatus && team.operationalStatus !== 'ACTIVE') eligibilityReason = 'Inactive';
          else if (avail && avail !== 'AVAILABLE') eligibilityReason = avail;
          else eligibilityReason = 'Unavailable';
        }

        const assignBtn = isEligible
          ? `<button type="button" class="btn-primary-admin" data-action="select-nearest-team" data-team-id="${teamIdVal}" data-team-name="${escapeHtml(team.teamName)}" data-distance="${team.distanceKm || ''}">
               Assign Team
             </button>`
          : `<span class="badge-status badge-team-inactive" style="font-size:0.75rem;">Ineligible (${escapeHtml(eligibilityReason)})</span>`;

        return `
          <div class="${cardClass}">
            <div style="flex:1;">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
                <span class="distance-badge ${rankClass}">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="3 11 22 2 13 21 11 13 3 11"></polygon></svg>
                  ${distanceText}
                </span>
                <strong style="color:var(--admin-forest);font-size:0.9375rem;">${escapeHtml(team.teamName)}</strong>
                <span style="font-size:0.75rem;color:var(--admin-text-muted);">(${escapeHtml(orgType)})</span>
              </div>

              <div style="font-size:0.8125rem;color:var(--admin-text-secondary);margin-bottom:6px;">
                Base: ${escapeHtml(address)}
                ${rawLat != null ? ` • <span style="font-family:'JetBrains Mono',monospace;font-size:0.75rem;">(${rawLat.toFixed(3)}, ${rawLng.toFixed(3)})</span>` : ''}
              </div>

              <div style="display:flex;align-items:center;flex-wrap:wrap;gap:6px;">
                <span style="font-size:0.6875rem;font-weight:700;color:var(--admin-text-muted);text-transform:uppercase;">Capabilities:</span>
                ${caps}
                <span style="font-size:0.6875rem;color:var(--admin-text-muted);margin-left:auto;">
                  Active Missions: <strong>${team.activeMissionsCount || team.activeAssignmentsCount || 0}</strong>
                </span>
              </div>
            </div>

            <div style="display:flex;flex-direction:column;align-items:flex-end;justify-content:center;min-width:130px;">
              ${assignBtn}
            </div>
          </div>
        `;
      }).join('');

    } catch (err) {
      list.innerHTML = `<div style="color:#DC2626;padding:16px;text-align:center;">Failed to compute nearest teams: ${escapeHtml(err.message)}</div>`;
    }
  }

  // 13. Rescue Teams Management Table & Details Modals
  function renderTeamsTable() {
    const tbody = document.getElementById('tbodyRescueTeams');
    const emptyState = document.getElementById('emptyTeamsState');
    if (!tbody) return;

    let filtered = [...allTeams];

    if (teamSearchQuery) {
      const q = teamSearchQuery.toLowerCase();
      filtered = filtered.filter(t => 
        (t.teamName && t.teamName.toLowerCase().includes(q)) ||
        (t.organizationType && t.organizationType.toLowerCase().includes(q)) ||
        (t.baseLocationAddress && t.baseLocationAddress.toLowerCase().includes(q)) ||
        (t.teamLeaderName && t.teamLeaderName.toLowerCase().includes(q))
      );
    }

    if (teamFilterStatus !== 'all') {
      const fs = teamFilterStatus.toUpperCase();
      if (fs === 'PENDING' || fs === 'APPROVED' || fs === 'REJECTED') {
        filtered = filtered.filter(t => (t.verificationStatus || '').toUpperCase() === fs);
      } else if (fs === 'ACTIVE' || fs === 'INACTIVE') {
        filtered = filtered.filter(t => (t.operationalStatus || '').toUpperCase() === fs);
      }
    }

    if (filtered.length === 0) {
      tbody.innerHTML = '';
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = filtered.map(team => {
      const vStatus = (team.verificationStatus || 'PENDING').toUpperCase();
      const oStatus = (team.operationalStatus || 'INACTIVE').toUpperCase();
      const aStatus = (team.availability || team.availabilityStatus || 'AVAILABLE').toUpperCase();
      const teamId = team.teamId || team.id;
      const orgType = team.teamCategory || team.organizationType || 'Disaster Response';
      const address = team.baseAddress || team.baseLocationAddress || 'Regional Base';
      const rawLat = team.latitude != null ? team.latitude : team.baseLatitude;
      const rawLng = team.longitude != null ? team.longitude : team.baseLongitude;

      let vBadge = `<span class="badge-status badge-team-pending">Pending Review</span>`;
      if (vStatus === 'APPROVED') vBadge = `<span class="badge-status badge-team-approved">Approved</span>`;
      else if (vStatus === 'REJECTED') vBadge = `<span class="badge-status badge-team-rejected">Rejected</span>`;

      let aBadge = `<span class="badge-status badge-team-active">${aStatus}</span>`;
      if (oStatus === 'INACTIVE' || aStatus === 'INACTIVE') {
        aBadge = `<span class="badge-status badge-team-inactive">Inactive</span>`;
      } else if (aStatus === 'OFF_DUTY') {
        aBadge = `<span class="badge-status" style="background:#FEF3C7;color:#92400E;">Off-Duty</span>`;
      } else if (aStatus === 'BUSY') {
        aBadge = `<span class="badge-status" style="background:#FEE2E2;color:#991B1B;">Busy</span>`;
      }

      const caps = Array.isArray(team.capabilities) && team.capabilities.length > 0
        ? team.capabilities.map(c => `<span class="badge-cap-tag">${escapeHtml(c.replace(/_/g, ' '))}</span>`).join('')
        : '<span style="font-size:0.75rem;color:var(--admin-text-muted);">Standard SAR</span>';

      const lat = rawLat != null ? rawLat.toFixed(4) : '—';
      const lng = rawLng != null ? rawLng.toFixed(4) : '—';

      const quickApproveBtn = vStatus === 'PENDING'
        ? `<button type="button" class="btn-success" data-action="quick-approve-team" data-id="${teamId}" style="padding:4px 8px;font-size:0.75rem;margin-right:4px;">Approve</button>`
        : '';

      return `
        <tr>
          <td>
            <div style="font-weight:700;color:var(--admin-forest);">${escapeHtml(team.teamName)}</div>
            <div style="font-size:0.75rem;color:var(--admin-text-muted);">${escapeHtml(orgType)}</div>
          </td>
          <td>
            <div style="font-size:0.8125rem;">${escapeHtml(address)}</div>
          </td>
          <td>
            <span style="font-family:'JetBrains Mono',monospace;font-size:0.75rem;">${lat}, ${lng}</span>
          </td>
          <td style="max-width:200px;">
            <div style="display:flex;flex-wrap:wrap;gap:2px;">${caps}</div>
          </td>
          <td>${vBadge}</td>
          <td>${aBadge}</td>
          <td style="text-align:right;white-space:nowrap;">
            ${quickApproveBtn}
            <button type="button" class="btn-compact" data-action="view-team" data-id="${teamId}">View Details</button>
          </td>
        </tr>
      `;
    }).join('');
  }

  function openTeamDetails(teamId) {
    const team = allTeams.find(t => String(t.teamId || t.id) === String(teamId));
    if (!team) return;
    selectedTeam = team;

    const modal = document.getElementById('teamDetailsModal');
    const title = document.getElementById('teamDetailsTitle');
    const body = document.getElementById('teamDetailsBody');
    const footer = document.getElementById('teamDetailsFooter');
    if (!modal || !body || !footer) return;

    if (title) title.textContent = `${team.teamName} — Profile & Verification`;

    const vStatus = (team.verificationStatus || 'PENDING').toUpperCase();
    const oStatus = (team.operationalStatus || 'INACTIVE').toUpperCase();
    const aStatus = (team.availability || team.availabilityStatus || 'AVAILABLE').toUpperCase();
    const idVal = team.teamId || team.id;
    const orgType = team.teamCategory || team.organizationType || 'Disaster Response';
    const leader = team.teamLeader || team.teamLeaderName || 'Command Officer';
    const address = team.baseAddress || team.baseLocationAddress || '—';

    const capsHtml = Array.isArray(team.capabilities) && team.capabilities.length > 0
      ? team.capabilities.map(c => `<span class="badge-cap-tag" style="font-size:0.75rem;">${escapeHtml(c.replace(/_/g, ' '))}</span>`).join(' ')
      : '<span>Standard SAR</span>';

    const rawLat = team.latitude != null ? team.latitude : team.baseLatitude;
    const rawLng = team.longitude != null ? team.longitude : team.baseLongitude;
    const lat = rawLat != null ? rawLat.toFixed(6) : 'N/A';
    const lng = rawLng != null ? rawLng.toFixed(6) : 'N/A';

    let verificationNotesHtml = '';
    if (team.verificationNotes || team.rejectionReason) {
      verificationNotesHtml = `
        <div class="admin-form-group">
          <span class="admin-label">Verification Review History</span>
          <div class="modal-box" style="background:#FAF8F2;">
            ${escapeHtml(team.verificationNotes || team.rejectionReason)}
          </div>
        </div>
      `;
    }

    body.innerHTML = `
      <div class="modal-grid-2">
        <div class="modal-field">
          <span class="modal-field-label">Organization Type</span>
          <span class="modal-field-value">${escapeHtml(orgType)}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Team Leader</span>
          <span class="modal-field-value">${escapeHtml(leader)}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Official Contact Phone</span>
          <span class="modal-field-value">${escapeHtml(team.contactNumber || '—')}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Login Account Email</span>
          <span class="modal-field-value">${escapeHtml(team.email || '—')}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Base Location Address</span>
          <span class="modal-field-value">${escapeHtml(address)}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">GPS Base Station</span>
          <span class="modal-field-value" style="font-family:'JetBrains Mono',monospace;">${lat}, ${lng}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Verification Status</span>
          <div style="margin-top:2px;">
            ${vStatus === 'APPROVED' ? '<span class="badge-status badge-team-approved">Approved</span>' : vStatus === 'REJECTED' ? '<span class="badge-status badge-team-rejected">Rejected</span>' : '<span class="badge-status badge-team-pending">Pending Review</span>'}
          </div>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Operational Availability</span>
          <div style="margin-top:2px;">
            <span class="badge-status badge-team-active">${aStatus}</span> (${oStatus})
          </div>
        </div>
      </div>

      <div class="admin-form-group">
        <span class="admin-label">Declared Rescue Capabilities</span>
        <div style="display:flex;flex-wrap:wrap;gap:4px;margin-top:4px;">
          ${capsHtml}
        </div>
      </div>

      ${verificationNotesHtml}
    `;

    // Footer buttons depending on status
    let actionButtons = '';
    if (vStatus === 'PENDING') {
      actionButtons = `
        <button type="button" class="btn-danger" id="btnRejectTeam">Reject Registration</button>
        <button type="button" class="btn-success" id="btnApproveTeam">Approve Team Registration</button>
      `;
    } else if (vStatus === 'APPROVED') {
      if (oStatus === 'ACTIVE') {
        actionButtons = `
          <button type="button" class="btn-danger" id="btnDeactivateTeam">Deactivate Team</button>
        `;
      } else {
        actionButtons = `
          <button type="button" class="btn-success" id="btnActivateTeam">Activate Team</button>
        `;
      }
    }

    footer.innerHTML = `
      ${actionButtons}
      <button type="button" class="btn-compact" data-close-modal="teamDetailsModal">Close</button>
    `;

    // Attach handlers
    const btnApprove = document.getElementById('btnApproveTeam');
    if (btnApprove) btnApprove.onclick = () => handleApproveTeam(idVal);

    const btnReject = document.getElementById('btnRejectTeam');
    if (btnReject) btnReject.onclick = () => handleRejectTeamPrompt(idVal);

    const btnDeactivate = document.getElementById('btnDeactivateTeam');
    if (btnDeactivate) btnDeactivate.onclick = () => handleToggleTeamStatus(idVal, 'INACTIVE');

    const btnActivate = document.getElementById('btnActivateTeam');
    if (btnActivate) btnActivate.onclick = () => handleToggleTeamStatus(idVal, 'ACTIVE');

    openModal('teamDetailsModal');
  }

  async function handleApproveTeam(teamId) {
    const team = allTeams.find(t => String(t.id || t.teamId) === String(teamId));
    const teamName = team ? (team.teamName || team.name) : `Team #${teamId}`;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Approve Rescue Team',
      message: `Are you sure you want to approve registration for '${teamName}'? This will activate the team and enable incident dispatching.`,
      confirmText: 'Approve Team',
      variant: 'success'
    });
    if (!confirmed) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.post(`/api/admin/rescue-teams/${teamId}/approve`, {
          verificationNotes: 'Verified and approved by DMO Administrator'
        });
      }
      closeModal('teamDetailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(`Rescue team '${teamName}' approved and operational!`, 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to approve team', 'error');
    }
  }

  async function handleRejectTeamPrompt(teamId) {
    const team = allTeams.find(t => String(t.id || t.teamId) === String(teamId));
    const teamName = team ? (team.teamName || team.name) : `Team #${teamId}`;

    const reason = await window.LW.ui.promptAction({
      title: 'Reject Team Registration',
      message: `Please specify the mandatory reason for rejecting registration for '${teamName}'. This will be recorded in the audit log.`,
      placeholder: 'Enter mandatory rejection reason...',
      confirmText: 'Reject Registration',
      variant: 'danger',
      required: true
    });
    if (reason === null) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.post(`/api/admin/rescue-teams/${teamId}/reject`, {
          reason: reason.trim()
        });
      }
      closeModal('teamDetailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(`Rescue team '${teamName}' registration rejected.`, 'info');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to reject team', 'error');
    }
  }

  async function handleToggleTeamStatus(teamId, newStatus) {
    const team = allTeams.find(t => String(t.id || t.teamId) === String(teamId));
    const teamName = team ? (team.teamName || team.name) : `Team #${teamId}`;
    const isActivating = newStatus === 'ACTIVE';

    const confirmed = await window.LW.ui.confirmAction({
      title: isActivating ? 'Activate Rescue Team' : 'Deactivate Rescue Team',
      message: isActivating
        ? `Activate '${teamName}' to resume mission dispatch availability?`
        : `Deactivate '${teamName}'? The team will not receive new assignments while inactive.`,
      confirmText: isActivating ? 'Activate Team' : 'Deactivate Team',
      variant: isActivating ? 'success' : 'warning'
    });
    if (!confirmed) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.post(`/api/admin/rescue-teams/${teamId}/status`, {
          operationalStatus: newStatus
        });
      }
      closeModal('teamDetailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(`Team '${teamName}' status updated to ${newStatus}.`, 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to update team status', 'error');
    }
  }

  async function handleConfirmClosure(missionId) {
    const confirmed = await window.LW.ui.confirmAction({
      title: 'Confirm Incident Resolution',
      message: `Verify rescue outcome and mark incident #SG-${missionId} as RESOLVED? This officially closes the incident.`,
      confirmText: 'Verify & Close Incident',
      variant: 'forest'
    });
    if (!confirmed) return;

    try {
      if (window.LW?.api) {
        await window.LW.api.patch(`/api/admin/reports/${missionId}/status`, {
          status: 'RESOLVED',
          reviewNotes: 'Rescue mission outcome verified and approved by DMO administration. Incident closed.'
        });
      }

      if (window.LW?.ui?.toast) window.LW.ui.toast(`Incident #SG-${missionId} successfully verified and closed.`, 'success');
      await loadData();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to close incident', 'error');
    }
  }

  // 12. View Mission Details & Response Notes
  function openMissionDetails(missionId) {
    const mission = allMissions.find(m => String(m.id) === String(missionId));
    if (!mission) return;

    const modal = document.getElementById('missionDetailsModal');
    const title = document.getElementById('missionDetailsTitle');
    const body = document.getElementById('missionDetailsBody');

    if (!modal || !body) return;

    if (title) title.textContent = `Mission #${mission.id} Operational Status & Notes`;

    const stage = (mission.operationalStage || 'ASSIGNED').toUpperCase();
    const notesHistory = mission.progressHistory || [];

    let historyHtml = '<div style="font-size:0.8125rem;color:var(--admin-text-muted);">No field notes submitted yet.</div>';
    if (notesHistory.length > 0) {
      historyHtml = `
        <div style="display:flex;flex-direction:column;gap:8px;">
          ${notesHistory.map(h => `
            <div style="background:#FAF8F2;padding:10px;border-left:3px solid var(--admin-sage);border-radius:4px;font-size:0.8125rem;">
              <strong>${escapeHtml(h.siteAssessment || 'Field Assessment')}</strong>
              ${h.roadPassability ? `<div style="color:var(--admin-text-secondary);margin-top:2px;">Road Passability: <strong>${escapeHtml(h.roadPassability)}</strong></div>` : ''}
              ${h.actionsTaken ? `<div style="color:var(--admin-text-secondary);margin-top:2px;">Actions: ${escapeHtml(h.actionsTaken)}</div>` : ''}
              <div style="font-size:0.6875rem;color:var(--admin-text-muted);margin-top:4px;">Recorded by ${escapeHtml(h.actor || 'Rescue Team')} • ${formatDate(h.timestamp)}</div>
            </div>
          `).join('')}
        </div>
      `;
    }

    let outcomeHtml = '';
    if (mission.outcomeReport) {
      outcomeHtml = `
        <div class="admin-form-group">
          <span class="admin-label">Final Outcome Report</span>
          <div class="modal-box" style="background:#ECFDF5;border-color:#A7F3D0;">
            <strong>Resolution Summary:</strong><br>${escapeHtml(mission.outcomeReport.resolutionSummary || 'Completed')}
            ${mission.outcomeReport.actionsPerformed ? `<div style="margin-top:4px;"><strong>Operations:</strong> ${escapeHtml(mission.outcomeReport.actionsPerformed)}</div>` : ''}
          </div>
        </div>
      `;
    }

    body.innerHTML = `
      <div class="modal-grid-2">
        <div class="modal-field">
          <span class="modal-field-label">Assigned Unit</span>
          <span class="modal-field-value">${escapeHtml(mission.assignedTeam || 'Rescue Force Alpha')}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Operational Stage</span>
          <span class="modal-field-value">${stage}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Location</span>
          <span class="modal-field-value">${escapeHtml(mission.locationDescription || 'Regional Sector')}</span>
        </div>
        <div class="modal-field">
          <span class="modal-field-label">Assignment Date</span>
          <span class="modal-field-value">${formatDate(mission.assignedAt)}</span>
        </div>
      </div>

      <div class="admin-form-group">
        <span class="admin-label">Field Response Notes History</span>
        ${historyHtml}
      </div>

      ${outcomeHtml}
    `;

    openModal('missionDetailsModal');
  }

  // Modal helpers
  function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
  }

  function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
  }

  // 13. Event Listeners Setup
  function initEventListeners() {
    // Navigation Tabs
    const tabContainer = document.getElementById('adminNavTabs');
    if (tabContainer) {
      tabContainer.addEventListener('click', (e) => {
        const btn = e.target.closest('.admin-tab-btn');
        if (!btn) return;
        tabContainer.querySelectorAll('.admin-tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        activeTab = btn.dataset.tab || 'overview';
        renderActiveView();
      });
    }

    // Incidents Search & Filter
    const incSearch = document.getElementById('inputIncidentSearch');
    if (incSearch) {
      incSearch.addEventListener('input', (e) => {
        incidentSearchQuery = e.target.value.trim();
        renderIncidentsTable();
      });
    }

    const incFilter = document.getElementById('selectIncidentFilter');
    if (incFilter) {
      incFilter.addEventListener('change', (e) => {
        incidentFilterStatus = e.target.value;
        renderIncidentsTable();
      });
    }

    // Alerts Search & Filter
    const alertSearch = document.getElementById('inputAlertSearch');
    if (alertSearch) {
      alertSearch.addEventListener('input', (e) => {
        alertSearchQuery = e.target.value.trim();
        renderAlertsTable();
      });
    }

    const alertFilter = document.getElementById('selectAlertFilter');
    if (alertFilter) {
      alertFilter.addEventListener('change', (e) => {
        alertFilterStatus = e.target.value;
        renderAlertsTable();
      });
    }

    const btnCreateAlert = document.getElementById('btnCreateAlert');
    if (btnCreateAlert) {
      btnCreateAlert.addEventListener('click', () => openCreateAlertModal());
    }

    // Rescue Search & Filter
    const rescueSearch = document.getElementById('inputRescueSearch');
    if (rescueSearch) {
      rescueSearch.addEventListener('input', (e) => {
        rescueSearchQuery = e.target.value.trim();
        renderRescueTable();
      });
    }

    const rescueFilter = document.getElementById('selectRescueFilter');
    if (rescueFilter) {
      rescueFilter.addEventListener('change', (e) => {
        rescueFilterStatus = e.target.value;
        renderRescueTable();
      });
    }

    // Rescue Teams Search & Filter
    const teamSearch = document.getElementById('inputTeamSearch');
    if (teamSearch) {
      teamSearch.addEventListener('input', (e) => {
        teamSearchQuery = e.target.value.trim();
        renderTeamsTable();
      });
    }

    const teamFilter = document.getElementById('selectTeamFilter');
    if (teamFilter) {
      teamFilter.addEventListener('change', (e) => {
        teamFilterStatus = e.target.value;
        renderTeamsTable();
      });
    }

    const btnRefreshTeams = document.getElementById('btnRefreshTeams');
    if (btnRefreshTeams) {
      btnRefreshTeams.addEventListener('click', async () => {
        await loadTeams();
        renderSummaryCards();
        renderTeamsTable();
        if (window.LW?.ui?.toast) window.LW.ui.toast('Rescue teams refreshed.', 'info');
      });
    }

    // Table Clicks Delegation
    document.addEventListener('click', (e) => {
      // View Incident Details
      const viewReportBtn = e.target.closest('[data-action="view-report"]');
      if (viewReportBtn) {
        const id = viewReportBtn.getAttribute('data-id');
        openIncidentDetails(id);
        return;
      }

      // Quick Assign to Rescue
      const assignBtn = e.target.closest('[data-action="assign-rescue"]');
      if (assignBtn) {
        const id = assignBtn.getAttribute('data-id');
        openAssignModal(id);
        return;
      }

      // View Team Details
      const viewTeamBtn = e.target.closest('[data-action="view-team"]');
      if (viewTeamBtn) {
        const id = viewTeamBtn.getAttribute('data-id');
        openTeamDetails(id);
        return;
      }

      // Quick Approve Team
      const quickApproveBtn = e.target.closest('[data-action="quick-approve-team"]');
      if (quickApproveBtn) {
        const id = quickApproveBtn.getAttribute('data-id');
        handleApproveTeam(id);
        return;
      }

      // Select Nearest Team from modal
      const selectNearestBtn = e.target.closest('[data-action="select-nearest-team"]');
      if (selectNearestBtn) {
        const teamId = selectNearestBtn.getAttribute('data-team-id');
        const teamName = selectNearestBtn.getAttribute('data-team-name');
        const distance = selectNearestBtn.getAttribute('data-distance');
        closeModal('nearestTeamsModal');
        openAssignModal(selectedReport.id, teamId, distance);
        return;
      }

      // Close Incident
      const closeBtn = e.target.closest('[data-action="close-incident"]');
      if (closeBtn) {
        const id = closeBtn.getAttribute('data-id');
        handleConfirmClosure(id);
        return;
      }

      // View Mission Notes
      const viewMissionBtn = e.target.closest('[data-action="view-mission"]');
      if (viewMissionBtn) {
        const id = viewMissionBtn.getAttribute('data-id');
        openMissionDetails(id);
        return;
      }

      // Alert actions
      const publishAlertBtn = e.target.closest('[data-action="publish-alert"]');
      if (publishAlertBtn) {
        const id = publishAlertBtn.getAttribute('data-id');
        handlePublishAlert(id);
        return;
      }

      const resolveAlertBtn = e.target.closest('[data-action="resolve-alert"]');
      if (resolveAlertBtn) {
        const id = resolveAlertBtn.getAttribute('data-id');
        handleResolveAlert(id);
        return;
      }

      const withdrawAlertBtn = e.target.closest('[data-action="withdraw-alert"]');
      if (withdrawAlertBtn) {
        const id = withdrawAlertBtn.getAttribute('data-id');
        handleWithdrawAlert(id);
        return;
      }

      const editAlertBtn = e.target.closest('[data-action="edit-alert"]');
      if (editAlertBtn) {
        const id = editAlertBtn.getAttribute('data-id');
        const alertObj = allAlerts.find(a => String(a.id) === String(id));
        openCreateAlertModal(alertObj);
        return;
      }

      // Close modals
      const closeTarget = e.target.closest('[data-close-modal]');
      if (closeTarget) {
        const modalId = closeTarget.getAttribute('data-close-modal');
        closeModal(modalId);
      }
    });

    // Close on backdrop click
    document.querySelectorAll('.admin-modal-backdrop').forEach(backdrop => {
      backdrop.addEventListener('click', (e) => {
        if (e.target === backdrop) backdrop.classList.remove('active');
      });
    });

    // Modal action buttons
    const btnSaveAlertDraft = document.getElementById('btnSaveAlertDraft');
    if (btnSaveAlertDraft) btnSaveAlertDraft.onclick = () => handleSaveAlert(false);

    const btnPublishAlertNow = document.getElementById('btnPublishAlertNow');
    if (btnPublishAlertNow) btnPublishAlertNow.onclick = () => handleSaveAlert(true);

    const btnConfirmAssign = document.getElementById('btnConfirmAssign');
    if (btnConfirmAssign) btnConfirmAssign.onclick = handleConfirmAssign;
  }

  // Initialization
  async function init() {
    if (!initAuthGuard()) return;

    // Check hash for direct tab navigation
    if (window.location.hash) {
      const h = window.location.hash.replace('#', '').toLowerCase();
      if (h === 'alerts') activeTab = 'alerts';
      else if (h === 'incidents') activeTab = 'incidents';
      else if (h === 'rescue') activeTab = 'rescue';
      else if (h === 'teams') activeTab = 'teams';

      const tabContainer = document.getElementById('adminNavTabs');
      const targetBtn = tabContainer?.querySelector(`[data-tab="${activeTab}"]`);
      if (tabContainer && targetBtn) {
        tabContainer.querySelectorAll('.admin-tab-btn').forEach(b => b.classList.remove('active'));
        targetBtn.classList.add('active');
      }
    }

    initEventListeners();
    await loadData();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
