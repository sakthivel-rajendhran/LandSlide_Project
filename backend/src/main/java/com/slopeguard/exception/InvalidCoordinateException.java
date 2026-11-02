package com.slopeguard.exception;

/**
 * Thrown when geographic coordinates are outside valid boundary limits.
 */
public class InvalidCoordinateException extends IllegalArgumentException {

    public InvalidCoordinateException(String message) {
        super(message);
    }
}
