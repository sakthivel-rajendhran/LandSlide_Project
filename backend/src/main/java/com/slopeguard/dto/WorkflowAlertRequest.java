package com.slopeguard.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkflowAlertRequest {

    @NotBlank(message = "Alert title is required")
    private String title;

    @NotBlank(message = "Affected area name is required")
    private String areaName;

    private Long areaId;

    private String assessmentId;

    @NotBlank(message = "Severity level is required")
    private String severity;

    @NotBlank(message = "Description is required")
    private String description;

    private String recommendedActions;

    private String sourceType = "REVIEWED_PLATFORM";

    private String referenceUrl;

    public WorkflowAlertRequest() {}

    public WorkflowAlertRequest(String title, String areaName, String assessmentId,
                                String severity, String description, String recommendedActions,
                                String sourceType, String referenceUrl) {
        this(title, areaName, null, assessmentId, severity, description, recommendedActions, sourceType, referenceUrl);
    }

    public WorkflowAlertRequest(String title, String areaName, Long areaId, String assessmentId,
                                String severity, String description, String recommendedActions,
                                String sourceType, String referenceUrl) {
        this.title = title;
        this.areaName = areaName;
        this.areaId = areaId;
        this.assessmentId = assessmentId;
        this.severity = severity;
        this.description = description;
        this.recommendedActions = recommendedActions;
        this.sourceType = sourceType != null ? sourceType : "REVIEWED_PLATFORM";
        this.referenceUrl = referenceUrl;
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
}
