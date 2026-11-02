package com.slopeguard.dto;

import com.slopeguard.model.AuditEvent;
import com.slopeguard.model.AuditEventType;
import com.slopeguard.model.AuditOutcome;
import com.slopeguard.model.ReportStatus;

import java.time.Instant;

/**
 * Public response DTO for administrative audit trail inspection.
 * Omits internal system details, credentials, and sensitive tokens.
 */
public class AuditEventResponse {

    private String eventId;
    private AuditEventType eventType;
    private Long actorUserId;
    private Long relatedReportId;
    private Instant timestamp;
    private AuditOutcome outcome;
    private String description;
    private ReportStatus previousStatus;
    private ReportStatus newStatus;

    public AuditEventResponse() {
    }

    public AuditEventResponse(String eventId,
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
        this.timestamp = timestamp;
        this.outcome = outcome;
        this.description = description;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public static AuditEventResponse fromEntity(AuditEvent event) {
        if (event == null) {
            return null;
        }
        return new AuditEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getActorUserId(),
                event.getRelatedReportId(),
                event.getTimestamp(),
                event.getOutcome(),
                event.getDescription(),
                event.getPreviousStatus(),
                event.getNewStatus()
        );
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
}
