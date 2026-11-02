package com.slopeguard.dto;

public class AlertTransitionRequest {

    private String reason;

    public AlertTransitionRequest() {}

    public AlertTransitionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
