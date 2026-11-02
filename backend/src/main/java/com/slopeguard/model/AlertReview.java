package com.slopeguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity capturing immutable review history of alert state transitions.
 * Owned by Module 1 (Ragul).
 */
@Entity
@Table(name = "alert_reviews")
public class AlertReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", length = 64, nullable = false)
    private String alertId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AlertReview() {
        this.createdAt = LocalDateTime.now();
    }

    public AlertReview(String alertId, String action, Long actorId, String reason) {
        this.alertId = alertId;
        this.action = action;
        this.actorId = actorId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
