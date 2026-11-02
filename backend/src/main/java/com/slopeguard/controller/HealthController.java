package com.slopeguard.controller;

import com.slopeguard.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing system health check and connectivity status.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        // Explicitly reports databaseConfigured: false in Phase 1
        HealthResponse response = new HealthResponse(
                "UP",
                "SlopeGuard Backend",
                false
        );
        return ResponseEntity.ok(response);
    }
}
