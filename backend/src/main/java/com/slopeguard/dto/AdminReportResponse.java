package com.slopeguard.dto;

import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.ReportStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrative view DTO for incident reports.
 * Includes reporter identification, audit review trail, and photo attachments.
 * <p>
 * Security Rule: Strictly restricted to users possessing ROLE_ADMIN.
 */
public class AdminReportResponse {

    private Long id;
    private Long reporterUserId;
    private String reporterName;
    private IncidentCategory incidentCategory;
    private String description;
    private String locationDescription;
    private Double latitude;
    private Double longitude;
    private ReportStatus status;
    private LocalDateTime reportedTimestamp;
    private LocalDateTime observationTimestamp;
    private String environmentalObservations;
    private LocalDateTime updatedAt;
    private String reviewNotes;
    private List<ReportPhotoResponse> photos = new ArrayList<>();

    // Rescue operation tracking fields for operational transparency
    private String rescueStatus;
    private String rescueTeamName;
    private LocalDateTime rescueAssignedAt;
    private LocalDateTime rescueAcceptedAt;
    private LocalDateTime rescueEnRouteAt;
    private LocalDateTime rescueOnSceneAt;
    private LocalDateTime rescueCompletedAt;
    private String rescueNotes;
    private RescueAssignmentSummaryDto rescueAssignment;

    public AdminReportResponse() {
    }

    public AdminReportResponse(Long id, String reporterName, IncidentCategory incidentCategory,
                               String description, String locationDescription, Double latitude, Double longitude,
                               ReportStatus status, LocalDateTime reportedTimestamp,
                               LocalDateTime updatedAt, String reviewNotes) {
        this(id, null, reporterName, incidentCategory, description, locationDescription, latitude, longitude,
                status, reportedTimestamp, null, null, updatedAt, reviewNotes, new ArrayList<>());
    }

    public AdminReportResponse(Long id, Long reporterUserId, String reporterName, IncidentCategory incidentCategory,
                               String description, String locationDescription, Double latitude, Double longitude,
                               ReportStatus status, LocalDateTime reportedTimestamp,
                               LocalDateTime observationTimestamp, String environmentalObservations,
                               LocalDateTime updatedAt, String reviewNotes, List<ReportPhotoResponse> photos) {
        this.id = id;
        this.reporterUserId = reporterUserId;
        this.reporterName = reporterName;
        this.incidentCategory = incidentCategory;
        this.description = description;
        this.locationDescription = locationDescription;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.reportedTimestamp = reportedTimestamp;
        this.observationTimestamp = observationTimestamp;
        this.environmentalObservations = environmentalObservations;
        this.updatedAt = updatedAt;
        this.reviewNotes = reviewNotes;
        this.photos = photos != null ? photos : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public IncidentCategory getIncidentCategory() {
        return incidentCategory;
    }

    public void setIncidentCategory(IncidentCategory incidentCategory) {
        this.incidentCategory = incidentCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public LocalDateTime getReportedTimestamp() {
        return reportedTimestamp;
    }

    public void setReportedTimestamp(LocalDateTime reportedTimestamp) {
        this.reportedTimestamp = reportedTimestamp;
    }

    public LocalDateTime getObservationTimestamp() {
        return observationTimestamp;
    }

    public void setObservationTimestamp(LocalDateTime observationTimestamp) {
        this.observationTimestamp = observationTimestamp;
    }

    public String getEnvironmentalObservations() {
        return environmentalObservations;
    }

    public void setEnvironmentalObservations(String environmentalObservations) {
        this.environmentalObservations = environmentalObservations;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public List<ReportPhotoResponse> getPhotos() {
        return photos;
    }

    public void setPhotos(List<ReportPhotoResponse> photos) {
        this.photos = photos != null ? photos : new ArrayList<>();
    }

    public String getRescueStatus() {
        return rescueStatus;
    }

    public void setRescueStatus(String rescueStatus) {
        this.rescueStatus = rescueStatus;
    }

    public String getRescueTeamName() {
        return rescueTeamName;
    }

    public void setRescueTeamName(String rescueTeamName) {
        this.rescueTeamName = rescueTeamName;
    }

    public LocalDateTime getRescueAssignedAt() {
        return rescueAssignedAt;
    }

    public void setRescueAssignedAt(LocalDateTime rescueAssignedAt) {
        this.rescueAssignedAt = rescueAssignedAt;
    }

    public LocalDateTime getRescueAcceptedAt() {
        return rescueAcceptedAt;
    }

    public void setRescueAcceptedAt(LocalDateTime rescueAcceptedAt) {
        this.rescueAcceptedAt = rescueAcceptedAt;
    }

    public LocalDateTime getRescueEnRouteAt() {
        return rescueEnRouteAt;
    }

    public void setRescueEnRouteAt(LocalDateTime rescueEnRouteAt) {
        this.rescueEnRouteAt = rescueEnRouteAt;
    }

    public LocalDateTime getRescueOnSceneAt() {
        return rescueOnSceneAt;
    }

    public void setRescueOnSceneAt(LocalDateTime rescueOnSceneAt) {
        this.rescueOnSceneAt = rescueOnSceneAt;
    }

    public LocalDateTime getRescueCompletedAt() {
        return rescueCompletedAt;
    }

    public void setRescueCompletedAt(LocalDateTime rescueCompletedAt) {
        this.rescueCompletedAt = rescueCompletedAt;
    }

    public String getRescueNotes() {
        return rescueNotes;
    }

    public void setRescueNotes(String rescueNotes) {
        this.rescueNotes = rescueNotes;
    }

    public RescueAssignmentSummaryDto getRescueAssignment() {
        return rescueAssignment;
    }

    public void setRescueAssignment(RescueAssignmentSummaryDto rescueAssignment) {
        this.rescueAssignment = rescueAssignment;
    }
}
