package com.slopeguard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class WorkflowAlertResponse {

    private String id;
    private String title;

    @JsonProperty("area_name")
    private String areaName;

    @JsonProperty("area_id")
    private Long areaId;

    @JsonProperty("assessment_id")
    private String assessmentId;

    private String severity;
    private String description;

    @JsonProperty("recommended_actions")
    private String recommendedActions;

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("reference_url")
    private String referenceUrl;

    private String status;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;

    public WorkflowAlertResponse() {}

    public WorkflowAlertResponse(String id, String title, String areaName, Long areaId, String assessmentId,
                                 String severity, String description, String recommendedActions,
                                 String sourceType, String referenceUrl, String status, Long createdBy,
                                 LocalDateTime createdAt, LocalDateTime publishedAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.title = title;
        this.areaName = areaName;
        this.areaId = areaId;
        this.assessmentId = assessmentId;
        this.severity = severity;
        this.description = description;
        this.recommendedActions = recommendedActions;
        this.sourceType = sourceType;
        this.referenceUrl = referenceUrl;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.resolvedAt = resolvedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(String recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
