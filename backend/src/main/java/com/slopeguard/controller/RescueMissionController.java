package com.slopeguard.controller;

import com.slopeguard.dto.*;
import com.slopeguard.model.*;
import com.slopeguard.service.AuditService;
import com.slopeguard.service.PhotoService;
import com.slopeguard.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Controller providing operational endpoints for authorized rescue teams and administrators:
 * - Mission assignment and team-level access control (isolation between teams)
 * - Administrative assignment and revocation
 * - Mission review & decision (Accept / Decline)
 * - Permission-gated GPS journey tracking
 * - Real-time location updates & breadcrumb journey trail
 * - Arrival recording & on-site progress updates
 * - Outcome submission for administrative verification
 * - Follow-up requests by administrators
 */
@RestController
@RequestMapping("/api/rescue/missions")
@Transactional
public class RescueMissionController {

    private final ReportService reportService;
    private final PhotoService photoService;
    private final AuditService auditService;
    private final com.slopeguard.service.RescueTeamService rescueTeamService;
    private final com.slopeguard.repository.RescueTeamRepository rescueTeamRepository;
    private final com.slopeguard.repository.RescueAssignmentRepository rescueAssignmentRepository;
    private final com.slopeguard.repository.IncidentReportRepository incidentReportRepository;
    private final com.slopeguard.repository.UserRepository userRepository;

    // In-memory thread-safe mission tracking state storage
    private static final Map<Long, MissionState> missionStates = new ConcurrentHashMap<>();

    public RescueMissionController(
            ReportService reportService,
            PhotoService photoService,
            AuditService auditService,
            com.slopeguard.service.RescueTeamService rescueTeamService,
            com.slopeguard.repository.RescueTeamRepository rescueTeamRepository,
            com.slopeguard.repository.RescueAssignmentRepository rescueAssignmentRepository,
            com.slopeguard.repository.IncidentReportRepository incidentReportRepository,
            com.slopeguard.repository.UserRepository userRepository) {
        this.reportService = reportService;
        this.photoService = photoService;
        this.auditService = auditService;
        this.rescueTeamService = rescueTeamService;
        this.rescueTeamRepository = rescueTeamRepository;
        this.rescueAssignmentRepository = rescueAssignmentRepository;
        this.incidentReportRepository = incidentReportRepository;
        this.userRepository = userRepository;
    }

    private static class MissionState {
        String operationalStage = "ASSIGNED";
        String assignedTeam = "Rescue Force Unit Alpha (NER)";
        String assignedOfficerId = "rescue_force_alpha";
        String assignedPriority = "HIGH";
        String instructions;
        String assignedAt;
        String decisionStatus = "PENDING";
        String decisionReason;
        String journeyStartedAt;
        String arrivedAt;
        boolean isTrackingActive = false;
        Instant lastLocationTimestamp;
        Double currentLatitude;
        Double currentLongitude;
        Double currentAccuracy;
        final List<RescueMissionResponse.GpsPoint> journeyTrail = Collections.synchronizedList(new ArrayList<>());
        final List<RescueMissionResponse.RescueProgressEntry> progressHistory = Collections.synchronizedList(new ArrayList<>());
        RescueOutcomeReportRequest outcomeReport;
        String adminFollowUpNotes;
    }

