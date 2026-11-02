package com.slopeguard.repository;

import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.IncidentReport;
import com.slopeguard.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link IncidentReport} entities.
 */
@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long>, JpaSpecificationExecutor<IncidentReport> {

    List<IncidentReport> findAllByOrderByReportedTimestampDesc();

    List<IncidentReport> findByReporterUserIdOrderByReportedTimestampDesc(Long reporterUserId);

    @Query("SELECT r FROM IncidentReport r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.incidentCategory = :category)")
    Page<IncidentReport> findByFilters(
            @Param("status") ReportStatus status,
            @Param("category") IncidentCategory category,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM IncidentReport r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.incidentCategory = :category)")
    long countByFilters(
            @Param("status") ReportStatus status,
            @Param("category") IncidentCategory category
    );
}
