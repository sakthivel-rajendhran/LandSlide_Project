package com.slopeguard.dto;

import com.slopeguard.model.IncidentCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request payload for citizen landslide incident report submissions.
 */
public class ReportCreateRequest {

    @Size(max = 100, message = "Reporter name must not exceed 100 characters")
    private String reporterName;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    @NotBlank(message = "Location description is required")
    @Size(min = 3, max = 255, message = "Location description must be between 3 and 255 characters")
    private String locationDescription;

    @NotNull(message = "Incident category is required")
    private IncidentCategory incidentCategory;

    private LocalDateTime reportedTimestamp;

    private LocalDateTime observationTimestamp;

    @Size(max = 1000, message = "Environmental observations must not exceed 1000 characters")
    private String environmentalObservations;

    public ReportCreateRequest() {
    }

    public ReportCreateRequest(String reporterName, String description, Double latitude, Double longitude,
                               String locationDescription, IncidentCategory incidentCategory,
                               LocalDateTime reportedTimestamp) {
        this.reporterName = reporterName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationDescription = locationDescription;
        this.incidentCategory = incidentCategory;
        this.reportedTimestamp = reportedTimestamp;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public IncidentCategory getIncidentCategory() {
        return incidentCategory;
    }

    public void setIncidentCategory(IncidentCategory incidentCategory) {
        this.incidentCategory = incidentCategory;
    }

    public LocalDateTime getReportedTimestamp() {
        return reportedTimestamp;
    }

    public void setReportedTimestamp(LocalDateTime reportedTimestamp) {
        this.reportedTimestamp = reportedTimestamp;
    }

    public LocalDateTime getObservationTimestamp() {
        return observationTimestamp;
    }

    public void setObservationTimestamp(LocalDateTime observationTimestamp) {
        this.observationTimestamp = observationTimestamp;
    }

    public String getEnvironmentalObservations() {
        return environmentalObservations;
    }

    public void setEnvironmentalObservations(String environmentalObservations) {
        this.environmentalObservations = environmentalObservations;
    }
}
