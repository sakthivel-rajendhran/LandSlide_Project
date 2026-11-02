package com.slopeguard.service;

import com.slopeguard.dto.*;
import com.slopeguard.exception.DuplicateEmailException;
import com.slopeguard.exception.ResourceNotFoundException;
import com.slopeguard.model.*;
import com.slopeguard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating Rescue Team lifecycle:
 * - Public self-registration (defaults to PENDING / INACTIVE)
 * - Administrative approval, rejection, activation, and deactivation
 * - Haversine distance calculation and nearest-team identification
 * - Incident mission assignment and team response workflow
 */
@Service
public class RescueTeamService {

    private static final Logger log = LoggerFactory.getLogger(RescueTeamService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final RescueTeamRepository rescueTeamRepository;
    private final RescueAssignmentRepository rescueAssignmentRepository;
    private final RescueTeamVerificationRepository rescueTeamVerificationRepository;
    private final UserRepository userRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ReportStatusAuditLogRepository statusAuditLogRepository;

    public RescueTeamService(
            RescueTeamRepository rescueTeamRepository,
            RescueAssignmentRepository rescueAssignmentRepository,
            RescueTeamVerificationRepository rescueTeamVerificationRepository,
            UserRepository userRepository,
            IncidentReportRepository incidentReportRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ReportStatusAuditLogRepository statusAuditLogRepository) {
        this.rescueTeamRepository = rescueTeamRepository;
        this.rescueAssignmentRepository = rescueAssignmentRepository;
        this.rescueTeamVerificationRepository = rescueTeamVerificationRepository;
        this.userRepository = userRepository;
        this.incidentReportRepository = incidentReportRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.statusAuditLogRepository = statusAuditLogRepository;
    }

    // =========================================================================
    // 1. REGISTRATION WORKFLOW
    // =========================================================================

    @Transactional
    public RescueTeamResponse registerRescueTeam(RescueTeamRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String trimmedName = request.getTeamName().trim();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("An account with email '" + normalizedEmail + "' already exists.");
        }

        if (rescueTeamRepository.existsByTeamNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("A rescue team with name '" + trimmedName + "' is already registered.");
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());

        // 1. Create underlying User account with RESCUE_FORCE role
        User user = new User();
        user.setFullName(request.getTeamLeader().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getContactNumber().trim());
        user.setRole(UserRole.RESCUE_FORCE);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // 2. Create RescueTeam profile in PENDING / INACTIVE state
        RescueTeam team = new RescueTeam();
        team.setUser(savedUser);
        team.setTeamName(trimmedName);
        team.setTeamCategory(request.getTeamCategory().trim().toUpperCase(Locale.ROOT));
        team.setTeamLeader(request.getTeamLeader().trim());
        team.setContactNumber(request.getContactNumber().trim());
        team.setTeamSize(request.getTeamSize());
        team.setOperatingDistrict(request.getOperatingDistrict().trim());
        team.setOperatingRegion(request.getOperatingRegion().trim());
        team.setBaseAddress(request.getBaseAddress().trim());
        team.setLatitude(request.getLatitude());
        team.setLongitude(request.getLongitude());
        team.setVerificationStatus("PENDING");
        team.setOperationalStatus("INACTIVE");
        team.setAvailability("INACTIVE");
        team.setCapabilities(new HashSet<>(request.getCapabilities()));

        RescueTeam savedTeam = rescueTeamRepository.save(team);
        log.info("Registered new Rescue Team: {} (ID: {}) awaiting DMO verification", savedTeam.getTeamName(), savedTeam.getId());

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                savedUser.getId(),
                null,
                AuditOutcome.SUCCESS,
                "Rescue team self-registration submitted: " + savedTeam.getTeamName() + " (Pending DMO verification)",
                null,
                null
        );

