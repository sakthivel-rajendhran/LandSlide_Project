package com.slopeguard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity representing metadata for an attached landslide incident photograph.
 * Persisted in MySQL database via Spring Data JPA.
 * <p>
 * Security Rule: Storage filenames and physical filesystem paths are stored internally
 * and must never be exposed directly to untrusted clients.
 */
@Entity
@Table(name = "report_photos")
public class ReportPhotoMetadata {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", insertable = false, updatable = false)
    private IncidentReport report;

    @Column(name = "storage_filename", nullable = false, length = 255)
    private String storageFilename;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "uploader_user_id")
    private Long uploaderUserId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public ReportPhotoMetadata() {
        this.uploadedAt = LocalDateTime.now();
    }

    public ReportPhotoMetadata(String id, Long reportId, String storageFilename, String contentType,
                               long fileSize, Long uploaderUserId) {
        this(id, reportId, storageFilename, null, contentType, fileSize, null, uploaderUserId);
    }

    public ReportPhotoMetadata(String id, Long reportId, String storageFilename, String originalFilename,
                               String contentType, long fileSize, String checksum, Long uploaderUserId) {
        this.id = id;
        this.reportId = reportId;
        this.storageFilename = storageFilename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.checksum = checksum;
        this.uploaderUserId = uploaderUserId;
        this.uploadedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public IncidentReport getReport() {
        return report;
    }

    public void setReport(IncidentReport report) {
        this.report = report;
        if (report != null) {
            this.reportId = report.getId();
        }
    }

    public String getStorageFilename() {
        return storageFilename;
    }

    public void setStorageFilename(String storageFilename) {
        this.storageFilename = storageFilename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUploaderUserId() {
        return uploaderUserId;
    }

    public void setUploaderUserId(Long uploaderUserId) {
        this.uploaderUserId = uploaderUserId;
    }
}
