package com.slopeguard.exception;

/**
 * Thrown when an uploaded file is empty, corrupted, or not an allowed format (JPEG, PNG, WebP).
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
