package com.slopeguard.repository;

import com.slopeguard.model.ReportStatusAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ReportStatusAuditLog} entities.
 */
@Repository
public interface ReportStatusAuditLogRepository extends JpaRepository<ReportStatusAuditLog, Long> {

    List<ReportStatusAuditLog> findByReportIdOrderByChangedAtAsc(Long reportId);
}
