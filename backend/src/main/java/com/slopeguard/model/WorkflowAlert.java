package com.slopeguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity representing an early-warning advisory in the operational workflow.
 * Workflow states: DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED -> RESOLVED / WITHDRAWN.
 * Owned by Module 1 (Ragul).
 */
@Entity
@Table(name = "workflow_alerts")
public class WorkflowAlert {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "area_name", nullable = false, length = 255)
    private String areaName;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "assessment_id", length = 64)
    private String assessmentId;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType = "REVIEWED_PLATFORM";

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "DRAFT";

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public WorkflowAlert() {
        this.createdAt = LocalDateTime.now();
    }

    public WorkflowAlert(String id, String title, String areaName, Long areaId, String assessmentId,
                         String severity, String description, String recommendedActions,
                         String sourceType, String referenceUrl, String status, Long createdBy) {
        this.id = id;
        this.title = title;
        this.areaName = areaName;
        this.areaId = areaId;
        this.assessmentId = assessmentId;
        this.severity = severity;
        this.description = description;
        this.recommendedActions = recommendedActions;
        this.sourceType = sourceType != null ? sourceType : "REVIEWED_PLATFORM";
        this.referenceUrl = referenceUrl;
        this.status = status != null ? status : "DRAFT";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.version = 0;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
