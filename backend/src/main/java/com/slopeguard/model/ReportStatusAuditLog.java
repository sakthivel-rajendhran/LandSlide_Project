package com.slopeguard.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Domain entity recording administrative status transitions and adjudication audit trail.
 * Persisted in MySQL database via Spring Data JPA.
 */
@Entity
@Table(name = "report_status_audit_logs")
public class ReportStatusAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "previous_status", length = 30)
    private ReportStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "new_status", nullable = false, length = 30)
    private ReportStatus newStatus;

    @Column(name = "admin_user_id")
    private Long changedByUserId;

    @Column(name = "admin_email", length = 255)
    private String changedByEmail;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    public ReportStatusAuditLog() {
        this.changedAt = LocalDateTime.now();
    }

    public ReportStatusAuditLog(Long id, Long reportId, ReportStatus previousStatus, ReportStatus newStatus,
                                Long changedByUserId, String changedByEmail, String reviewNotes) {
        this.id = id;
        this.reportId = reportId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedByUserId = changedByUserId;
        this.changedByEmail = changedByEmail;
        this.reviewNotes = reviewNotes;
        this.changedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
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

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Long changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public String getChangedByEmail() {
        return changedByEmail;
    }

    public void setChangedByEmail(String changedByEmail) {
        this.changedByEmail = changedByEmail;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
