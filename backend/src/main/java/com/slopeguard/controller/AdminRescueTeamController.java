package com.slopeguard.controller;

import com.slopeguard.dto.*;
import com.slopeguard.service.RescueTeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller providing administrative endpoints for:
 * - Rescue team verification and approval workflow (PENDING -> APPROVED / REJECTED)
 * - Operational status activation/deactivation
 * - Haversine GPS nearest-team identification for incidents
 * - Incident mission assignment and tracking
 */
@RestController
@RequestMapping("/api/admin/rescue-teams")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRescueTeamController {

    private final RescueTeamService rescueTeamService;

    public AdminRescueTeamController(RescueTeamService rescueTeamService) {
        this.rescueTeamService = rescueTeamService;
    }

    /**
     * Lists all registered rescue teams.
     */
    @GetMapping
    public ResponseEntity<List<RescueTeamResponse>> getAllTeams() {
        return ResponseEntity.ok(rescueTeamService.getAllTeams());
    }

    /**
     * Retrieves full details for a specific rescue team.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RescueTeamResponse> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(rescueTeamService.getTeamById(id));
    }

    /**
     * Approves a pending rescue team registration and activates the unit.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<RescueTeamResponse> approveTeam(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long adminId = extractUserId(authentication);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(rescueTeamService.approveTeam(id, adminId, notes));
    }

    /**
     * Rejects a rescue team registration with a documented reason.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<RescueTeamResponse> rejectTeam(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long adminId = extractUserId(authentication);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(rescueTeamService.rejectTeam(id, adminId, reason));
    }

    /**
     * Activates an approved rescue team.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<RescueTeamResponse> activateTeam(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = extractUserId(authentication);
        return ResponseEntity.ok(rescueTeamService.activateTeam(id, adminId));
    }

    /**
     * Deactivates a rescue team.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<RescueTeamResponse> deactivateTeam(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = extractUserId(authentication);
        return ResponseEntity.ok(rescueTeamService.deactivateTeam(id, adminId));
    }

    /**
     * Updates operational status (ACTIVE / INACTIVE).
     */
    @PostMapping("/{id}/status")
    public ResponseEntity<RescueTeamResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long adminId = extractUserId(authentication);
        String status = body != null ? body.get("operationalStatus") : null;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(rescueTeamService.activateTeam(id, adminId));
        } else {
            return ResponseEntity.ok(rescueTeamService.deactivateTeam(id, adminId));
        }
    }

    /**
     * Computes Haversine distance and identifies nearest eligible Rescue Teams for an incident.
     */
    @GetMapping("/nearest")
    public ResponseEntity<List<NearestRescueTeamResponse>> findNearestTeams(
            @RequestParam("reportId") Long reportId,
            @RequestParam(value = "capability", required = false) String capability) {
        return ResponseEntity.ok(rescueTeamService.findNearestTeamsForIncident(reportId, capability));
    }

    /**
     * Dispatches an incident report to a selected Rescue Team.
     */
    @PostMapping("/assign")
    public ResponseEntity<RescueAssignmentResponse> assignTeam(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Long adminId = extractUserId(authentication);
        Long reportId = Long.valueOf(body.get("reportId").toString());
        Long teamId = Long.valueOf(body.get("teamId").toString());
        String priority = body.get("priority") != null ? body.get("priority").toString() : "HIGH";
        String instructions = body.get("instructions") != null ? body.get("instructions").toString() : null;

        return ResponseEntity.ok(rescueTeamService.assignTeamToIncident(reportId, teamId, priority, instructions, adminId));
    }

    /**
     * Lists all rescue assignments for DMO tracking.
     */
    @GetMapping("/assignments")
    public ResponseEntity<List<RescueAssignmentResponse>> getAllAssignments() {
        return ResponseEntity.ok(rescueTeamService.getAllAssignments());
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        return 1L; // Fallback to default admin
    }
}
