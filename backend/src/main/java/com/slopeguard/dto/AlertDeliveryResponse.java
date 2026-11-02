package com.slopeguard.dto;

import java.time.LocalDateTime;

public class AlertDeliveryResponse {

    private Long id;
    private String alertId;
    private String channel;
    private String recipientGroup;
    private String status;
    private LocalDateTime attemptedAt;
    private String failureReason;

    public AlertDeliveryResponse() {}

    public AlertDeliveryResponse(Long id, String alertId, String channel, String recipientGroup, String status, LocalDateTime attemptedAt, String failureReason) {
        this.id = id;
        this.alertId = alertId;
        this.channel = channel;
        this.recipientGroup = recipientGroup;
        this.status = status;
        this.attemptedAt = attemptedAt;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRecipientGroup() {
        return recipientGroup;
    }

    public void setRecipientGroup(String recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