        return toTeamResponse(savedTeam);
    }

    // =========================================================================
    // 2. ADMIN TEAM MANAGEMENT & APPROVAL WORKFLOW
    // =========================================================================

    @Transactional(readOnly = true)
    public List<RescueTeamResponse> getAllTeams() {
        return rescueTeamRepository.findAll().stream()
                .map(this::toTeamResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RescueTeamResponse getTeamById(Long teamId) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));
        return toTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public RescueTeamResponse getTeamByUserId(Long userId) {
        RescueTeam team = rescueTeamRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No Rescue Team profile associated with user #" + userId));
        return toTeamResponse(team);
    }

    @Transactional
    public RescueTeamResponse approveTeam(Long teamId, Long adminUserId, String reviewNotes) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found."));

        team.setVerificationStatus("APPROVED");
        team.setVerifiedBy(adminUser);
        team.setVerifiedAt(LocalDateTime.now());
        team.setRejectionReason(null);
        // Automatically activate upon approval as convenient standard
        team.setOperationalStatus("ACTIVE");
        team.setAvailability("AVAILABLE");

        RescueTeam saved = rescueTeamRepository.save(team);

        rescueTeamVerificationRepository.save(new RescueTeamVerification(
                saved, adminUser, "APPROVE", reviewNotes != null ? reviewNotes : "Approved by DMO administrator"
        ));

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                adminUserId,
                null,
                AuditOutcome.SUCCESS,
                "Admin approved rescue team: " + saved.getTeamName() + " (Operational Status: ACTIVE)",
                null,
                null
        );

        return toTeamResponse(saved);
    }

    @Transactional
    public RescueTeamResponse rejectTeam(Long teamId, Long adminUserId, String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("A valid rejection reason is required.");
        }

        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found."));

        team.setVerificationStatus("REJECTED");
        team.setOperationalStatus("INACTIVE");
        team.setAvailability("INACTIVE");
        team.setRejectionReason(reason.trim());
        team.setVerifiedBy(adminUser);
        team.setVerifiedAt(LocalDateTime.now());

        RescueTeam saved = rescueTeamRepository.save(team);

        rescueTeamVerificationRepository.save(new RescueTeamVerification(
                saved, adminUser, "REJECT", reason.trim()
        ));

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                adminUserId,
                null,
                AuditOutcome.SUCCESS,
                "Admin rejected rescue team: " + saved.getTeamName() + " (Reason: " + reason.trim() + ")",
                null,
                null
        );

        return toTeamResponse(saved);
    }

    @Transactional
    public RescueTeamResponse activateTeam(Long teamId, Long adminUserId) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found."));

        if (!"APPROVED".equalsIgnoreCase(team.getVerificationStatus())) {
            throw new IllegalArgumentException("Only APPROVED teams can be activated. Current verification status is " + team.getVerificationStatus());
        }

        team.setOperationalStatus("ACTIVE");
        team.setAvailability("AVAILABLE");
        RescueTeam saved = rescueTeamRepository.save(team);

        rescueTeamVerificationRepository.save(new RescueTeamVerification(
                saved, adminUser, "ACTIVATE", "Team activated by DMO"
        ));

        return toTeamResponse(saved);
    }

    @Transactional
    public RescueTeamResponse deactivateTeam(Long teamId, Long adminUserId) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found."));

        team.setOperationalStatus("INACTIVE");
        team.setAvailability("INACTIVE");
        RescueTeam saved = rescueTeamRepository.save(team);

        rescueTeamVerificationRepository.save(new RescueTeamVerification(
                saved, adminUser, "DEACTIVATE", "Team deactivated by DMO"
        ));

        return toTeamResponse(saved);
    }

    // =========================================================================
    // 3. TEAM PROFILE & AVAILABILITY MANAGEMENT
    // =========================================================================

    @Transactional
    public RescueTeamResponse updateTeamProfile(Long teamId, Long authenticatedUserId, RescueTeamUpdateRequest request) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));

        if (!team.getUser().getId().equals(authenticatedUserId)) {
            User user = userRepository.findById(authenticatedUserId).orElse(null);
            if (user == null || user.getRole() != UserRole.ADMIN) {
                throw new AccessDeniedException("You are not authorized to modify this rescue team's profile.");
            }
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());

        team.setTeamLeader(request.getTeamLeader().trim());
        team.setContactNumber(request.getContactNumber().trim());
        team.setTeamSize(request.getTeamSize());
        team.setOperatingDistrict(request.getOperatingDistrict().trim());
        team.setOperatingRegion(request.getOperatingRegion().trim());
        team.setBaseAddress(request.getBaseAddress().trim());
        team.setLatitude(request.getLatitude());
        team.setLongitude(request.getLongitude());
        team.setCapabilities(new HashSet<>(request.getCapabilities()));

        return toTeamResponse(rescueTeamRepository.save(team));
    }

    @Transactional
    public RescueTeamResponse updateAvailability(Long teamId, Long authenticatedUserId, String newAvailability) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));

        if (!team.getUser().getId().equals(authenticatedUserId)) {
            User user = userRepository.findById(authenticatedUserId).orElse(null);
            if (user == null || user.getRole() != UserRole.ADMIN) {
                throw new AccessDeniedException("You are not authorized to update this team's availability.");
            }
        }

        if (!"APPROVED".equalsIgnoreCase(team.getVerificationStatus())) {
            throw new IllegalArgumentException("Unapproved teams cannot change operational availability.");
        }
        if (!"ACTIVE".equalsIgnoreCase(team.getOperationalStatus())) {
            throw new IllegalArgumentException("Inactive teams cannot change operational availability.");
        }

        String target = newAvailability.trim().toUpperCase(Locale.ROOT);
        if (!target.equals("AVAILABLE") && !target.equals("OFF_DUTY") && !target.equals("BUSY")) {
            throw new IllegalArgumentException("Invalid availability status: " + newAvailability);
        }

        team.setAvailability(target);
        return toTeamResponse(rescueTeamRepository.save(team));
    }

    // =========================================================================
    // 4. NEAREST RESCUE TEAM IDENTIFICATION (HAVERSINE ALGORITHM)
    // =========================================================================

    /**
     * Calculates the authoritative Haversine straight-line distance in kilometers.
     * Formula:
     * a = sin²(Δφ/2) + cos(φ1) × cos(φ2) × sin²(Δλ/2)
     * c = 2 × atan2(√a, √(1−a))
     * Distance = R × c (where R = 6371.0 km)
     */
    public double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distance = EARTH_RADIUS_KM * c;

        // Round to 1 decimal place (e.g. 4.2 km)
        return Math.round(distance * 10.0) / 10.0;
    }

    @Transactional(readOnly = true)
    public List<NearestRescueTeamResponse> findNearestTeamsForIncident(Long reportId, String requiredCapability) {
        IncidentReport report = incidentReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident Report #" + reportId + " not found."));

        Double incLat = report.getLatitude();
        Double incLng = report.getLongitude();

        if (incLat == null || incLng == null) {
            throw new IllegalArgumentException("Incident #" + reportId + " does not have valid GPS coordinates.");
        }
        validateCoordinates(incLat, incLng);

        List<RescueTeam> allTeams = rescueTeamRepository.findAll();
        List<NearestRescueTeamResponse> results = new ArrayList<>();

        for (RescueTeam team : allTeams) {
            if (team.getLatitude() == null || team.getLongitude() == null) {
                continue;
            }

            double distanceKm = calculateHaversineDistanceKm(incLat, incLng, team.getLatitude(), team.getLongitude());
            long activeMissions = rescueAssignmentRepository.countActiveAssignmentsForTeam(team.getId());

            NearestRescueTeamResponse dto = new NearestRescueTeamResponse();
            dto.setTeamId(team.getId());
            dto.setTeamName(team.getTeamName());
            dto.setTeamCategory(team.getTeamCategory());
            dto.setTeamLeader(team.getTeamLeader());
            dto.setContactNumber(team.getContactNumber());
            dto.setBaseAddress(team.getBaseAddress());
            dto.setOperatingDistrict(team.getOperatingDistrict());
            dto.setOperatingRegion(team.getOperatingRegion());
            dto.setLatitude(team.getLatitude());
            dto.setLongitude(team.getLongitude());
            dto.setDistanceKm(distanceKm);
            dto.setCapabilities(team.getCapabilities());
            dto.setActiveMissionsCount(activeMissions);

            // Determine effective availability and eligibility
            boolean isApproved = "APPROVED".equalsIgnoreCase(team.getVerificationStatus());
            boolean isActive = "ACTIVE".equalsIgnoreCase(team.getOperationalStatus());
            boolean isAvailable = "AVAILABLE".equalsIgnoreCase(team.getAvailability());

            if (!isApproved) {
                dto.setEligible(false);
                dto.setIneligibilityReason("Team verification pending or rejected (" + team.getVerificationStatus() + ")");
                dto.setAvailability(team.getAvailability());
            } else if (!isActive) {
                dto.setEligible(false);
                dto.setIneligibilityReason("Team is currently deactivated");
                dto.setAvailability("INACTIVE");
            } else if ("OFF_DUTY".equalsIgnoreCase(team.getAvailability())) {
                dto.setEligible(false);
                dto.setIneligibilityReason("Team is currently off-duty");
                dto.setAvailability("OFF_DUTY");
            } else if (activeMissions >= 3) {
                dto.setEligible(false);
                dto.setIneligibilityReason("Team reached maximum mission capacity (" + activeMissions + " active)");
                dto.setAvailability("BUSY");
            } else if (requiredCapability != null && !requiredCapability.isBlank()
                    && !hasCapabilityMatch(team.getCapabilities(), requiredCapability)) {
                dto.setEligible(false);
                dto.setIneligibilityReason("Lacks required capability: " + requiredCapability);
                dto.setAvailability(activeMissions > 0 ? "BUSY" : "AVAILABLE");
            } else {
                dto.setEligible(true);
                dto.setIneligibilityReason(null);
                dto.setAvailability(activeMissions > 0 ? "BUSY" : "AVAILABLE");
            }

            results.add(dto);
        }

        // Sort: eligible teams first, ordered by distance ascending
        results.sort(Comparator.comparing(NearestRescueTeamResponse::isEligible, Comparator.reverseOrder())
                .thenComparing(NearestRescueTeamResponse::getDistanceKm));

        return results;
    }

    private boolean hasCapabilityMatch(Set<String> capabilities, String target) {
        if (capabilities == null || capabilities.isEmpty()) return false;
        String cleanTarget = target.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (String cap : capabilities) {
            String cleanCap = cap.trim().toUpperCase().replace("-", "_").replace(" ", "_");
            if (cleanCap.equals(cleanTarget) || cleanCap.contains(cleanTarget) || cleanTarget.contains(cleanCap)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // 5. RESCUE ASSIGNMENT WORKFLOW (PERSISTED IN MYSQL)
    // =========================================================================

    @Transactional
    public RescueAssignmentResponse assignTeamToIncident(Long reportId, Long teamId, String priority, String instructions, Long adminUserId) {
        IncidentReport report = incidentReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident Report #" + reportId + " not found."));
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Team #" + teamId + " not found."));
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found."));

        if (!"APPROVED".equalsIgnoreCase(team.getVerificationStatus())) {
            throw new IllegalArgumentException("Cannot assign mission to an unapproved rescue team.");
        }
        if (!"ACTIVE".equalsIgnoreCase(team.getOperationalStatus())) {
            throw new IllegalArgumentException("Cannot assign mission to an inactive rescue team.");
        }

        // Check for duplicate active assignment
        Optional<RescueAssignment> activeAssignment = rescueAssignmentRepository.findActiveAssignmentForReport(reportId);
        if (activeAssignment.isPresent()) {
            throw new IllegalArgumentException("Incident #" + reportId + " is already actively assigned to team '"
                    + activeAssignment.get().getRescueTeam().getTeamName() + "' (Status: "
                    + activeAssignment.get().getAssignmentStatus() + ")");
        }

        Double distanceKm = null;
        if (report.getLatitude() != null && report.getLongitude() != null &&
                team.getLatitude() != null && team.getLongitude() != null) {
            distanceKm = calculateHaversineDistanceKm(report.getLatitude(), report.getLongitude(), team.getLatitude(), team.getLongitude());
        }

        RescueAssignment assignment = new RescueAssignment();
        assignment.setIncidentReport(report);
        assignment.setRescueTeam(team);
        assignment.setAssignedBy(adminUser);
        assignment.setPriority(priority != null ? priority.trim().toUpperCase() : "HIGH");
        assignment.setInstructions(instructions != null ? instructions.trim() : "Verify slope conditions, evacuate endangered citizens, and clear debris.");
        assignment.setAssignmentStatus("ASSIGNED");
        assignment.setDistanceKm(distanceKm);
        assignment.setAssignedAt(LocalDateTime.now());

        RescueAssignment saved = rescueAssignmentRepository.save(assignment);

        // Update incident report status to UNDER_REVIEW if currently PENDING
        if (report.getStatus() == ReportStatus.PENDING) {
            report.setStatus(ReportStatus.UNDER_REVIEW);
            report.setStatusUpdatedAt(LocalDateTime.now());
            report.setReviewNotes("Assigned to " + team.getTeamName() + " (Distance: " + (distanceKm != null ? distanceKm + " km" : "N/A") + ")");
            incidentReportRepository.save(report);
        }

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                adminUserId,
                reportId,
                AuditOutcome.SUCCESS,
                "Admin dispatched Rescue Team '" + team.getTeamName() + "' to Incident #" + reportId + " (" + (distanceKm != null ? distanceKm + " km" : "N/A") + ")",
                report.getStatus(),
                report.getStatus()
        );

        return toAssignmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RescueAssignmentResponse> getAllAssignments() {
        return rescueAssignmentRepository.findAllOrdered().stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RescueAssignmentResponse> getAssignmentsForTeamUser(Long userId) {
        RescueTeam team = rescueTeamRepository.findByUserId(userId).orElse(null);
        if (team == null) {
            return Collections.emptyList();
        }
        return rescueAssignmentRepository.findAllByRescueTeamIdOrderByAssignedAtDesc(team.getId()).stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RescueAssignmentResponse acceptAssignment(Long assignmentId, Long userId) {
        RescueAssignment assignment = getAuthorizedAssignment(assignmentId, userId);

        if (!"ASSIGNED".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new IllegalArgumentException("Assignment cannot be accepted from current status: " + assignment.getAssignmentStatus());
        }

        assignment.setAssignmentStatus("ACCEPTED");
        assignment.setAcceptedAt(LocalDateTime.now());
        RescueAssignment saved = rescueAssignmentRepository.save(assignment);

        IncidentReport report = saved.getIncidentReport();
        if (report != null && report.getStatus() == ReportStatus.PENDING) {
            report.setStatus(ReportStatus.UNDER_REVIEW);
            report.setStatusUpdatedAt(LocalDateTime.now());
            incidentReportRepository.save(report);
        }

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                userId,
                saved.getIncidentReport().getId(),
                AuditOutcome.SUCCESS,
                "Rescue Team '" + saved.getRescueTeam().getTeamName() + "' accepted assignment #" + assignmentId,
                null,
                null
        );

        return toAssignmentResponse(saved);
    }

    @Transactional
    public RescueAssignmentResponse declineAssignment(Long assignmentId, Long userId, String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("A reason is required to decline an assignment.");
        }

        RescueAssignment assignment = getAuthorizedAssignment(assignmentId, userId);

        if ("COMPLETED".equalsIgnoreCase(assignment.getAssignmentStatus()) || "DECLINED".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new IllegalArgumentException("Cannot decline assignment with status: " + assignment.getAssignmentStatus());
        }

        assignment.setAssignmentStatus("DECLINED");
        assignment.setDeclinedAt(LocalDateTime.now());
        assignment.setDeclineReason(reason.trim());
        RescueAssignment saved = rescueAssignmentRepository.save(assignment);

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                userId,
                saved.getIncidentReport().getId(),
                AuditOutcome.SUCCESS,
                "Rescue Team '" + saved.getRescueTeam().getTeamName() + "' declined assignment #" + assignmentId + " (Reason: " + reason.trim() + ")",
                null,
                null
        );

        return toAssignmentResponse(saved);
    }

    @Transactional
    public RescueAssignmentResponse updateAssignmentStatus(Long assignmentId, Long userId, String newStatus) {
        RescueAssignment assignment = getAuthorizedAssignment(assignmentId, userId);
        if ("COMPLETED".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new IllegalStateException("Cannot update status of an already COMPLETED assignment.");
        }
        String target = newStatus.trim().toUpperCase(Locale.ROOT);
        IncidentReport report = assignment.getIncidentReport();

        if ("EN_ROUTE".equals(target)) {
            assignment.setAssignmentStatus("EN_ROUTE");
            if (assignment.getEnRouteAt() == null) assignment.setEnRouteAt(LocalDateTime.now());
            if (report != null && report.getStatus() != ReportStatus.RESOLVED) {
                report.setStatus(ReportStatus.RESPONDING);
                report.setStatusUpdatedAt(LocalDateTime.now());
                incidentReportRepository.save(report);
            }
        } else if ("ON_SCENE".equals(target)) {
            assignment.setAssignmentStatus("ON_SCENE");
            if (assignment.getOnSceneAt() == null) assignment.setOnSceneAt(LocalDateTime.now());
            if (report != null && report.getStatus() != ReportStatus.RESOLVED) {
                report.setStatus(ReportStatus.RESPONDING);
                report.setStatusUpdatedAt(LocalDateTime.now());
                incidentReportRepository.save(report);
            }
        } else {
            throw new IllegalArgumentException("Unsupported status transition: " + newStatus);
        }

        RescueAssignment saved = rescueAssignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    @Transactional
    public RescueAssignmentResponse completeAssignment(Long assignmentId, Long userId, String responseNotes) {
        if (responseNotes == null || responseNotes.trim().isBlank()) {
            throw new IllegalArgumentException("Completion remarks / response notes are mandatory.");
        }

        RescueAssignment assignment = getAuthorizedAssignment(assignmentId, userId);

        if ("COMPLETED".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new IllegalStateException("Assignment #" + assignmentId + " is already marked as COMPLETED.");
        }
        if ("DECLINED".equalsIgnoreCase(assignment.getAssignmentStatus()) || "REVOKED".equalsIgnoreCase(assignment.getAssignmentStatus())) {
            throw new IllegalStateException("Cannot complete assignment with status: " + assignment.getAssignmentStatus());
        }

        String notes = responseNotes.trim();
        assignment.setAssignmentStatus("COMPLETED");
        assignment.setCompletedAt(LocalDateTime.now());
        assignment.setResponseNotes(notes);

        RescueAssignment saved = rescueAssignmentRepository.save(assignment);

        // Update incident report status to RESOLVED with completion notes
        IncidentReport report = saved.getIncidentReport();
        if (report != null) {
            ReportStatus prevStatus = report.getStatus();
            report.setStatus(ReportStatus.RESOLVED);
            report.setStatusUpdatedAt(LocalDateTime.now());
            report.setUpdatedAt(LocalDateTime.now());
            String teamName = (saved.getRescueTeam() != null) ? saved.getRescueTeam().getTeamName() : "Rescue Unit";
            report.setReviewNotes("Rescue operation completed by " + teamName + ". Remarks: " + notes);
            incidentReportRepository.save(report);

            try {
                if (statusAuditLogRepository != null) {
                    ReportStatusAuditLog legacyLog = new ReportStatusAuditLog(
                            null,
                            report.getId(),
                            prevStatus,
                            ReportStatus.RESOLVED,
                            userId,
                            teamName,
                            "Rescue mission completed: " + notes
                    );
                    statusAuditLogRepository.save(legacyLog);
                }
            } catch (Exception ignored) {}

            auditService.recordEvent(
                    AuditEventType.REPORT_STATUS_CHANGED,
                    userId,
                    report.getId(),
                    AuditOutcome.SUCCESS,
                    "Rescue Team '" + teamName + "' marked mission #" + assignmentId + " COMPLETED (Status -> RESOLVED)",
                    prevStatus,
                    ReportStatus.RESOLVED
            );
        }

        return toAssignmentResponse(saved);
    }

    private RescueAssignment getAuthorizedAssignment(Long assignmentId, Long userId) {
        RescueAssignment assignment = rescueAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue Assignment #" + assignmentId + " not found."));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new AccessDeniedException("User account not recognized.");
        }

        if (user.getRole() == UserRole.ADMIN) {
            return assignment;
        }

        if (!assignment.getRescueTeam().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("This mission is not assigned to your rescue unit.");
        }

        return assignment;
    }

    // =========================================================================
    // 6. HELPER MAPPERS & VALIDATORS
    // =========================================================================

    public void validateCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Coordinates cannot be null.");
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude " + lat + " is out of bounds [-90.0, 90.0].");
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Longitude " + lon + " is out of bounds [-180.0, 180.0].");
        }
    }

    public RescueTeamResponse toTeamResponse(RescueTeam t) {
        RescueTeamResponse r = new RescueTeamResponse();
        r.setId(t.getId());
        r.setUserId(t.getUser().getId());
        r.setEmail(t.getUser().getEmail());
        r.setTeamName(t.getTeamName());
        r.setTeamCategory(t.getTeamCategory());
        r.setTeamLeader(t.getTeamLeader());
        r.setContactNumber(t.getContactNumber());
        r.setTeamSize(t.getTeamSize());
        r.setOperatingDistrict(t.getOperatingDistrict());
        r.setOperatingRegion(t.getOperatingRegion());
        r.setBaseAddress(t.getBaseAddress());
        r.setLatitude(t.getLatitude());
        r.setLongitude(t.getLongitude());
        r.setVerificationStatus(t.getVerificationStatus());
        r.setOperationalStatus(t.getOperationalStatus());
        r.setAvailability(t.getAvailability());
        r.setRejectionReason(t.getRejectionReason());
        r.setVerifiedByName(t.getVerifiedBy() != null ? t.getVerifiedBy().getFullName() : null);
        r.setVerifiedAt(t.getVerifiedAt());
        r.setCreatedAt(t.getCreatedAt());
        r.setCapabilities(t.getCapabilities() != null ? new HashSet<>(t.getCapabilities()) : Collections.emptySet());

        long activeCount = rescueAssignmentRepository.countActiveAssignmentsForTeam(t.getId());
        r.setActiveMissionsCount(activeCount);

        return r;
    }

    public RescueAssignmentResponse toAssignmentResponse(RescueAssignment ra) {
        RescueAssignmentResponse r = new RescueAssignmentResponse();
        r.setId(ra.getId());

        IncidentReport rep = ra.getIncidentReport();
        if (rep != null) {
            r.setIncidentReportId(rep.getId());
            r.setIncidentCategory(rep.getIncidentCategory() != null ? rep.getIncidentCategory().name() : null);
            r.setIncidentLocation(rep.getLocationDescription());
            r.setLatitude(rep.getLatitude());
            r.setLongitude(rep.getLongitude());
            r.setSeverity(rep.getStatus() != null ? rep.getStatus().name() : null);
            r.setIncidentDescription(rep.getDescription());
        }

        RescueTeam tm = ra.getRescueTeam();
        if (tm != null) {
            r.setRescueTeamId(tm.getId());
            r.setRescueTeamName(tm.getTeamName());
            r.setTeamCategory(tm.getTeamCategory());
            r.setTeamLeader(tm.getTeamLeader());
            r.setTeamContact(tm.getContactNumber());
        }

        r.setAssignedByName(ra.getAssignedBy() != null ? ra.getAssignedBy().getFullName() : null);
        r.setPriority(ra.getPriority());
        r.setInstructions(ra.getInstructions());
        r.setAssignmentStatus(ra.getAssignmentStatus());
        r.setDistanceKm(ra.getDistanceKm());
        r.setAssignedAt(ra.getAssignedAt());
        r.setAcceptedAt(ra.getAcceptedAt());
        r.setDeclinedAt(ra.getDeclinedAt());
        r.setDeclineReason(ra.getDeclineReason());
        r.setEnRouteAt(ra.getEnRouteAt());
        r.setOnSceneAt(ra.getOnSceneAt());
        r.setCompletedAt(ra.getCompletedAt());
        r.setResponseNotes(ra.getResponseNotes());

        return r;
    }
}
