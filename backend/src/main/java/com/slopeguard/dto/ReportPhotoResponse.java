package com.slopeguard.dto;

import java.time.LocalDateTime;

/**
 * Public response DTO for report photographs.
 * Never exposes server filesystem paths, storage disk filenames, or private uploader details.
 */
public class ReportPhotoResponse {

    private String id;
    private Long reportId;
    private String contentType;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private String downloadUrl;

    public ReportPhotoResponse() {
    }

    public ReportPhotoResponse(String id, Long reportId, String contentType, long fileSize,
                               LocalDateTime uploadedAt, String downloadUrl) {
        this.id = id;
        this.reportId = reportId;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.downloadUrl = downloadUrl;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
