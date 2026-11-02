package com.slopeguard.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

/**
 * Domain entity capturing security and lifecycle transitions.
 * Persisted in MySQL via Spring Data JPA.
 * Sensitive data such as password hashes, tokens, and raw media are strictly excluded.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "related_report_id")
    private Long relatedReportId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "outcome", nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "previous_status", length = 30)
    private ReportStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "new_status", length = 30)
    private ReportStatus newStatus;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public AuditEvent() {
        this.timestamp = Instant.now();
    }

    public AuditEvent(String eventId,
                      AuditEventType eventType,
                      Long actorUserId,
                      Long relatedReportId,
                      Instant timestamp,
                      AuditOutcome outcome,
                      String description,
                      ReportStatus previousStatus,
                      ReportStatus newStatus) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.relatedReportId = relatedReportId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.outcome = outcome;
        this.description = description;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Long getRelatedReportId() {
        return relatedReportId;
    }

    public void setRelatedReportId(Long relatedReportId) {
        this.relatedReportId = relatedReportId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AuditOutcome outcome) {
        this.outcome = outcome;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReportStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ReportStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ReportStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ReportStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
