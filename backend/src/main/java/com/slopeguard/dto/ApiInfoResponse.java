package com.slopeguard.dto;

import java.util.List;

/**
 * DTO providing backend metadata and available endpoint routes.
 */
public class ApiInfoResponse {

    private String application;
    private String version;
    private String phase;
    private String description;
    private List<String> endpoints;

    public ApiInfoResponse() {
    }

    public ApiInfoResponse(String application, String version, String phase, String description, List<String> endpoints) {
        this.application = application;
        this.version = version;
        this.phase = phase;
        this.description = description;
        this.endpoints = endpoints;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }
}
