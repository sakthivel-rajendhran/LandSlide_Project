package com.slopeguard.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * Payload for registering a new specialized Rescue Team.
 */
public class RescueTeamRegisterRequest {

    @NotBlank(message = "Official team name is required")
    @Size(max = 150)
    private String teamName;

    @NotBlank(message = "Team category is required")
    private String teamCategory; // NDRF, SDRF, FIRE_AND_RESCUE, CIVIL_DEFENSE, VOLUNTEER_SAR, MEDICAL_RESPONSE

    @NotBlank(message = "Team leader name is required")
    @Size(max = 100)
    private String teamLeader;

    @NotBlank(message = "Official email address is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Contact phone number is required")
    private String contactNumber;

    @Min(value = 1, message = "Team size must be at least 1 responder")
    private int teamSize = 5;

    @NotBlank(message = "Operating district is required")
    private String operatingDistrict;

    @NotBlank(message = "Operating region is required")
    private String operatingRegion;

    @NotBlank(message = "Base headquarters address is required")
    private String baseAddress;

    @NotNull(message = "Base latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Base longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotEmpty(message = "At least one rescue capability must be selected")
    private Set<String> capabilities;

    public RescueTeamRegisterRequest() {}

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamCategory() { return teamCategory; }
    public void setTeamCategory(String teamCategory) { this.teamCategory = teamCategory; }

    public String getTeamLeader() { return teamLeader; }
    public void setTeamLeader(String teamLeader) { this.teamLeader = teamLeader; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<String> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities; }
}
