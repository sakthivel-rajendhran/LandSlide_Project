package com.slopeguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the SlopeGuard backend service.
 * <p>
 * SlopeGuard is an AI-assisted landslide risk monitoring and early warning
 * system designed for Northeast India.
 * <p>
 * Phase 1 Architecture:
 * - Operates without external database dependencies (MySQL will be integrated in Phase 2).
 * - Utilizes thread-safe in-memory storage for citizen incident reporting.
 * - Serves verified sample risk datasets for Northeast India hill tracts.
 */
@SpringBootApplication
public class SlopeGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlopeGuardApplication.class, args);
    }
}
