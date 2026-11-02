package com.slopeguard.service;

import com.slopeguard.dto.AuditEventResponse;
import com.slopeguard.model.AuditEvent;
import com.slopeguard.model.AuditEventType;
import com.slopeguard.model.AuditOutcome;
import com.slopeguard.model.ReportStatus;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for capturing and inspecting security-relevant and administrative actions.
 */
public interface AuditService {

    /**
     * Records an immutable audit event in memory.
     */
    AuditEvent recordEvent(AuditEventType eventType,
                           Long actorUserId,
                           Long relatedReportId,
                           AuditOutcome outcome,
                           String description,
                           ReportStatus previousStatus,
                           ReportStatus newStatus);

    /**
     * Queries audit events with optional filtering and pagination.
     */
    List<AuditEventResponse> getAuditLogs(AuditEventType eventType,
                                          Long actorUserId,
                                          Long relatedReportId,
                                          Instant startDate,
                                          Instant endDate,
                                          int page,
                                          int size);

    /**
     * Retrieves audit events related to a specific incident report.
     */
    List<AuditEventResponse> getAuditLogsForReport(Long reportId, int page, int size);
}
