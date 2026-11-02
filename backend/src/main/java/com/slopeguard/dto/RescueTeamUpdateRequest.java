package com.slopeguard.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * Payload for updating team information and settings.
 */
public class RescueTeamUpdateRequest {

    @NotBlank(message = "Team leader name is required")
    private String teamLeader;

    @NotBlank(message = "Contact phone number is required")
    private String contactNumber;

    @Min(value = 1, message = "Team size must be at least 1 responder")
    private int teamSize;

    @NotBlank(message = "Operating district is required")
    private String operatingDistrict;

    @NotBlank(message = "Operating region is required")
    private String operatingRegion;

    @NotBlank(message = "Base headquarters address is required")
    private String baseAddress;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    @NotEmpty(message = "Capabilities cannot be empty")
    private Set<String> capabilities;

    public RescueTeamUpdateRequest() {}

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

    public Set<String> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities; }
}
