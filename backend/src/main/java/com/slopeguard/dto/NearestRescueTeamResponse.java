package com.slopeguard.dto;

import java.util.Set;

/**
 * Representation of a Rescue Team evaluated for an incident based on GPS proximity,
 * capability matching, and operational availability.
 */
public class NearestRescueTeamResponse {

    private Long teamId;
    private String teamName;
    private String teamCategory;
    private String teamLeader;
    private String contactNumber;
    private String baseAddress;
    private String operatingDistrict;
    private String operatingRegion;
    private Double latitude;
    private Double longitude;
    private Double distanceKm; // Authoritative Haversine straight-line distance in km
    private Set<String> capabilities;
    private String availability; // AVAILABLE, BUSY, OFF_DUTY, INACTIVE
    private long activeMissionsCount;
    private boolean isEligible;
    private String ineligibilityReason;

    public NearestRescueTeamResponse() {}

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamCategory() { return teamCategory; }
    public void setTeamCategory(String teamCategory) { this.teamCategory = teamCategory; }

    public String getTeamLeader() { return teamLeader; }
    public void setTeamLeader(String teamLeader) { this.teamLeader = teamLeader; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getBaseAddress() { return baseAddress; }
    public void setBaseAddress(String baseAddress) { this.baseAddress = baseAddress; }

    public String getOperatingDistrict() { return operatingDistrict; }
    public void setOperatingDistrict(String operatingDistrict) { this.operatingDistrict = operatingDistrict; }

    public String getOperatingRegion() { return operatingRegion; }
    public void setOperatingRegion(String operatingRegion) { this.operatingRegion = operatingRegion; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Set<String> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public long getActiveMissionsCount() { return activeMissionsCount; }
    public void setActiveMissionsCount(long activeMissionsCount) { this.activeMissionsCount = activeMissionsCount; }

    public boolean isEligible() { return isEligible; }
    public void setEligible(boolean eligible) { isEligible = eligible; }

    public String getIneligibilityReason() { return ineligibilityReason; }
    public void setIneligibilityReason(String ineligibilityReason) { this.ineligibilityReason = ineligibilityReason; }
}
