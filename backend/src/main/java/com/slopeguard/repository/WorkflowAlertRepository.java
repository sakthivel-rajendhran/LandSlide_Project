package com.slopeguard.repository;

import com.slopeguard.model.WorkflowAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowAlertRepository extends JpaRepository<WorkflowAlert, String> {

    List<WorkflowAlert> findByStatusOrderByPublishedAtDesc(String status);

    @Query("SELECT a FROM WorkflowAlert a WHERE a.status = 'PUBLISHED' ORDER BY a.publishedAt DESC")
    List<WorkflowAlert> findPublishedAlerts(Pageable pageable);

    @Query("SELECT a FROM WorkflowAlert a WHERE (:status IS NULL OR a.status = :status) AND (:severity IS NULL OR a.severity = :severity) ORDER BY a.createdAt DESC")
    Page<WorkflowAlert> searchAlerts(@Param("status") String status, @Param("severity") String severity, Pageable pageable);

    long countByStatus(String status);

    long countByStatusAndSeverityIn(String status, List<String> severities);
}
