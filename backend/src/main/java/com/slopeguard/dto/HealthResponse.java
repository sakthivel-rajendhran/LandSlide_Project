package com.slopeguard.dto;

/**
 * DTO representing system health status.
 * Explicitly indicates databaseConfigured: false in Phase 1.
 */
public class HealthResponse {

    private String status;
    private String application;
    private boolean databaseConfigured;

    public HealthResponse() {
    }

    public HealthResponse(String status, String application, boolean databaseConfigured) {
        this.status = status;
        this.application = application;
        this.databaseConfigured = databaseConfigured;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public boolean isDatabaseConfigured() {
        return databaseConfigured;
    }

    public void setDatabaseConfigured(boolean databaseConfigured) {
        this.databaseConfigured = databaseConfigured;
    }
}
