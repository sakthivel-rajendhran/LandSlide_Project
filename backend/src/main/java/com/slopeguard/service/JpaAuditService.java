package com.slopeguard.service;

import com.slopeguard.dto.AuditEventResponse;
import com.slopeguard.model.AuditEvent;
import com.slopeguard.model.AuditEventType;
import com.slopeguard.model.AuditOutcome;
import com.slopeguard.model.ReportStatus;
import com.slopeguard.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Production MySQL-backed implementation of {@link AuditService}.
 * Persists immutable audit records into the `audit_events` table.
 */
@Service
@Primary
public class JpaAuditService implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(JpaAuditService.class);

    private final AuditEventRepository auditEventRepository;

    public JpaAuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    @Transactional
    public AuditEvent recordEvent(AuditEventType eventType,
                                  Long actorUserId,
                                  Long relatedReportId,
                                  AuditOutcome outcome,
                                  String description,
                                  ReportStatus previousStatus,
                                  ReportStatus newStatus) {
        try {
            String eventId = "AUDIT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            AuditEvent event = new AuditEvent(
                    eventId,
                    eventType,
                    actorUserId,
                    relatedReportId,
                    Instant.now(),
                    outcome,
                    description,
                    previousStatus,
                    newStatus
            );
            AuditEvent saved = auditEventRepository.save(event);
            log.info("Persisted audit event: id={}, type={}, actor={}, reportId={}, outcome={}",
                    saved.getEventId(), eventType, actorUserId, relatedReportId, outcome);
            return saved;
        } catch (Exception ex) {
            log.error("Failed to persist audit event: type={}, error={}", eventType, ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditLogs(AuditEventType eventType,
                                                Long actorUserId,
                                                Long relatedReportId,
                                                Instant startDate,
                                                Instant endDate,
                                                int page,
                                                int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range: startDate cannot be after endDate.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditEvent> eventPage = auditEventRepository.findByFilters(
                eventType, actorUserId, relatedReportId, startDate, endDate, pageable
        );

        List<AuditEventResponse> responses = new ArrayList<>();
        for (AuditEvent event : eventPage.getContent()) {
            responses.add(AuditEventResponse.fromEntity(event));
        }

        return Collections.unmodifiableList(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditLogsForReport(Long reportId, int page, int size) {
        return getAuditLogs(null, null, reportId, null, null, page, size);
    }
}
