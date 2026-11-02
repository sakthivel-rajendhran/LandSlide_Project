/**
 * SlopeGuard — Simplified Rescue Team Dashboard Page Controller (rescue-dashboard.js)
 * Clean, compact, professional console for authorized rescue teams.
 */

(function () {
  'use strict';

  // State Management
  let currentUser = null;
  let teamProfile = null;
  let allMissions = [];
  let activeFilter = 'all';
  let searchQuery = '';
  let selectedMission = null;

  // 1. Guard & Authentication
  function initAuthGuard() {
    try {
      const raw = sessionStorage.getItem('sg_user') || localStorage.getItem('sg_user');
      currentUser = raw ? JSON.parse(raw) : null;
    } catch (e) {
      currentUser = null;
    }

    if (!currentUser) {
      window.location.replace('login.html?next=rescue-dashboard.html');
      return false;
    }

    const role = (currentUser.role || '').toUpperCase();
    if (role !== 'OFFICER' && role !== 'RESCUE_FORCE' && role !== 'ADMIN') {
      window.location.replace('dashboard.html');
      return false;
    }

    // Populate topbar profile
    const nameEl = document.getElementById('officerName');
    const avatarEl = document.getElementById('officerAvatar');
    const officerName = currentUser.name || currentUser.fullName || 'Rescue Force Unit';
    if (nameEl) nameEl.textContent = officerName;
    if (avatarEl) avatarEl.textContent = officerName.charAt(0).toUpperCase();

    // Sign out button
    const signOutBtn = document.getElementById('btnSignOut');
    if (signOutBtn) {
      signOutBtn.addEventListener('click', () => {
        sessionStorage.removeItem('sg_user');
        sessionStorage.removeItem('sg_token');
        localStorage.removeItem('sg_user');
        localStorage.removeItem('sg_token');
        window.location.replace('login.html');
      });
    }

    return true;
  }

  // 2. Map backend mission state into 5 Simple Mission Statuses
  function getMissionSimpleStatus(m) {
    if (!m) return 'NEW ASSIGNMENT';
    const decision = (m.decisionStatus || '').toUpperCase();
    const stage = (m.operationalStage || '').toUpperCase();
    const status = (m.status || '').toUpperCase();

    if (decision === 'DECLINED') {
      return 'DECLINED';
    }
    if (stage === 'AWAITING_VERIFICATION' || stage === 'COMPLETED' || stage === 'VERIFIED_RESOLVED' || status === 'RESOLVED') {
      return 'COMPLETED';
    }
    if (stage === 'ON_SCENE' || stage === 'IN_PROGRESS') {
      return 'ON SCENE';
    }
    if (stage === 'EN_ROUTE') {
      return 'EN ROUTE';
    }
    if (stage === 'ACCEPTED' || decision === 'ACCEPTED') {
      return 'ACCEPTED';
    }
    // Default initial assignment
    return 'NEW ASSIGNMENT';
  }

  // Escape HTML helper
  function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // Format Incident Category display
  function formatIncidentCategory(cat) {
    if (!cat) return 'Incident';
    const clean = String(cat).replace(/_/g, ' ').toLowerCase();
    return clean.charAt(0).toUpperCase() + clean.slice(1);
  }

  // Format date display
  function formatAssignmentDate(dtStr) {
    if (!dtStr) return '—';
    try {
      const d = new Date(dtStr);
      if (isNaN(d.getTime())) return dtStr;
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    } catch (e) {
      return dtStr;
    }
  }

  // 3. Load Missions & Team Profile from Backend
  async function loadTeamProfile() {
    try {
      if (window.LW?.api) {
        teamProfile = await window.LW.api.get('/api/rescue/profile').catch(() => null);
      }
    } catch (e) {
      console.warn('[rescue-dashboard] Could not load team profile:', e);
    }
    updateProfileUI();
  }

  function updateProfileUI() {
    const banner = document.getElementById('rescueNoticeBanner');
    const bTitle = document.getElementById('rescueNoticeTitle');
    const bDesc = document.getElementById('rescueNoticeDesc');

    const availDot = document.getElementById('availDot');
    const availLabel = document.getElementById('availLabel');

    if (teamProfile) {
      // 1. Update status dot & label
      const avail = (teamProfile.availabilityStatus || 'AVAILABLE').toUpperCase();
      const op = (teamProfile.operationalStatus || 'ACTIVE').toUpperCase();

      if (availDot) {
        availDot.className = 'avail-dot';
        if (op === 'INACTIVE' || avail === 'INACTIVE') availDot.classList.add('inactive');
        else if (avail === 'OFF_DUTY') availDot.classList.add('off-duty');
        else if (avail === 'BUSY') availDot.classList.add('busy');
      }

      if (availLabel) {
        if (op === 'INACTIVE' || avail === 'INACTIVE') availLabel.textContent = 'Inactive';
        else if (avail === 'OFF_DUTY') availLabel.textContent = 'Off-Duty';
        else if (avail === 'BUSY') availLabel.textContent = 'Busy';
        else availLabel.textContent = 'Available';
      }

      // 2. Update notice banner
      const vStatus = (teamProfile.verificationStatus || 'APPROVED').toUpperCase();
      if (vStatus === 'PENDING') {
        if (banner) {
          banner.style.display = 'flex';
          banner.className = 'rescue-notice-banner';
          if (bTitle) bTitle.textContent = 'Registration Pending DMO Verification';
          if (bDesc) bDesc.textContent = 'Your Rescue Team registration has been recorded with status PENDING and is currently awaiting verification and authorization by the District Disaster Management Officer (DMO). You will be able to receive and respond to mission assignments once approved.';
        }
      } else if (vStatus === 'REJECTED') {
        if (banner) {
          banner.style.display = 'flex';
          banner.className = 'rescue-notice-banner danger';
          if (bTitle) bTitle.textContent = 'Registration Rejected by DMO';
          if (bDesc) bDesc.textContent = `Your registration request was reviewed and rejected. Reason: ${teamProfile.verificationNotes || 'Did not meet operational criteria.'}`;
        }
      } else {
        if (banner) banner.style.display = 'none';
      }
    }
  }

  function openProfileModal() {
    if (!teamProfile) return;
    const body = document.getElementById('profileModalBody');
    if (!body) return;

    const rawLat = teamProfile.latitude != null ? teamProfile.latitude : teamProfile.baseLatitude;
    const rawLng = teamProfile.longitude != null ? teamProfile.longitude : teamProfile.baseLongitude;
    const lat = rawLat != null ? rawLat.toFixed(5) : '—';
    const lng = rawLng != null ? rawLng.toFixed(5) : '—';
    const org = teamProfile.teamCategory || teamProfile.organizationType || 'Disaster Response';
    const leader = teamProfile.teamLeader || teamProfile.teamLeaderName || '—';
    const address = teamProfile.baseAddress || teamProfile.baseLocationAddress || '—';
    const currentAvail = (teamProfile.availability || teamProfile.availabilityStatus || 'AVAILABLE').toUpperCase();

    const caps = Array.isArray(teamProfile.capabilities) && teamProfile.capabilities.length > 0
      ? teamProfile.capabilities.map(c => `<span style="background:rgba(107,127,91,0.15);color:#2E3A2F;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;display:inline-block;margin:2px;">${escapeHtml(c.replace(/_/g, ' '))}</span>`).join('')
      : '<span>Standard SAR</span>';

    body.innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:14px;font-size:0.8125rem;">
        <div>
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Team Name</span>
          <strong>${escapeHtml(teamProfile.teamName)}</strong>
        </div>
        <div>
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Organization</span>
          <strong>${escapeHtml(org)}</strong>
        </div>
        <div>
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Team Leader</span>
          <span>${escapeHtml(leader)}</span>
        </div>
        <div>
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Contact Phone</span>
          <span>${escapeHtml(teamProfile.contactNumber || '—')}</span>
        </div>
        <div style="grid-column: span 2;">
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Base Address & GPS</span>
          <span>${escapeHtml(address)} • <code style="font-family:'JetBrains Mono',monospace;">${lat}, ${lng}</code></span>
        </div>
        <div style="grid-column: span 2;">
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Declared Capabilities</span>
          <div style="margin-top:4px;">${caps}</div>
        </div>
        <div>
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Verification Status</span>
          <span style="font-weight:700;color:${teamProfile.verificationStatus === 'APPROVED' ? '#059669' : '#D97706'};">${teamProfile.verificationStatus}</span>
        </div>
        <div>
          <span style="color:var(--rescue-text-muted);display:block;font-size:0.6875rem;text-transform:uppercase;font-weight:700;">Operational Status</span>
          <span style="font-weight:700;">${teamProfile.operationalStatus}</span>
        </div>
      </div>

      <div class="rescue-form-group" style="background:#FAF8F2;padding:12px;border:1px solid var(--rescue-border);border-radius:6px;">
        <label class="rescue-label" for="selectTeamAvailability" style="font-weight:700;margin-bottom:6px;">Set Operational Availability</label>
        <select id="selectTeamAvailability" class="rescue-select">
          <option value="AVAILABLE" ${currentAvail === 'AVAILABLE' ? 'selected' : ''}>AVAILABLE — Ready for immediate mission deployment</option>
          <option value="OFF_DUTY" ${currentAvail === 'OFF_DUTY' ? 'selected' : ''}>OFF_DUTY — Off-hours or shift break (standby)</option>
          <option value="BUSY" ${currentAvail === 'BUSY' ? 'selected' : ''}>BUSY — Actively engaged in ongoing field response</option>
          <option value="INACTIVE" ${currentAvail === 'INACTIVE' ? 'selected' : ''}>INACTIVE — Unit temporarily non-operational</option>
        </select>
        <p style="font-size:0.75rem;color:var(--rescue-text-muted);margin:6px 0 0;">
          Your availability determines eligibility when DMO searches for the nearest rescue teams.
        </p>
      </div>
    `;

    openModal('profileModal');
  }

  let isSavingAvailability = false;
  async function handleSaveAvailability() {
    if (isSavingAvailability) return;
    const sel = document.getElementById('selectTeamAvailability');
    const btn = document.getElementById('btnSaveAvailability');
    if (!sel) return;
    const newStatus = sel.value;

    isSavingAvailability = true;
    const origHtml = btn ? btn.innerHTML : 'Save Status';
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Updating...';
    }

    try {
      if (window.LW?.api) {
        teamProfile = await window.LW.api.put('/api/rescue/availability', {
          availability: newStatus,
          availabilityStatus: newStatus
        });
      }
      updateProfileUI();
      closeModal('profileModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast(`Operational availability updated to ${newStatus}.`, 'success');
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to update availability', 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = origHtml;
      }
      isSavingAvailability = false;
    }
  }

  async function loadMissions(silent = false) {
    try {
      await loadTeamProfile();
      if (window.LW?.reportsStorage?.getRescueMissions) {
        allMissions = await window.LW.reportsStorage.getRescueMissions();
      } else {
        allMissions = [];
      }
    } catch (err) {
      console.error('[rescue-dashboard] Error loading missions:', err);
      allMissions = [];
    }

    renderSummaryCards();
    renderMissionsTable();
  }

  // 4. Render the 4 Compact Summary Cards
  function renderSummaryCards() {
    let countNew = 0;
    let countAccepted = 0;
    let countInProgress = 0;
    let countCompleted = 0;

    allMissions.forEach(m => {
      const simple = getMissionSimpleStatus(m);
      if (simple === 'NEW ASSIGNMENT') countNew++;
      else if (simple === 'ACCEPTED') countAccepted++;
      else if (simple === 'EN ROUTE' || simple === 'ON SCENE') countInProgress++;
      else if (simple === 'COMPLETED') countCompleted++;
    });

    const elNew = document.getElementById('statNewAssignments');
    const elAccepted = document.getElementById('statAcceptedMissions');
    const elProgress = document.getElementById('statInProgress');
    const elCompleted = document.getElementById('statCompletedMissions');

    if (elNew) elNew.textContent = countNew;
    if (elAccepted) elAccepted.textContent = countAccepted;
    if (elProgress) elProgress.textContent = countInProgress;
    if (elCompleted) elCompleted.textContent = countCompleted;
  }

  // 5. Render Assigned Missions Table
  function renderMissionsTable() {
    const tbody = document.getElementById('missionsTableBody');
    const emptyState = document.getElementById('emptyState');
    const countBadge = document.getElementById('missionsCountBadge');
    if (!tbody) return;

    // Filter missions (exclude declined unless in 'all' and no active matches)
    const filtered = allMissions.filter(m => {
      const simple = getMissionSimpleStatus(m);

      // Status pill filter
      if (activeFilter === 'NEW ASSIGNMENT' && simple !== 'NEW ASSIGNMENT') return false;
      if (activeFilter === 'ACCEPTED' && simple !== 'ACCEPTED') return false;
      if (activeFilter === 'IN PROGRESS' && simple !== 'EN ROUTE' && simple !== 'ON SCENE') return false;
      if (activeFilter === 'COMPLETED' && simple !== 'COMPLETED') return false;

      // Text search filter
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        const idMatch = String(m.id || '').toLowerCase().includes(query);
        const typeMatch = (m.incidentCategory || m.categoryDisplayName || '').toLowerCase().includes(query);
        const locMatch = (m.locationDescription || '').toLowerCase().includes(query);
        const descMatch = (m.description || '').toLowerCase().includes(query);
        if (!idMatch && !typeMatch && !locMatch && !descMatch) return false;
      }

      return true;
    });

    if (countBadge) {
      countBadge.textContent = `${filtered.length} ${filtered.length === 1 ? 'Mission' : 'Missions'}`;
    }

    if (filtered.length === 0) {
      tbody.innerHTML = '';
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = filtered.map(m => {
      const simple = getMissionSimpleStatus(m);
      const priority = (m.assignedPriority || m.priority || 'HIGH').toUpperCase();
      const priorityClass = priority === 'HIGH' ? 'badge-priority-high' : (priority === 'MEDIUM' ? 'badge-priority-medium' : 'badge-priority-low');

      let statusBadgeClass = 'badge-status-new';
      if (simple === 'ACCEPTED') statusBadgeClass = 'badge-status-accepted';
      else if (simple === 'EN ROUTE') statusBadgeClass = 'badge-status-enroute';
      else if (simple === 'ON SCENE') statusBadgeClass = 'badge-status-onscene';
      else if (simple === 'COMPLETED') statusBadgeClass = 'badge-status-completed';

      const lat = m.latitude != null ? Number(m.latitude) : null;
      const lng = m.longitude != null ? Number(m.longitude) : null;
      const coordsText = (lat != null && lng != null) ? `${lat.toFixed(4)}, ${lng.toFixed(4)}` : '—';
      const locText = m.locationDescription || 'Assigned Regional Sector';
      const dateText = formatAssignmentDate(m.assignedAt || m.reportedTimestamp || m.timestamp);

      // Quick action button
      let quickActionHtml = '';
      if (simple === 'NEW ASSIGNMENT') {
        quickActionHtml = `<button type="button" class="btn-primary-action" data-quick-action="accept" data-mission-id="${m.id}" title="Quick Accept">Accept</button>`;
      } else if (simple === 'ACCEPTED') {
        quickActionHtml = `<button type="button" class="btn-primary-action" data-quick-action="enroute" data-mission-id="${m.id}" title="Deploy En Route">En Route</button>`;
      } else if (simple === 'EN ROUTE') {
        quickActionHtml = `<button type="button" class="btn-primary-action" data-quick-action="onscene" data-mission-id="${m.id}" title="Arrived On Scene">On Scene</button>`;
      }

      return `
        <tr>
          <td class="mission-id-cell">#SG-${m.id}</td>
          <td><strong>${escapeHtml(formatIncidentCategory(m.incidentCategory))}</strong></td>
          <td><span class="badge-priority ${priorityClass}">${priority}</span></td>
          <td>
            <div class="mission-location-cell" title="${escapeHtml(locText)}">
              <img src="assets/icons/location-pin.png" alt="Location pin" class="location-pin-img">
              <span class="mission-location-text">${escapeHtml(locText)}</span>
            </div>
          </td>
          <td class="mission-coords">${coordsText}</td>
          <td>${dateText}</td>
          <td><span class="badge-status ${statusBadgeClass}">${simple}</span></td>
          <td style="text-align: right; white-space: nowrap;">
            <div style="display: inline-flex; align-items: center; gap: 8px;">
              ${quickActionHtml}
              <button type="button" class="btn-details" data-view-details="${m.id}">View Details</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  // 6. Open Mission Details Modal
  function openMissionDetails(missionId) {
    const mission = allMissions.find(m => String(m.id) === String(missionId));
    if (!mission) return;
    selectedMission = mission;

    const modal = document.getElementById('detailsModal');
    const modalTitle = document.getElementById('detailsModalTitle');
    const modalBody = document.getElementById('detailsModalBody');
    const modalFooter = document.getElementById('detailsModalFooter');

    if (!modal || !modalBody) return;

    const simple = getMissionSimpleStatus(mission);
    const priority = (mission.assignedPriority || mission.priority || 'HIGH').toUpperCase();
    const priorityClass = priority === 'HIGH' ? 'badge-priority-high' : (priority === 'MEDIUM' ? 'badge-priority-medium' : 'badge-priority-low');

    let statusBadgeClass = 'badge-status-new';
    if (simple === 'ACCEPTED') statusBadgeClass = 'badge-status-accepted';
    else if (simple === 'EN ROUTE') statusBadgeClass = 'badge-status-enroute';
    else if (simple === 'ON SCENE') statusBadgeClass = 'badge-status-onscene';
    else if (simple === 'COMPLETED') statusBadgeClass = 'badge-status-completed';

    const lat = mission.latitude != null ? Number(mission.latitude) : null;
    const lng = mission.longitude != null ? Number(mission.longitude) : null;
    const coordsText = (lat != null && lng != null) ? `${lat.toFixed(5)}, ${lng.toFixed(5)}` : 'Not available';
    const locText = mission.locationDescription || 'Assigned Regional Sector';
    const dateText = formatAssignmentDate(mission.assignedAt || mission.reportedTimestamp || mission.timestamp);

    modalTitle.innerHTML = `Mission Details: <span style="font-family:'JetBrains Mono',monospace;">#SG-${mission.id}</span>`;

    // Photos HTML
    let photosHtml = '<div style="font-size: 0.8125rem; color: var(--rescue-text-muted);">No photographs attached.</div>';
    if (mission.photos && mission.photos.length > 0) {
      photosHtml = `
        <div class="photo-gallery-preview">
          ${mission.photos.map(p => {
            const url = p.downloadUrl || p.dataUrl || p.url || `http://localhost:8080${p.downloadUrl || ''}`;
            return `<img src="${escapeHtml(url)}" alt="Incident photograph" class="photo-thumb" onclick="window.open('${escapeHtml(url)}', '_blank')">`;
          }).join('')}
        </div>
      `;
    }

    // Admin/DMO Instructions
    const instructionsText = mission.instructions || mission.adminFollowUpNotes || 'Verify on-site slope movement, inspect culvert drainage, evaluate road passability, and ensure citizen safety.';

    // Progress Notes History
    let progressHtml = '';
    if (mission.progressHistory && mission.progressHistory.length > 0) {
      progressHtml = `
        <div class="detail-item">
          <span class="detail-label">Field Response History</span>
          <div style="background: #FAF8F2; border: 1px solid var(--rescue-border-light); border-radius: 6px; padding: 10px; display: flex; flex-direction: column; gap: 8px;">
            ${mission.progressHistory.map(h => `
              <div style="font-size: 0.775rem; border-left: 3px solid var(--rescue-sage); padding-left: 8px;">
                <strong>${escapeHtml(h.siteAssessment || 'Assessment')}</strong>
                ${h.roadPassability ? `<span style="color:var(--rescue-text-muted);"> • Passability: ${escapeHtml(h.roadPassability)}</span>` : ''}
                ${h.actionsTaken ? `<div style="color:var(--rescue-text-secondary); margin-top:2px;">Actions: ${escapeHtml(h.actionsTaken)}</div>` : ''}
              </div>
            `).join('')}
          </div>
        </div>
      `;
    }

    modalBody.innerHTML = `
      <!-- Essential Info 2-Column Grid -->
      <div class="detail-grid-2col">
        <div class="detail-item">
          <span class="detail-label">Incident ID & Type</span>
          <div class="detail-val">#SG-${mission.id} — ${escapeHtml(formatIncidentCategory(mission.incidentCategory))}</div>
        </div>

        <div class="detail-item">
          <span class="detail-label">Priority & Status</span>
          <div style="display: flex; align-items: center; gap: 8px; margin-top: 2px;">
            <span class="badge-priority ${priorityClass}">${priority}</span>
            <span class="badge-status ${statusBadgeClass}">${simple}</span>
          </div>
        </div>

        <div class="detail-item">
          <span class="detail-label">Incident Location</span>
          <div style="display: flex; align-items: center; gap: 6px; margin-top: 2px;">
            <img src="assets/icons/location-pin.png" alt="Location pin" class="location-pin-img">
            <span class="detail-val">${escapeHtml(locText)}</span>
          </div>
        </div>

        <div class="detail-item">
          <span class="detail-label">GPS Coordinates</span>
          <div class="detail-val" style="font-family:'JetBrains Mono',monospace; font-size:0.8125rem;">
            ${coordsText}
          </div>
        </div>

        <div class="detail-item">
          <span class="detail-label">Assignment Date</span>
          <div class="detail-val">${dateText}</div>
        </div>

        <div class="detail-item">
          <span class="detail-label">Assigned Unit</span>
          <div class="detail-val">${escapeHtml(mission.assignedTeam || 'Rescue Force Unit Alpha')}</div>
        </div>
      </div>

      <!-- Admin / DMO Instructions -->
      <div class="detail-instruction-box">
        <div class="detail-instruction-label">DMO / Command Instructions</div>
        <div>${escapeHtml(instructionsText)}</div>
      </div>

      <!-- Citizen Description -->
      <div class="detail-item">
        <span class="detail-label">Citizen Incident Report</span>
        <div class="detail-description-box">
          ${escapeHtml(mission.description || 'No description provided by citizen.')}
        </div>
      </div>

      <!-- Photos Gallery -->
      <div class="detail-item">
        <span class="detail-label">Photographs / Visual Evidence</span>
        ${photosHtml}
      </div>

      <!-- Response History -->
      ${progressHtml}
    `;

    // Status-appropriate modal footer action buttons
    let actionButtonsHtml = '';
    if (simple === 'NEW ASSIGNMENT') {
      actionButtonsHtml = `
        <button type="button" class="btn-modal-decline" id="btnModalDecline">Decline Assignment</button>
        <button type="button" class="btn-modal-forest" id="btnModalAccept">Accept Assignment</button>
      `;
    } else if (simple === 'ACCEPTED') {
      actionButtonsHtml = `
        <button type="button" class="btn-modal-forest" id="btnModalEnRoute">Mark En Route</button>
      `;
    } else if (simple === 'EN ROUTE') {
      actionButtonsHtml = `
        <button type="button" class="btn-modal-forest" id="btnModalOnScene">Mark On Scene</button>
      `;
    } else if (simple === 'ON SCENE') {
      actionButtonsHtml = `
        <button type="button" class="btn-modal-close" id="btnModalNotes">Submit Response Notes</button>
        <button type="button" class="btn-modal-success" id="btnModalComplete">Mark Completed</button>
      `;
    } else if (simple === 'COMPLETED') {
      actionButtonsHtml = `
        <span class="badge-status badge-status-completed" style="margin-right: auto;">Mission Completed & Verified</span>
      `;
    }

    modalFooter.innerHTML = `
      ${actionButtonsHtml}
      <button type="button" class="btn-modal-close" data-close-modal="detailsModal">Close</button>
    `;

    // Attach listeners to modal action buttons
    const btnAccept = document.getElementById('btnModalAccept');
    if (btnAccept) btnAccept.onclick = () => handleAccept(mission.id);

    const btnDecline = document.getElementById('btnModalDecline');
    if (btnDecline) btnDecline.onclick = () => openDeclineModal(mission);

    const btnEnRoute = document.getElementById('btnModalEnRoute');
    if (btnEnRoute) btnEnRoute.onclick = () => handleStartJourney(mission.id);

    const btnOnScene = document.getElementById('btnModalOnScene');
    if (btnOnScene) btnOnScene.onclick = () => handleMarkArrival(mission.id);

    const btnNotes = document.getElementById('btnModalNotes');
    if (btnNotes) btnNotes.onclick = () => openNotesModal(mission);

    const btnComplete = document.getElementById('btnModalComplete');
    if (btnComplete) btnComplete.onclick = () => openCompleteModal(mission);

    modal.classList.add('active');
  }

  // 7. Mission Action Handlers

  // Accept Mission with Confirmation
  async function handleAccept(missionId) {
    if (!missionId) return;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Accept Rescue Assignment',
      message: `Accept rescue response assignment for Incident #SG-${missionId}? Your team will be deployed.`,
      confirmText: 'Accept Assignment',
      variant: 'success'
    });
    if (!confirmed) return;

    const btnAccept = document.getElementById('btnModalAccept');
    if (btnAccept) {
      btnAccept.disabled = true;
      btnAccept.innerHTML = '<span class="lw-btn-spinner"></span> Accepting...';
    }

    try {
      await window.LW.reportsStorage.acceptMission(missionId, currentUser);
      closeModal('detailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast('Mission accepted. Ready for deployment.', 'success');
      await loadMissions();
    } catch (err) {
      if (btnAccept) {
        btnAccept.disabled = false;
        btnAccept.innerHTML = 'Accept Assignment';
      }
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to accept mission', 'error');
    }
  }

  // Decline Mission
  function openDeclineModal(mission) {
    selectedMission = mission;
    const input = document.getElementById('declineReasonInput');
    if (input) input.value = '';
    openModal('declineModal');
  }

  let isDeclining = false;
  async function handleConfirmDecline() {
    if (!selectedMission || isDeclining) return;
    const btn = document.getElementById('btnConfirmDecline');
    const input = document.getElementById('declineReasonInput');
    const reason = input ? input.value.trim() : '';

    if (!reason) {
      if (window.LW?.ui?.toast) window.LW.ui.toast('A mandatory reason must be provided to decline.', 'warning');
      input?.focus();
      return;
    }

    isDeclining = true;
    const origHtml = btn ? btn.innerHTML : 'Confirm Decline';
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Declining...';
    }

    try {
      await window.LW.reportsStorage.declineMission(selectedMission.id, reason, currentUser);
      closeModal('declineModal');
      closeModal('detailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast('Mission assignment declined.', 'info');
      await loadMissions();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to decline mission', 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = origHtml;
      }
      isDeclining = false;
    }
  }

  // Start Journey (En Route) with Confirmation
  async function handleStartJourney(missionId) {
    if (!missionId) return;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Confirm Deployment Departure',
      message: `Mark response unit as EN ROUTE to Incident #SG-${missionId}?`,
      confirmText: 'Depart Base (En Route)',
      variant: 'primary'
    });
    if (!confirmed) return;

    const btn = document.getElementById('btnModalEnRoute');
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Deploying...';
    }

    try {
      await window.LW.reportsStorage.startJourney(missionId, currentUser);
      closeModal('detailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast('Response team deployed (EN ROUTE).', 'success');
      await loadMissions();
    } catch (err) {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = 'Depart Base (En Route)';
      }
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to update status', 'error');
    }
  }

  // Mark Arrival (On Scene) with Confirmation
  async function handleMarkArrival(missionId) {
    if (!missionId) return;

    const confirmed = await window.LW.ui.confirmAction({
      title: 'Confirm Arrival On Scene',
      message: `Confirm that your response team has arrived at Incident #SG-${missionId}?`,
      confirmText: 'Mark On Scene',
      variant: 'forest'
    });
    if (!confirmed) return;

    const btn = document.getElementById('btnModalOnScene');
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Recording Arrival...';
    }

    try {
      await window.LW.reportsStorage.markArrival(missionId, currentUser);
      closeModal('detailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast('Status updated to ON SCENE.', 'success');
      await loadMissions();
    } catch (err) {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = 'Mark On Scene';
      }
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to mark arrival', 'error');
    }
  }

  // Submit Response Notes Modal
  function openNotesModal(mission) {
    selectedMission = mission;
    const assessment = document.getElementById('notesSiteAssessment');
    const passability = document.getElementById('notesRoadPassability');
    const actions = document.getElementById('notesActionsTaken');

    if (assessment) assessment.value = '';
    if (passability) passability.value = 'Partially Blocked';
    if (actions) actions.value = '';

    openModal('notesModal');
  }

  let isSavingNotes = false;
  async function handleSaveNotes() {
    if (!selectedMission || isSavingNotes) return;
    const btn = document.getElementById('btnSaveNotes');
    const assessmentEl = document.getElementById('notesSiteAssessment');
    const passabilityEl = document.getElementById('notesRoadPassability');
    const actionsEl = document.getElementById('notesActionsTaken');

    const assessment = assessmentEl ? assessmentEl.value.trim() : '';
    const passability = passabilityEl ? passabilityEl.value : 'Passable';
    const actions = actionsEl ? actionsEl.value.trim() : '';

    if (!assessment) {
      if (window.LW?.ui?.toast) window.LW.ui.toast('Site assessment notes are required.', 'warning');
      assessmentEl?.focus();
      return;
    }

    isSavingNotes = true;
    const origHtml = btn ? btn.innerHTML : 'Save & Submit Notes';
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Saving Notes...';
    }

    try {
      await window.LW.reportsStorage.submitProgressUpdate(selectedMission.id, {
        siteAssessment: assessment,
        roadPassability: passability,
        actionsTaken: actions
      }, currentUser);

      closeModal('notesModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast('Field response notes saved successfully.', 'success');
      await loadMissions();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to save notes', 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = origHtml;
      }
      isSavingNotes = false;
    }
  }

  // Complete Mission Modal
  function openCompleteModal(mission) {
    selectedMission = mission;
    const summary = document.getElementById('outcomeResolutionSummary');
    const actions = document.getElementById('outcomeActionsPerformed');

    if (summary) summary.value = '';
    if (actions) actions.value = '';

    openModal('completeModal');
  }

  let isCompleting = false;
  async function handleConfirmComplete() {
    if (!selectedMission || isCompleting) return;
    const btn = document.getElementById('btnConfirmComplete');
    const summaryEl = document.getElementById('outcomeResolutionSummary');
    const actionsEl = document.getElementById('outcomeActionsPerformed');

    const summary = summaryEl ? summaryEl.value.trim() : '';
    const actions = actionsEl ? actionsEl.value.trim() : '';

    if (!summary) {
      if (window.LW?.ui?.toast) window.LW.ui.toast('Resolution summary is required.', 'warning');
      summaryEl?.focus();
      return;
    }

    isCompleting = true;
    const origHtml = btn ? btn.innerHTML : 'Submit Final Outcome';
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="lw-btn-spinner"></span> Submitting Outcome...';
    }

    try {
      await window.LW.reportsStorage.submitOutcomeForVerification(selectedMission.id, {
        resolutionSummary: summary,
        actionsPerformed: actions
      }, currentUser);

      closeModal('completeModal');
      closeModal('detailsModal');
      if (window.LW?.ui?.toast) window.LW.ui.toast('Mission completed and submitted for administrative verification.', 'success');
      await loadMissions();
    } catch (err) {
      if (window.LW?.ui?.toast) window.LW.ui.toast(err.message || 'Failed to complete mission', 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = origHtml;
      }
      isCompleting = false;
    }
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

  // 8. Event Listeners Setup
  function initEventListeners() {
    // Search input
    const searchInput = document.getElementById('missionSearchInput');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value.trim();
        renderMissionsTable();
      });
    }

    // Filter pills
    const filterPills = document.getElementById('filterPills');
    if (filterPills) {
      filterPills.addEventListener('click', (e) => {
        const btn = e.target.closest('.filter-pill');
        if (!btn) return;
        filterPills.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        activeFilter = btn.dataset.filter || 'all';
        renderMissionsTable();
      });
    }

    // Missions table delegation
    const tbody = document.getElementById('missionsTableBody');
    if (tbody) {
      tbody.addEventListener('click', (e) => {
        const detailsBtn = e.target.closest('[data-view-details]');
        if (detailsBtn) {
          const id = detailsBtn.getAttribute('data-view-details');
          openMissionDetails(id);
          return;
        }

        const quickBtn = e.target.closest('[data-quick-action]');
        if (quickBtn) {
          const action = quickBtn.getAttribute('data-quick-action');
          const id = quickBtn.getAttribute('data-mission-id');
          if (action === 'accept') handleAccept(id);
          else if (action === 'enroute') handleStartJourney(id);
          else if (action === 'onscene') handleMarkArrival(id);
        }
      });
    }

    // Modal close buttons
    document.addEventListener('click', (e) => {
      const closeTarget = e.target.closest('[data-close-modal]');
      if (closeTarget) {
        const modalId = closeTarget.getAttribute('data-close-modal');
        closeModal(modalId);
      }
    });

    // Close modal on backdrop click
    document.querySelectorAll('.rescue-modal-backdrop').forEach(backdrop => {
      backdrop.addEventListener('click', (e) => {
        if (e.target === backdrop) {
          backdrop.classList.remove('active');
        }
      });
    });

    // Modal submit buttons
    const btnConfirmDecline = document.getElementById('btnConfirmDecline');
    if (btnConfirmDecline) btnConfirmDecline.onclick = handleConfirmDecline;

    const btnSaveNotes = document.getElementById('btnSaveNotes');
    if (btnSaveNotes) btnSaveNotes.onclick = handleSaveNotes;

    const btnConfirmComplete = document.getElementById('btnConfirmComplete');
    if (btnConfirmComplete) btnConfirmComplete.onclick = handleConfirmComplete;

    // Team Profile & Availability
    const btnTeamProfile = document.getElementById('btnTeamProfile');
    if (btnTeamProfile) {
      btnTeamProfile.addEventListener('click', () => {
        openProfileModal();
      });
    }

    const btnSaveAvailability = document.getElementById('btnSaveAvailability');
    if (btnSaveAvailability) btnSaveAvailability.onclick = handleSaveAvailability;

    // Listen for cross-tab or service syncs
    if (window.LW?.reportsStorage?.onSync) {
      window.LW.reportsStorage.onSync(() => {
        loadMissions(true);
      });
    }
  }

  // Initialization
  async function init() {
    if (!initAuthGuard()) return;
    initEventListeners();
    await loadMissions();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
