package com.slopeguard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PublicOverviewResponse {

    @JsonProperty("monitored_regions")
    private Long monitoredRegions;

    @JsonProperty("elevated_risk_regions")
    private Long elevatedRiskRegions;

    @JsonProperty("data_status")
    private String dataStatus;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;

    public PublicOverviewResponse() {}

    public PublicOverviewResponse(Long monitoredRegions, Long elevatedRiskRegions, String dataStatus, LocalDateTime lastUpdated) {
        this.monitoredRegions = monitoredRegions;
        this.elevatedRiskRegions = elevatedRiskRegions;
        this.dataStatus = dataStatus;
        this.lastUpdated = lastUpdated;
    }

    public Long getMonitoredRegions() {
        return monitoredRegions;
    }

    public void setMonitoredRegions(Long monitoredRegions) {
        this.monitoredRegions = monitoredRegions;
    }

    public Long getElevatedRiskRegions() {
        return elevatedRiskRegions;
    }

    public void setElevatedRiskRegions(Long elevatedRiskRegions) {
        this.elevatedRiskRegions = elevatedRiskRegions;
    }

    public String getDataStatus() {
        return dataStatus;
    }

    public void setDataStatus(String dataStatus) {
        this.dataStatus = dataStatus;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
