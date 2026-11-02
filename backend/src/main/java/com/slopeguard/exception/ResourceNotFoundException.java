package com.slopeguard.exception;

/**
 * Thrown when a requested resource (risk record, incident report, etc.) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
