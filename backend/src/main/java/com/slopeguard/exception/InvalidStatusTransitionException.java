package com.slopeguard.exception;

/**
 * Thrown when an illegal lifecycle status transition is attempted on a report.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
