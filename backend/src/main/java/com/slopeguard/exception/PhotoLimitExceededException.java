package com.slopeguard.exception;

/**
 * Thrown when an incident report has reached its maximum allowable photo attachment limit.
 */
public class PhotoLimitExceededException extends RuntimeException {

    public PhotoLimitExceededException(String message) {
        super(message);
    }
}
