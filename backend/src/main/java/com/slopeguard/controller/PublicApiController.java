package com.slopeguard.controller;

import com.slopeguard.dto.PublicAlertResponse;
import com.slopeguard.dto.PublicOverviewResponse;
import com.slopeguard.service.WorkflowAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller providing public landing page statistics, published early-warning hazard advisories,
 * and external data source attributions without requiring authentication.
 * Owned by Module 1 (Ragul).
 */
@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    private final WorkflowAlertService alertService;

    public PublicApiController(WorkflowAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Public monitoring figures: monitored regions, elevated risk regions, data freshness status, last updated timestamp.
     */
    @GetMapping("/overview")
    public ResponseEntity<PublicOverviewResponse> getOverview() {
        return ResponseEntity.ok(alertService.getPublicOverview());
    }

    /**
     * Lists active published hazard alerts for the public landing feed and public map.
     * Guaranteed to return only alerts with PUBLISHED status.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<PublicAlertResponse>> getPublicAlerts(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(alertService.getPublicAlerts(limit));
    }

    /**
     * Retrieves specific published hazard advisory by ID.
     * Rejects non-published or draft alerts with 404 to avoid leaking draft existence.
     */
    @GetMapping("/alerts/{id}")
    public ResponseEntity<PublicAlertResponse> getPublicAlert(@PathVariable String id) {
        return ResponseEntity.ok(alertService.getPublicAlert(id));
    }

    /**
     * Returns list of configured satellite, meteorological, and geospatial providers for footer attribution.
     */
    @GetMapping("/data-sources")
    public ResponseEntity<Map<String, Object>> getDataSources() {
        return ResponseEntity.ok(Map.of(
                "dataSources", List.of(
                        Map.of("name", "Copernicus DEM GLO-30", "provider", "European Space Agency (ESA) / Airbus", "description", "Digital Surface Model at 30-meter spatial resolution (EPSG:4326)"),
                        Map.of("name", "Open-Meteo Weather Forecast API", "provider", "Open-Meteo", "description", "Numerical weather prediction and hourly rainfall estimation models"),
                        Map.of("name", "Sentinel-1 Synthetic Aperture Radar", "provider", "Copernicus Sentinel", "description", "InSAR differential interferometry for ground surface displacement tracking"),
                        Map.of("name", "Geological Survey of India (GSI) Lithology Maps", "provider", "Geological Survey of India", "description", "Regional bedrock lithology and structural fault line classifications")
                ),
                "lastVerified", "2026-10-13T00:00:00Z"
        ));
    }
}
