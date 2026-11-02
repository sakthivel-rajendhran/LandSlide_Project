package com.slopeguard.exception;

/**
 * Thrown when the Python AI microservice cannot be reached or returns an unrecoverable error.
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
