package com.slopeguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit log entity recording administrative verification decisions
 * (Approve, Reject, Activate, Deactivate) on registered Rescue Teams.
 */
@Entity
@Table(name = "rescue_team_verifications")
public class RescueTeamVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_team_id", nullable = false)
    private RescueTeam rescueTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id", nullable = false)
    private User reviewedBy;

    @Column(name = "action", nullable = false, length = 30)
    private String action; // APPROVE, REJECT, ACTIVATE, DEACTIVATE

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public RescueTeamVerification() {
        this.reviewedAt = LocalDateTime.now();
    }

    public RescueTeamVerification(RescueTeam rescueTeam, User reviewedBy, String action, String notes) {
        this.rescueTeam = rescueTeam;
        this.reviewedBy = reviewedBy;
        this.action = action;
        this.notes = notes;
        this.reviewedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RescueTeam getRescueTeam() { return rescueTeam; }
    public void setRescueTeam(RescueTeam rescueTeam) { this.rescueTeam = rescueTeam; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
