package com.slopeguard.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Detailed representation of a registered Rescue Team.
 */
public class RescueTeamResponse {

    private Long id;
    private Long userId;
    private String email;
    private String teamName;
    private String teamCategory;
    private String teamLeader;
    private String contactNumber;
    private int teamSize;
    private String operatingDistrict;
    private String operatingRegion;
    private String baseAddress;
    private Double latitude;
    private Double longitude;
    private String verificationStatus; // PENDING, APPROVED, REJECTED
    private String operationalStatus;  // ACTIVE, INACTIVE
    private String availability;       // AVAILABLE, BUSY, OFF_DUTY, INACTIVE
    private String rejectionReason;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private Set<String> capabilities;
    private long activeMissionsCount;

    public RescueTeamResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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
    public String getAvailabilityStatus() { return availability; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getVerifiedByName() { return verifiedByName; }
    public void setVerifiedByName(String verifiedByName) { this.verifiedByName = verifiedByName; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<String> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities; }

    public long getActiveMissionsCount() { return activeMissionsCount; }
    public void setActiveMissionsCount(long activeMissionsCount) { this.activeMissionsCount = activeMissionsCount; }
}
