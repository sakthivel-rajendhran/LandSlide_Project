package com.slopeguard.controller;

import com.slopeguard.dto.AdminReportResponse;
import com.slopeguard.dto.AuditEventResponse;
import com.slopeguard.dto.ReportPhotoResponse;
import com.slopeguard.dto.ReportReviewRequest;
import com.slopeguard.dto.ReportStatusUpdateRequest;
import com.slopeguard.model.AuditEventType;
import com.slopeguard.model.AuditOutcome;
import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.ReportStatus;
import com.slopeguard.service.AuditService;
import com.slopeguard.service.PhotoService;
import com.slopeguard.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller providing administrative oversight, review queues, status adjudication,
 * and incident audit inspection.
 * <p>
 * Security Policy:
 * Restricted strictly to users possessing the ADMIN role via Spring Security filter chain.
 */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;
    private final PhotoService photoService;
    private final AuditService auditService;

    public AdminReportController(ReportService reportService, PhotoService photoService, AuditService auditService) {
        this.reportService = reportService;
        this.photoService = photoService;
        this.auditService = auditService;
    }

    /**
     * Lists incident reports in the administrative review queue with filtering, sorting, and pagination.
     */
    @GetMapping
    public ResponseEntity<List<AdminReportResponse>> getAdminReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        List<AdminReportResponse> reports = reportService.getAdminReports(status, category, sort, page, size);
        int total = reportService.countAdminReports(status, category);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(total));
        headers.add("X-Page-Number", String.valueOf(page));
        headers.add("X-Page-Size", String.valueOf(size));
        headers.add("X-Total-Pages", String.valueOf(totalPages));

        return ResponseEntity.ok().headers(headers).body(reports);
    }

    /**
     * Retrieves specific incident report details for administrative review.
     * Captures an audit record of administrative inspection.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminReportResponse> getAdminReportById(
            @PathVariable Long id,
            Authentication authentication) {

        AdminReportResponse report = reportService.getAdminReportById(id);
        Long adminId = extractAdminId(authentication);

        auditService.recordEvent(
                AuditEventType.REPORT_ACCESSED,
                adminId,
                id,
                AuditOutcome.SUCCESS,
                "Administrator inspected report details",
                null,
                report.getStatus()
        );

        return ResponseEntity.ok(report);
    }

    /**
     * Filtered shortcut retrieving pending incident reports awaiting administrative triage.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AdminReportResponse>> getPendingReports(
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        List<AdminReportResponse> reports = reportService.getPendingAdminReports(sort, page, size);
        int total = reportService.countAdminReports(ReportStatus.PENDING, null);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(total));
        headers.add("X-Page-Number", String.valueOf(page));
        headers.add("X-Page-Size", String.valueOf(size));
        headers.add("X-Total-Pages", String.valueOf(totalPages));

        return ResponseEntity.ok().headers(headers).body(reports);
    }

    /**
     * Retrieves photo metadata attached to an incident report for administrative evidence review.
     */
    @GetMapping("/{id}/photos")
    public ResponseEntity<List<ReportPhotoResponse>> getAdminReportPhotos(@PathVariable Long id) {
        // Enforce report existence check (throws ResourceNotFoundException if missing)
        reportService.getAdminReportById(id);
        List<ReportPhotoResponse> photos = photoService.getPhotosForReport(id, null, true);
        return ResponseEntity.ok(photos);
    }

    /**
     * Adjudicates the lifecycle verification status of an incident report.
     * Enforces explicit state transition rules and records an audit event.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminReportResponse> updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request,
            Authentication authentication) {

        Long adminId = extractAdminId(authentication);
        String adminEmail = extractAdminEmail(authentication);

        AdminReportResponse updated = reportService.updateReportStatus(id, request, adminId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * Submits an administrative review note and updates report review status.
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<AdminReportResponse> reviewReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportReviewRequest request,
            Authentication authentication) {

        Long adminId = extractAdminId(authentication);
        String adminEmail = extractAdminEmail(authentication);

        AdminReportResponse updated = reportService.reviewReport(id, request.getReviewNote(), adminId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieves chronological audit history for a specific incident report.
     */
    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<AuditEventResponse>> getReportAuditLogs(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        // Enforce report existence check (throws ResourceNotFoundException if missing)
        reportService.getAdminReportById(id);
        List<AuditEventResponse> logs = auditService.getAuditLogsForReport(id, page, size);
        return ResponseEntity.ok(logs);
    }

    private Long extractAdminId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                if (jwt.hasClaim("userId")) {
                    return jwt.getClaim("userId");
                }
                return Long.valueOf(jwt.getSubject());
            } catch (Exception ignored) {
            }
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String extractAdminEmail(Authentication authentication) {
        if (authentication == null) {
            return "admin";
        }
        if (authentication.getPrincipal() instanceof Jwt jwt && jwt.hasClaim("email")) {
            return jwt.getClaimAsString("email");
        }
        return authentication.getName();
    }
}
