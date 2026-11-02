package com.slopeguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity representing an administrative dispatch of an incident report
 * to an eligible registered Rescue Team.
 */
@Entity
@Table(name = "rescue_assignments")
public class RescueAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_report_id", nullable = false)
    private IncidentReport incidentReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_team_id", nullable = false)
    private RescueTeam rescueTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    @Column(name = "priority", nullable = false, length = 30)
    private String priority = "HIGH";

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "assignment_status", nullable = false, length = 30)
    private String assignmentStatus = "ASSIGNED"; // ASSIGNED, ACCEPTED, DECLINED, EN_ROUTE, ON_SCENE, COMPLETED, REVOKED

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "en_route_at")
    private LocalDateTime enRouteAt;

    @Column(name = "on_scene_at")
    private LocalDateTime onSceneAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RescueAssignment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.assignedAt = LocalDateTime.now();
        this.assignmentStatus = "ASSIGNED";
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public IncidentReport getIncidentReport() { return incidentReport; }
    public void setIncidentReport(IncidentReport incidentReport) { this.incidentReport = incidentReport; }

    public RescueTeam getRescueTeam() { return rescueTeam; }
    public void setRescueTeam(RescueTeam rescueTeam) { this.rescueTeam = rescueTeam; }

    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getDeclinedAt() { return declinedAt; }
    public void setDeclinedAt(LocalDateTime declinedAt) { this.declinedAt = declinedAt; }

    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }

    public LocalDateTime getEnRouteAt() { return enRouteAt; }
    public void setEnRouteAt(LocalDateTime enRouteAt) { this.enRouteAt = enRouteAt; }

    public LocalDateTime getOnSceneAt() { return onSceneAt; }
    public void setOnSceneAt(LocalDateTime onSceneAt) { this.onSceneAt = onSceneAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getResponseNotes() { return responseNotes; }
    public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
