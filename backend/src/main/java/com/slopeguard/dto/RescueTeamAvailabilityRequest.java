package com.slopeguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for toggling team operational availability state.
 */
public class RescueTeamAvailabilityRequest {

    @NotBlank(message = "Availability status is required")
    @Pattern(regexp = "^(AVAILABLE|OFF_DUTY|BUSY)$", message = "Availability must be AVAILABLE, OFF_DUTY, or BUSY")
    private String availability;

    public RescueTeamAvailabilityRequest() {}

    public RescueTeamAvailabilityRequest(String availability) {
        this.availability = availability;
    }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getAvailabilityStatus() { return availability; }
    public void setAvailabilityStatus(String availabilityStatus) {
        if (this.availability == null || this.availability.isBlank()) {
            this.availability = availabilityStatus;
        }
    }
}
