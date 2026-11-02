package com.slopeguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity representing an alert notification dispatch attempt across channels.
 * Owned by Module 1 (Ragul).
 */
@Entity
@Table(name = "alert_deliveries")
public class AlertDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", length = 64, nullable = false)
    private String alertId;

    @Column(name = "channel", length = 50, nullable = false)
    private String channel;

    @Column(name = "recipient_group", length = 100, nullable = false)
    private String recipientGroup;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    public AlertDelivery() {
        this.attemptedAt = LocalDateTime.now();
    }

    public AlertDelivery(String alertId, String channel, String recipientGroup, String status, String failureReason) {
        this.alertId = alertId;
        this.channel = channel;
        this.recipientGroup = recipientGroup;
        this.status = status;
        this.failureReason = failureReason;
        this.attemptedAt = LocalDateTime.now();
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
