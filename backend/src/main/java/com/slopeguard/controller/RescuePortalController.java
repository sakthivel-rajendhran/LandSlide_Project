package com.slopeguard.controller;

import com.slopeguard.dto.*;
import com.slopeguard.service.RescueTeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller providing operational endpoints for authenticated Rescue Teams:
 * - Profile and settings management
 * - Operational availability toggle (AVAILABLE / OFF_DUTY)
 * - Persistent assigned mission viewing and lifecycle progression
 */
@RestController
@RequestMapping("/api/rescue")
@PreAuthorize("hasAnyRole('RESCUE_FORCE', 'OFFICER', 'ADMIN')")
public class RescuePortalController {

    private final RescueTeamService rescueTeamService;

    public RescuePortalController(RescueTeamService rescueTeamService) {
        this.rescueTeamService = rescueTeamService;
    }

    /**
     * Retrieves the profile and verification status of the authenticated team.
     */
    @GetMapping("/profile")
    public ResponseEntity<RescueTeamResponse> getMyProfile(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(rescueTeamService.getTeamByUserId(userId));
    }

    /**
     * Updates profile details for the authenticated team.
     */
    @PutMapping("/profile")
    public ResponseEntity<RescueTeamResponse> updateMyProfile(
            @Valid @RequestBody RescueTeamUpdateRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        RescueTeamResponse team = rescueTeamService.getTeamByUserId(userId);
        return ResponseEntity.ok(rescueTeamService.updateTeamProfile(team.getId(), userId, request));
    }

    /**
     * Toggles availability status (AVAILABLE / OFF_DUTY) for the authenticated team.
     */
    @PutMapping("/availability")
    public ResponseEntity<RescueTeamResponse> updateAvailability(
            @Valid @RequestBody RescueTeamAvailabilityRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        RescueTeamResponse team = rescueTeamService.getTeamByUserId(userId);
        return ResponseEntity.ok(rescueTeamService.updateAvailability(team.getId(), userId, request.getAvailability()));
    }

    /**
     * Lists persistent mission assignments targeted to the authenticated team.
     * Prevents unapproved teams from accessing operational assignments.
     */
    @GetMapping("/assignments")
    public ResponseEntity<List<RescueAssignmentResponse>> getMyAssignments(Authentication authentication) {
        Long userId = extractUserId(authentication);
        RescueTeamResponse team;
        try {
            team = rescueTeamService.getTeamByUserId(userId);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        if (!"APPROVED".equalsIgnoreCase(team.getVerificationStatus())) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(rescueTeamService.getAssignmentsForTeamUser(userId));
    }

    /**
     * Accepts an assigned rescue mission.
     */
    @PostMapping("/assignments/{id}/accept")
    public ResponseEntity<RescueAssignmentResponse> acceptAssignment(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(rescueTeamService.acceptAssignment(id, userId));
    }

    /**
     * Declines an assigned rescue mission with a stated reason.
     */
    @PostMapping("/assignments/{id}/decline")
    public ResponseEntity<RescueAssignmentResponse> declineAssignment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(rescueTeamService.declineAssignment(id, userId, reason));
    }

    /**
     * Updates mission response progress (EN_ROUTE / ON_SCENE).
     */
    @PostMapping("/assignments/{id}/status")
    public ResponseEntity<RescueAssignmentResponse> updateAssignmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        String status = body != null ? body.get("status") : "EN_ROUTE";
        return ResponseEntity.ok(rescueTeamService.updateAssignmentStatus(id, userId, status));
    }

    /**
     * Completes a mission with field outcome notes.
     */
    @PostMapping("/assignments/{id}/complete")
    public ResponseEntity<RescueAssignmentResponse> completeAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        String notes = body != null ? body.get("responseNotes") : null;
        return ResponseEntity.ok(rescueTeamService.completeAssignment(id, userId, notes));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        throw new AccessDeniedException("Unauthenticated access.");
    }
}