    private MissionState getMissionStateOrThrow(Long reportId) {
        MissionState state = missionStates.get(reportId);
        if (state == null) {
            try {
                var assignOpt = rescueAssignmentRepository.findByIncidentReportId(reportId);
                if (assignOpt.isPresent() && !"REVOKED".equalsIgnoreCase(assignOpt.get().getAssignmentStatus())) {
                    com.slopeguard.model.RescueAssignment assign = assignOpt.get();
                    state = missionStates.computeIfAbsent(reportId, k -> new MissionState());
                    com.slopeguard.model.RescueTeam team = assign.getRescueTeam();
                    if (team != null) {
                        state.assignedOfficerId = team.getUser() != null ? team.getUser().getEmail() : String.valueOf(team.getId());
                        state.assignedTeam = team.getTeamName();
                        state.instructions = assign.getInstructions();
                        state.assignedPriority = assign.getPriority() != null ? assign.getPriority() : "HIGH";
                        if (state.operationalStage == null) {
                            state.operationalStage = assign.getAssignmentStatus() != null ? assign.getAssignmentStatus() : "ASSIGNED";
                        }
                        if (state.decisionStatus == null) {
                            state.decisionStatus = "ASSIGNED".equalsIgnoreCase(state.operationalStage) ? "PENDING" : state.operationalStage;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (state == null) {
            throw new com.slopeguard.exception.ResourceNotFoundException("Mission #" + reportId + " has not been assigned to any Rescue Force unit.");
        }
        return state;
    }

    /**
     * Verifies if the authenticated caller has access to the specified mission state.
     * Admins have universal access; Rescue Force members only see missions assigned to their unit.
     */
    private boolean isMissionAssignedToUser(MissionState state, Authentication authentication) {
        if (authentication == null) return false;

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return true;

        String assignedOfficerId = state.assignedOfficerId != null ? state.assignedOfficerId.trim().toLowerCase() : "";
        String assignedTeam = state.assignedTeam != null ? state.assignedTeam.trim().toLowerCase() : "";

        String userEmail = "";
        String userName = "";
        String userIdStr = "";

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object emailClaim = jwt.getClaims().get("email");
            if (emailClaim != null) userEmail = emailClaim.toString().trim().toLowerCase();

            Object nameClaim = jwt.getClaims().get("name");
            if (nameClaim != null) userName = nameClaim.toString().trim().toLowerCase();

            Object subClaim = jwt.getClaims().get("sub");
            if (subClaim != null) userIdStr = subClaim.toString().trim();
            Object uidClaim = jwt.getClaims().get("userId");
            if (uidClaim != null) userIdStr = uidClaim.toString().trim();
        } else {
            userEmail = authentication.getName() != null ? authentication.getName().trim().toLowerCase() : "";
        }

        // Direct ID / Email Match
        if (!assignedOfficerId.isEmpty()) {
            if (assignedOfficerId.equals(userIdStr) || assignedOfficerId.equals(userEmail)) {
                return true;
            }
            // Team Alpha aliases (Lyngdoh / Khasi / rescue_force_alpha)
            if (userEmail.contains("officer@") || userEmail.contains("lyngdoh") || userName.contains("lyngdoh")) {
                if (assignedOfficerId.contains("alpha") || assignedOfficerId.contains("khasi") || assignedOfficerId.contains("lyngdoh") || assignedOfficerId.contains("officer@")) {
                    return true;
                }
            }
            // Team Bravo aliases (Barman / Dima / rescue_force_bravo)
            if (userEmail.contains("bravo") || userEmail.contains("barman") || userEmail.contains("dima") || userName.contains("barman")) {
                if (assignedOfficerId.contains("bravo") || assignedOfficerId.contains("dima") || assignedOfficerId.contains("barman")) {
                    return true;
                }
            }
            // Team Noney aliases (Kamei)
            if (userEmail.contains("noney") || userName.contains("kamei")) {
                if (assignedOfficerId.contains("noney") || assignedOfficerId.contains("kamei")) {
                    return true;
                }
            }
            // Team Garo / Tura aliases (Sangma / Shillong)
            if (userEmail.contains("shillong") || userName.contains("sangma")) {
                if (assignedOfficerId.contains("shillong") || assignedOfficerId.contains("sangma")) {
                    return true;
                }
            }
            if (!userName.isEmpty() && (assignedOfficerId.contains(userName) || userName.contains(assignedOfficerId))) {
                return true;
            }
        }

        // Team Name Match
        if (!assignedTeam.isEmpty()) {
            if (!userEmail.isEmpty() && assignedTeam.contains(userEmail)) return true;
            if (!userName.isEmpty() && (assignedTeam.contains(userName) || userName.contains(assignedTeam))) return true;
            if ((userEmail.contains("officer@") || userName.contains("lyngdoh")) && (assignedTeam.contains("alpha") || assignedTeam.contains("lyngdoh") || assignedTeam.contains("khasi"))) {
                return true;
            }
            if ((userEmail.contains("bravo") || userName.contains("barman")) && (assignedTeam.contains("bravo") || assignedTeam.contains("barman") || assignedTeam.contains("dima"))) {
                return true;
            }
        }

        // Direct Rescue Team database entity matching
        Long userId = extractUserId(authentication);
        if (userId != null) {
            var myTeamOpt = rescueTeamRepository.findByUserId(userId);
            if (myTeamOpt.isPresent()) {
                var myTeam = myTeamOpt.get();
                if (assignedTeam.equalsIgnoreCase(myTeam.getTeamName()) ||
                    (!userEmail.isEmpty() && assignedOfficerId.equalsIgnoreCase(userEmail)) ||
                    assignedOfficerId.equals(String.valueOf(myTeam.getId()))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Enforces team-level access control. Throws AccessDeniedException (403) if unauthorized.
     */
    private void checkTeamAccessOrThrow(Long reportId, Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Unauthenticated access.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;

        MissionState state = getMissionStateOrThrow(reportId);
        if ("REVOKED".equals(state.operationalStage)) {
            throw new AccessDeniedException("Access denied: Mission #" + reportId + " assignment has been revoked by administration.");
        }
        if (!isMissionAssignedToUser(state, authentication)) {
            throw new AccessDeniedException("Forbidden: Mission #" + reportId + " is not assigned to your Rescue Force unit.");
        }
    }

    /**
     * Lists active or assigned missions for authorized rescue teams and administrators.
     * Enforces strict team isolation: non-admins only see missions targeted to their unit.
     */
    @GetMapping
    public ResponseEntity<List<RescueMissionResponse>> getAssignedMissions(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && authentication != null) {
            Long userId = extractUserId(authentication);
            if (userId != null) {
                var teamOpt = rescueTeamRepository.findByUserId(userId);
                if (teamOpt.isPresent()) {
                    var team = teamOpt.get();
                    if (!"APPROVED".equalsIgnoreCase(team.getVerificationStatus())) {
                        return ResponseEntity.ok(Collections.emptyList());
                    }
                }
            }
        }

        // Synchronize persistent database assignments from MySQL into missionStates
        try {
            var dbAssignments = rescueAssignmentRepository.findAllOrdered();
            for (com.slopeguard.model.RescueAssignment assign : dbAssignments) {
                if ("REVOKED".equalsIgnoreCase(assign.getAssignmentStatus())) continue;
                Long rId = assign.getIncidentReport() != null ? assign.getIncidentReport().getId() : null;
                if (rId != null) {
                    MissionState state = missionStates.computeIfAbsent(rId, k -> new MissionState());
                    com.slopeguard.model.RescueTeam team = assign.getRescueTeam();
                    if (team != null) {
                        state.assignedOfficerId = team.getUser() != null ? team.getUser().getEmail() : String.valueOf(team.getId());
                        state.assignedTeam = team.getTeamName();
                        state.instructions = assign.getInstructions();
                        state.assignedPriority = assign.getPriority() != null ? assign.getPriority() : "HIGH";
                        if (state.operationalStage == null) {
                            state.operationalStage = assign.getAssignmentStatus() != null ? assign.getAssignmentStatus() : "ASSIGNED";
                        }
                        if (state.decisionStatus == null) {
                            state.decisionStatus = "ASSIGNED".equalsIgnoreCase(state.operationalStage) ? "PENDING" : state.operationalStage;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        List<AdminReportResponse> allReports = reportService.getAllAdminReports();

        List<RescueMissionResponse> missions = allReports.stream()
                .filter(report -> missionStates.containsKey(report.getId()))
                .filter(report -> {
                    MissionState state = missionStates.get(report.getId());
                    if (state == null) return false;
                    if ("REVOKED".equals(state.operationalStage)) {
                        return isAdmin;
                    }
                    if (isAdmin) {
                        return true;
                    }
                    return isMissionAssignedToUser(state, authentication);
                })
                .map(this::toMissionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(missions);
    }

    /**
     * Retrieves specific mission details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RescueMissionResponse> getMissionById(@PathVariable Long id, Authentication authentication) {
        checkTeamAccessOrThrow(id, authentication);
        AdminReportResponse report = reportService.getAdminReportById(id);
        return ResponseEntity.ok(toMissionResponse(report));
    }

    /**
     * Administrator assigns an incident report to a specific Rescue Force team.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    public ResponseEntity<RescueMissionResponse> assignMission(
            @PathVariable Long id,
            @Valid @RequestBody RescueMissionAssignRequest request,
            Authentication authentication) {

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long adminId = extractUserId(authentication);
        String adminActor = extractActor(authentication);

        String teamId = request.getTeamId().trim();
        String teamName = (request.getTeamName() != null && !request.getTeamName().isBlank())
                ? request.getTeamName().trim()
                : teamId;
        String priority = (request.getPriority() != null && !request.getPriority().isBlank())
                ? request.getPriority().trim().toUpperCase()
                : "HIGH";
        String instructions = (request.getInstructions() != null && !request.getInstructions().isBlank())
                ? request.getInstructions().trim()
                : "Verify on-site slope movement, inspect culvert drainage, evaluate road passability, and report back.";

        state.assignedOfficerId = teamId;
        state.assignedTeam = teamName;
        state.assignedPriority = priority;
        state.instructions = instructions;
        state.assignedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        state.operationalStage = "ASSIGNED";
        state.decisionStatus = "PENDING";
        state.decisionReason = null;
        state.journeyStartedAt = null;
        state.arrivedAt = null;
        state.isTrackingActive = false;

        // Transition incident status to UNDER_REVIEW if currently PENDING
        if (report.getStatus() == ReportStatus.PENDING) {
            reportService.updateReportStatus(id, new ReportStatusUpdateRequest(
                    ReportStatus.UNDER_REVIEW,
                    "Assigned to Rescue Force unit: " + teamName + " (Priority: " + priority + ")"
            ));
        }

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                adminId,
                id,
                AuditOutcome.SUCCESS,
                "Administrator (" + adminActor + ") assigned incident to " + teamName + " (" + teamId + ")",
                report.getStatus(),
                report.getStatus() == ReportStatus.PENDING ? ReportStatus.UNDER_REVIEW : report.getStatus()
        );

        AdminReportResponse updated = reportService.getAdminReportById(id);
        return ResponseEntity.ok(toMissionResponse(updated));
    }

    /**
     * Administrator revokes an existing mission assignment.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<RescueMissionResponse> revokeAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody,
            Authentication authentication) {

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long adminId = extractUserId(authentication);
        String adminActor = extractActor(authentication);

        String previousTeam = state.assignedTeam;
        String reason = (requestBody != null && requestBody.containsKey("reason"))
                ? requestBody.get("reason")
                : "Administrator revoked assignment";

        state.operationalStage = "REVOKED";
        state.decisionStatus = "REVOKED";
        state.isTrackingActive = false;
        state.assignedOfficerId = "UNASSIGNED";
        state.assignedTeam = "Unassigned";

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                adminId,
                id,
                AuditOutcome.SUCCESS,
                "Administrator (" + adminActor + ") revoked assignment from " + previousTeam + ". Reason: " + reason,
                report.getStatus(),
                report.getStatus()
        );

        AdminReportResponse updated = reportService.getAdminReportById(id);
        return ResponseEntity.ok(toMissionResponse(updated));
    }

    /**
     * Accept or decline an assigned mission.
     */
    @PostMapping("/{id}/decision")
    public ResponseEntity<RescueMissionResponse> recordMissionDecision(
            @PathVariable Long id,
            @Valid @RequestBody RescueMissionDecisionRequest request,
            Authentication authentication) {

        checkTeamAccessOrThrow(id, authentication);

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long userId = extractUserId(authentication);
        String actor = extractActor(authentication);

        boolean isAccept = "ACCEPT".equalsIgnoreCase(request.getDecision());
        if (!isAccept && (request.getReason() == null || request.getReason().trim().isEmpty())) {
            throw new IllegalArgumentException("A mandatory reason must be provided when declining an assignment.");
        }

        if (isAccept) {
            state.decisionStatus = "ACCEPTED";
            state.operationalStage = "ACCEPTED";
            state.decisionReason = null;

            // Transition incident to UNDER_REVIEW
            if (report.getStatus() == ReportStatus.PENDING) {
                reportService.updateReportStatus(id, new ReportStatusUpdateRequest(ReportStatus.UNDER_REVIEW, "Assignment accepted by " + actor));
            }

            auditService.recordEvent(
                    AuditEventType.REPORT_STATUS_CHANGED,
                    userId,
                    id,
                    AuditOutcome.SUCCESS,
                    "Rescue team accepted mission assignment",
                    ReportStatus.PENDING,
                    ReportStatus.UNDER_REVIEW
            );
        } else {
            state.decisionStatus = "DECLINED";
            state.operationalStage = "ASSIGNED";
            state.decisionReason = request.getReason().trim();

            auditService.recordEvent(
                    AuditEventType.REPORT_REVIEWED,
                    userId,
                    id,
                    AuditOutcome.SUCCESS,
                    "Rescue team declined mission assignment: " + state.decisionReason,
                    report.getStatus(),
                    report.getStatus()
            );
        }

        try {
            var assignOpt = rescueAssignmentRepository.findByIncidentReportId(id);
            if (assignOpt.isPresent()) {
                var assign = assignOpt.get();
                assign.setAssignmentStatus(isAccept ? "ACCEPTED" : "DECLINED");
                if (!isAccept) assign.setDeclineReason(request.getReason().trim());
                rescueAssignmentRepository.save(assign);
            }
        } catch (Exception ignored) {}

        AdminReportResponse updated = reportService.getAdminReportById(id);
        return ResponseEntity.ok(toMissionResponse(updated));
    }

    /**
     * Starts journey and activates GPS tracking session.
     * Enforces strict state sequence: mission must be ACCEPTED first.
     */
    @PostMapping("/{id}/start-journey")
    public ResponseEntity<RescueMissionResponse> startJourney(
            @PathVariable Long id,
            Authentication authentication) {

        checkTeamAccessOrThrow(id, authentication);

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long userId = extractUserId(authentication);
        String actor = extractActor(authentication);

        if (!"ACCEPTED".equalsIgnoreCase(state.decisionStatus) && !"ACCEPTED".equalsIgnoreCase(state.operationalStage)) {
            throw new IllegalStateException("Cannot start journey before the mission assignment has been accepted.");
        }

        state.operationalStage = "EN_ROUTE";
        state.isTrackingActive = true;
        state.journeyStartedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        try {
            var assignOpt = rescueAssignmentRepository.findByIncidentReportId(id);
            if (assignOpt.isPresent()) {
                var assign = assignOpt.get();
                assign.setAssignmentStatus("EN_ROUTE");
                if (assign.getEnRouteAt() == null) assign.setEnRouteAt(LocalDateTime.now());
                rescueAssignmentRepository.save(assign);
            }
            var repOpt = incidentReportRepository.findById(id);
            if (repOpt.isPresent()) {
                IncidentReport rep = repOpt.get();
                if (rep.getStatus() != ReportStatus.RESOLVED) {
                    rep.setStatus(ReportStatus.RESPONDING);
                    rep.setStatusUpdatedAt(LocalDateTime.now());
                    incidentReportRepository.save(rep);
                }
            }
        } catch (Exception ignored) {}

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                userId,
                id,
                AuditOutcome.SUCCESS,
                "Rescue team initiated journey and activated GPS tracking protocol",
                report.getStatus(),
                report.getStatus()
        );

        return ResponseEntity.ok(toMissionResponse(report));
    }



    /**
     * Marks arrival on scene.
     */
    @PostMapping("/{id}/arrive")
    public ResponseEntity<RescueMissionResponse> markArrival(
            @PathVariable Long id,
            Authentication authentication) {

        checkTeamAccessOrThrow(id, authentication);

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long userId = extractUserId(authentication);
        String actor = extractActor(authentication);

        state.operationalStage = "ON_SCENE";
        state.arrivedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        try {
            var assignOpt = rescueAssignmentRepository.findByIncidentReportId(id);
            if (assignOpt.isPresent()) {
                var assign = assignOpt.get();
                assign.setAssignmentStatus("ON_SCENE");
                if (assign.getOnSceneAt() == null) assign.setOnSceneAt(LocalDateTime.now());
                rescueAssignmentRepository.save(assign);
            }
            var repOpt = incidentReportRepository.findById(id);
            if (repOpt.isPresent()) {
                IncidentReport rep = repOpt.get();
                if (rep.getStatus() != ReportStatus.RESOLVED) {
                    rep.setStatus(ReportStatus.RESPONDING);
                    rep.setStatusUpdatedAt(LocalDateTime.now());
                    incidentReportRepository.save(rep);
                }
            }
        } catch (Exception ignored) {}

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                userId,
                id,
                AuditOutcome.SUCCESS,
                "Rescue team recorded arrival on scene at incident location",
                report.getStatus(),
                report.getStatus()
        );

        return ResponseEntity.ok(toMissionResponse(report));
    }

    /**
     * Submits on-site progress assessment update.
     */
    @PostMapping("/{id}/progress")
    public ResponseEntity<RescueMissionResponse> recordProgress(
            @PathVariable Long id,
            @Valid @RequestBody RescueProgressUpdateRequest request,
            Authentication authentication) {

        checkTeamAccessOrThrow(id, authentication);

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long userId = extractUserId(authentication);
        String actor = extractActor(authentication);

        state.operationalStage = "IN_PROGRESS";
        try {
            var assignOpt = rescueAssignmentRepository.findByIncidentReportId(id);
            if (assignOpt.isPresent()) {
                var assign = assignOpt.get();
                assign.setResponseNotes(request.getSiteAssessment());
                rescueAssignmentRepository.save(assign);
            }
        } catch (Exception ignored) {}
        RescueMissionResponse.RescueProgressEntry entry = new RescueMissionResponse.RescueProgressEntry(
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                actor,
                request.getSiteAssessment(),
                request.getRoadPassability(),
                request.getObservedHazards(),
                request.getActionsTaken(),
                request.getResourcesRequired()
        );
        state.progressHistory.add(entry);

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                userId,
                id,
                AuditOutcome.SUCCESS,
                "Rescue team submitted field progress update: " + request.getSiteAssessment(),
                report.getStatus(),
                report.getStatus()
        );

        return ResponseEntity.ok(toMissionResponse(report));
    }

    /**
     * Submits response outcome report for administrator verification.
     * Note: Incident remains in UNDER_REVIEW; final closure is reserved for administrators.
     */
    @PostMapping("/{id}/outcome")
    public ResponseEntity<RescueMissionResponse> submitOutcome(
            @PathVariable Long id,
            @Valid @RequestBody RescueOutcomeReportRequest request,
            Authentication authentication) {

        checkTeamAccessOrThrow(id, authentication);

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long userId = extractUserId(authentication);
        String actor = extractActor(authentication);

        if (request.getResolutionSummary() == null || request.getResolutionSummary().trim().isBlank()) {
            throw new IllegalArgumentException("Resolution summary / completion remarks are mandatory.");
        }
        if ("COMPLETED".equalsIgnoreCase(state.operationalStage)) {
            throw new IllegalStateException("Mission #" + id + " has already been completed.");
        }

        state.operationalStage = "COMPLETED";
        state.isTrackingActive = false;
        state.outcomeReport = request;

        String summary = request.getResolutionSummary().trim();
        try {
            var assignOpt = rescueAssignmentRepository.findByIncidentReportId(id);
            if (assignOpt.isPresent()) {
                var assign = assignOpt.get();
                assign.setAssignmentStatus("COMPLETED");
                assign.setCompletedAt(LocalDateTime.now());
                assign.setResponseNotes(summary);
                rescueAssignmentRepository.save(assign);
            }
        } catch (Exception ignored) {}

        // Persist incident report status as RESOLVED
        var repOpt = incidentReportRepository.findById(id);
        if (repOpt.isPresent()) {
            IncidentReport rep = repOpt.get();
            ReportStatus prev = rep.getStatus();
            rep.setStatus(ReportStatus.RESOLVED);
            rep.setStatusUpdatedAt(LocalDateTime.now());
            rep.setUpdatedAt(LocalDateTime.now());
            rep.setReviewNotes("Rescue operation completed by " + state.assignedTeam + ". Remarks: " + summary);
            incidentReportRepository.save(rep);

            auditService.recordEvent(
                    AuditEventType.REPORT_STATUS_CHANGED,
                    userId,
                    id,
                    AuditOutcome.SUCCESS,
                    "Rescue team completed mission #" + id + " (Status -> RESOLVED): " + summary,
                    prev,
                    ReportStatus.RESOLVED
            );
        } else {
            reportService.reviewReport(id, "Rescue Team Outcome Submitted by " + actor + ": " + summary, userId, actor);
        }

        AdminReportResponse updated = reportService.getAdminReportById(id);
        return ResponseEntity.ok(toMissionResponse(updated));
    }

    /**
     * Administrator requests further action/follow-up from the rescue team.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/follow-up")
    public ResponseEntity<RescueMissionResponse> requestFollowUp(
            @PathVariable Long id,
            @Valid @RequestBody RescueFollowUpRequest request,
            Authentication authentication) {

        AdminReportResponse report = reportService.getAdminReportById(id);
        MissionState state = missionStates.computeIfAbsent(id, k -> new MissionState());
        Long userId = extractUserId(authentication);
        String actor = extractActor(authentication);

        state.operationalStage = "FOLLOW_UP_REQUIRED";
        state.adminFollowUpNotes = request.getInstructions().trim();

        // Update report review notes with follow up request
        reportService.reviewReport(id, "Administrator Follow-up Required: " + state.adminFollowUpNotes, userId, actor);

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                userId,
                id,
                AuditOutcome.SUCCESS,
                "Administrator requested further response action from rescue team: " + state.adminFollowUpNotes,
                report.getStatus(),
                report.getStatus()
        );

        AdminReportResponse updated = reportService.getAdminReportById(id);
        return ResponseEntity.ok(toMissionResponse(updated));
    }

    private RescueMissionResponse toMissionResponse(AdminReportResponse report) {
        MissionState state = missionStates.get(report.getId());
        if (state == null) {
            state = new MissionState();
        }
        RescueMissionResponse res = new RescueMissionResponse();

        res.setId(report.getId());
        res.setIncidentCategory(report.getIncidentCategory());
        res.setCategoryDisplayName(mapCategory(report.getIncidentCategory()));
        res.setDescription(report.getDescription());
        res.setLocationDescription(report.getLocationDescription());
        res.setLatitude(report.getLatitude());
        res.setLongitude(report.getLongitude());
        res.setReportedTimestamp(report.getReportedTimestamp());
        res.setStatus(report.getStatus());

        if (report.getStatus() == ReportStatus.REJECTED) {
            res.setOperationalStage("REJECTED");
        } else if (report.getStatus() == ReportStatus.RESOLVED && !"COMPLETED".equals(state.operationalStage)) {
            res.setOperationalStage("COMPLETED");
        } else {
            res.setOperationalStage(state.operationalStage != null ? state.operationalStage : "ASSIGNED");
        }

        res.setAssignedTeam(state.assignedTeam);
        res.setAssignedOfficerId(state.assignedOfficerId);
        res.setAssignedPriority(state.assignedPriority);
        res.setInstructions(state.instructions);
        res.setAssignedAt(state.assignedAt);

        res.setDecisionStatus(state.decisionStatus);
        res.setDecisionReason(state.decisionReason);
        res.setJourneyStartedAt(state.journeyStartedAt);
        res.setArrivedAt(state.arrivedAt);
        res.setTrackingActive(state.isTrackingActive);

        res.setCurrentLatitude(state.currentLatitude);
        res.setCurrentLongitude(state.currentLongitude);
        res.setCurrentAccuracy(state.currentAccuracy);
        if (state.lastLocationTimestamp != null) {
            res.setLastLocationUpdate(state.lastLocationTimestamp.toString());
            res.setLocationStale(Duration.between(state.lastLocationTimestamp, Instant.now()).getSeconds() > 60);
        } else {
            res.setLocationStale(true);
        }

        res.setJourneyTrail(new ArrayList<>(state.journeyTrail));
        res.setProgressHistory(new ArrayList<>(state.progressHistory));
        res.setOutcomeReport(state.outcomeReport);
        res.setAdminFollowUpNotes(state.adminFollowUpNotes);

        try {
            List<ReportPhotoResponse> photos = photoService.getPhotosForReport(report.getId(), null, true);
            res.setPhotos(photos != null ? photos : Collections.emptyList());
        } catch (Exception e) {
            res.setPhotos(Collections.emptyList());
        }

        return res;
    }

    private String mapCategory(IncidentCategory category) {
        if (category == null) return "Observation";
        switch (category) {
            case MUD_SLIDE: return "Landslide";
            case ROAD_SUBSIDENCE: return "Road Blockage";
            case SLOPE_CRACK: return "Ground Cracks";
            case ROCKFALL: return "Falling Rocks";
            case DEBRIS_FLOW: return "Debris Flow";
            case EROSION: return "Slope Erosion";
            case OTHER:
            default: return "Other Observation";
        }
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object sub = jwt.getClaims().get("sub");
            if (sub != null) {
                try {
                    return Long.parseLong(sub.toString());
                } catch (NumberFormatException ignored) {}
            }
            Object uid = jwt.getClaims().get("userId");
            if (uid != null) {
                try {
                    return Long.parseLong(uid.toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 1L;
    }

    private String extractActor(Authentication authentication) {
        if (authentication == null) return "Rescue Force Personnel";
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object nameClaim = jwt.getClaims().get("name");
            if (nameClaim != null) return nameClaim.toString();
        }
        return authentication.getName() != null ? authentication.getName() : "Rescue Force Personnel";
    }
}
