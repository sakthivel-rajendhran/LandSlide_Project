package com.slopeguard.exception;

/**
 * Thrown when an AI prediction request provides physically impossible or invalid features.
 */
public class InvalidPredictionFeatureException extends RuntimeException {

    public InvalidPredictionFeatureException(String message) {
        super(message);
    }
}
