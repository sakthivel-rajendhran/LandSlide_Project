package com.slopeguard.exception;

/**
 * Thrown when AI service integration is disabled via configuration.
 */
public class AiServiceDisabledException extends RuntimeException {

    public AiServiceDisabledException(String message) {
        super(message);
    }
}
