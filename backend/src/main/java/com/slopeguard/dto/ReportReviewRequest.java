package com.slopeguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for administrative report review note submission.
 */
public class ReportReviewRequest {

    @NotBlank(message = "Review note is required")
    @Size(max = 1000, message = "Review note must not exceed 1000 characters")
    private String reviewNote;

    public ReportReviewRequest() {
    }

    public ReportReviewRequest(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getReviewNotes() {
        return reviewNote;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNote = reviewNotes;
    }
}
