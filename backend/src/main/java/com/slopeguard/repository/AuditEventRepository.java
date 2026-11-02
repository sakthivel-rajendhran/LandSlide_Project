package com.slopeguard.repository;

import com.slopeguard.model.AuditEvent;
import com.slopeguard.model.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for {@link AuditEvent} entities.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    List<AuditEvent> findByRelatedReportIdOrderByTimestampDesc(Long relatedReportId);

    @Query("SELECT e FROM AuditEvent e WHERE " +
           "(:eventType IS NULL OR e.eventType = :eventType) AND " +
           "(:actorUserId IS NULL OR e.actorUserId = :actorUserId) AND " +
           "(:relatedReportId IS NULL OR e.relatedReportId = :relatedReportId) AND " +
           "(:startDate IS NULL OR e.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR e.timestamp <= :endDate)")
    Page<AuditEvent> findByFilters(
            @Param("eventType") AuditEventType eventType,
            @Param("actorUserId") Long actorUserId,
            @Param("relatedReportId") Long relatedReportId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
}
