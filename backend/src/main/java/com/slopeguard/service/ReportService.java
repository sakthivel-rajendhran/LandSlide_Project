package com.slopeguard.service;

import com.slopeguard.dto.AdminReportResponse;
import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.dto.ReportResponse;
import com.slopeguard.dto.ReportStatusUpdateRequest;
import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.IncidentReport;
import com.slopeguard.model.ReportStatus;
import com.slopeguard.model.ReportStatusAuditLog;

import java.util.List;

/**
 * Service interface for managing citizen incident reports, administrative review queues,
 * and status transitions. Backed by an in-memory thread-safe store in Phase 1-4.
 */
public interface ReportService {

    /**
     * Submits a new citizen landslide incident report.
     */
    ReportResponse createReport(ReportCreateRequest request);

    /**
     * Submits a new citizen landslide incident report associated with an authenticated user account.
     */
    ReportResponse createReport(ReportCreateRequest request, Long reporterUserId, String reporterFullName);

    /**
     * Retrieves all active incident reports for public viewing (privacy filtered).
     */
    List<ReportResponse> getAllPublicReports();

    /**
     * Retrieves reports submitted specifically by the given citizen user.
     */
    List<ReportResponse> getReportsForCitizen(Long citizenUserId);

    /**
     * Retrieves a single incident report by ID with ownership enforcement.
     * Accessible by the report owner or an administrator.
     */
    ReportResponse getReportByIdSecured(Long id, Long requesterUserId, boolean isAdmin);

    /**
     * Retrieves a single incident report by ID for public viewing.
     */
    ReportResponse getPublicReportById(Long id);

    /**
     * Retrieves internal report domain entity (for photo attachments & internal services).
     */
    IncidentReport getReportEntity(Long id);

    /**
     * Retrieves all incident reports with administrative audit details.
     */
    List<AdminReportResponse> getAllAdminReports();

    /**
     * Retrieves administrative review queue with filtering, sorting, and in-memory pagination.
     */
    List<AdminReportResponse> getAdminReports(ReportStatus status, IncidentCategory category, String sort, int page, int size);

    /**
     * Retrieves total count of reports matching filter criteria.
     */
    int countAdminReports(ReportStatus status, IncidentCategory category);

    /**
     * Retrieves single incident report details for administrative review.
     */
    AdminReportResponse getAdminReportById(Long id);

    /**
     * Retrieves pending incident reports awaiting administrative review.
     */
    List<AdminReportResponse> getPendingAdminReports(String sort, int page, int size);

    /**
     * Records an administrative review note on an incident report.
     */
    AdminReportResponse reviewReport(Long id, String reviewNote, Long adminUserId, String adminEmail);

    /**
     * Updates the verification status of an incident report.
     */
    AdminReportResponse updateReportStatus(Long id, ReportStatusUpdateRequest request);

    /**
     * Updates the status of an incident report and appends an audit event log.
     */
    AdminReportResponse updateReportStatus(Long id, ReportStatusUpdateRequest request, Long adminUserId, String adminEmail);

    /**
     * Retrieves the audit log history for a specific report.
     */
    List<ReportStatusAuditLog> getAuditLogsForReport(Long reportId);
}
