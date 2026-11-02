package com.slopeguard.dto;

import java.time.LocalDateTime;

/**
 * Summary DTO of a rescue mission assignment embedded within incident report responses
 * to provide operational transparency to citizens and administrators.
 */
public class RescueAssignmentSummaryDto {

    private Long id;
    private Long teamId;
    private String teamName;
    private String contactNumber;
    private String priority;
    private String instructions;
    private String status; // ASSIGNED, ACCEPTED, EN_ROUTE, ON_SCENE, COMPLETED, DECLINED, REVOKED
    private Double distanceKm;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime enRouteAt;
    private LocalDateTime onSceneAt;
    private LocalDateTime completedAt;
    private String responseNotes;

    public RescueAssignmentSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getEnRouteAt() {
        return enRouteAt;
    }

    public void setEnRouteAt(LocalDateTime enRouteAt) {
        this.enRouteAt = enRouteAt;
    }

    public LocalDateTime getOnSceneAt() {
        return onSceneAt;
    }

    public void setOnSceneAt(LocalDateTime onSceneAt) {
        this.onSceneAt = onSceneAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getResponseNotes() {
        return responseNotes;
    }

    public void setResponseNotes(String responseNotes) {
        this.responseNotes = responseNotes;
    }
}

