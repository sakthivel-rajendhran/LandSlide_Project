package com.slopeguard.dto;

import java.time.LocalDateTime;

/**
 * Representation of a persistent rescue assignment between an incident report and a team.
 */
public class RescueAssignmentResponse {

    private Long id;
    private Long incidentReportId;
    private String incidentCategory;
    private String incidentLocation;
    private Double latitude;
    private Double longitude;
    private String severity;
    private String incidentDescription;

    private Long rescueTeamId;
    private String rescueTeamName;
    private String teamCategory;
    private String teamLeader;
    private String teamContact;

    private String assignedByName;
    private String priority;
    private String instructions;
    private String assignmentStatus;
    private Double distanceKm;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime declinedAt;
    private String declineReason;
    private LocalDateTime enRouteAt;
    private LocalDateTime onSceneAt;
    private LocalDateTime completedAt;
    private String responseNotes;

    public RescueAssignmentResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIncidentReportId() { return incidentReportId; }
    public void setIncidentReportId(Long incidentReportId) { this.incidentReportId = incidentReportId; }

    public String getIncidentCategory() { return incidentCategory; }
    public void setIncidentCategory(String incidentCategory) { this.incidentCategory = incidentCategory; }

    public String getIncidentLocation() { return incidentLocation; }
    public void setIncidentLocation(String incidentLocation) { this.incidentLocation = incidentLocation; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getIncidentDescription() { return incidentDescription; }
    public void setIncidentDescription(String incidentDescription) { this.incidentDescription = incidentDescription; }

    public Long getRescueTeamId() { return rescueTeamId; }
    public void setRescueTeamId(Long rescueTeamId) { this.rescueTeamId = rescueTeamId; }

    public String getRescueTeamName() { return rescueTeamName; }
    public void setRescueTeamName(String rescueTeamName) { this.rescueTeamName = rescueTeamName; }

    public String getTeamCategory() { return teamCategory; }
    public void setTeamCategory(String teamCategory) { this.teamCategory = teamCategory; }

    public String getTeamLeader() { return teamLeader; }
    public void setTeamLeader(String teamLeader) { this.teamLeader = teamLeader; }

    public String getTeamContact() { return teamContact; }
    public void setTeamContact(String teamContact) { this.teamContact = teamContact; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

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
}
