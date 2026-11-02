package com.slopeguard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PublicAlertResponse {

    private String id;
    private String title;

    @JsonProperty("area_name")
    private String areaName;

    private String severity;
    private String description;

    @JsonProperty("recommended_actions")
    private String recommendedActions;

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("review_status")
    private String reviewStatus;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    public PublicAlertResponse() {}

    public PublicAlertResponse(String id, String title, String areaName, String severity,
                               String description, String recommendedActions, String sourceType,
                               String reviewStatus, LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.areaName = areaName;
        this.severity = severity;
        this.description = description;
        this.recommendedActions = recommendedActions;
        this.sourceType = sourceType;
        this.reviewStatus = reviewStatus;
        this.publishedAt = publishedAt;
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

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
