package com.slopeguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Domain entity representing a registered Rescue Team (e.g. NDRF, SDRF, Civil Defense).
 * Contains geographical base coordinates, operational availability, capabilities,
 * and DMO verification status.
 */
@Entity
@Table(name = "rescue_teams", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rescue_teams_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_rescue_teams_name", columnNames = "team_name")
})
public class RescueTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_name", nullable = false, length = 150)
    private String teamName;

    @Column(name = "team_category", nullable = false, length = 50)
    private String teamCategory; // NDRF, SDRF, FIRE_AND_RESCUE, CIVIL_DEFENSE, VOLUNTEER_SAR, MEDICAL_RESPONSE

    @Column(name = "team_leader", nullable = false, length = 100)
    private String teamLeader;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Column(name = "team_size", nullable = false)
    private int teamSize = 5;

    @Column(name = "operating_district", nullable = false, length = 100)
    private String operatingDistrict;

    @Column(name = "operating_region", nullable = false, length = 100)
    private String operatingRegion;

    @Column(name = "base_address", nullable = false, length = 255)
    private String baseAddress;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "verification_status", nullable = false, length = 30)
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "operational_status", nullable = false, length = 30)
    private String operationalStatus = "INACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "availability", nullable = false, length = 30)
    private String availability = "INACTIVE"; // AVAILABLE, BUSY, OFF_DUTY, INACTIVE

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rescue_team_capabilities", joinColumns = @JoinColumn(name = "rescue_team_id"))
    @Column(name = "capability", nullable = false)
    private Set<String> capabilities = new HashSet<>();

    public RescueTeam() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.verificationStatus = "PENDING";
        this.operationalStatus = "INACTIVE";
        this.availability = "INACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamCategory() { return teamCategory; }
    public void setTeamCategory(String teamCategory) { this.teamCategory = teamCategory; }

    public String getTeamLeader() { return teamLeader; }
    public void setTeamLeader(String teamLeader) { this.teamLeader = teamLeader; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }

    public String getOperatingDistrict() { return operatingDistrict; }
    public void setOperatingDistrict(String operatingDistrict) { this.operatingDistrict = operatingDistrict; }

    public String getOperatingRegion() { return operatingRegion; }
    public void setOperatingRegion(String operatingRegion) { this.operatingRegion = operatingRegion; }

    public String getBaseAddress() { return baseAddress; }
    public void setBaseAddress(String baseAddress) { this.baseAddress = baseAddress; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public User getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<String> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities; }
}
