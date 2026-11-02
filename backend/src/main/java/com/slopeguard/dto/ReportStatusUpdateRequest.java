package com.slopeguard.dto;

import com.slopeguard.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for administrative report status adjudication.
 */
public class ReportStatusUpdateRequest {

    @NotNull(message = "Report status is required")
    private ReportStatus status;

    @Size(max = 500, message = "Review notes must not exceed 500 characters")
    private String reviewNotes;

    public ReportStatusUpdateRequest() {
    }

    public ReportStatusUpdateRequest(ReportStatus status, String reviewNotes) {
        this.status = status;
        this.reviewNotes = reviewNotes;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getReviewNote() {
        return reviewNotes;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNotes = reviewNote;
    }
}
