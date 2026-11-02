package com.slopeguard.exception;

/**
 * Thrown when an environmental data request is made while the provider is administratively disabled.
 */
public class EnvironmentalProviderDisabledException extends RuntimeException {

    public EnvironmentalProviderDisabledException(String message) {
        super(message);
    }
}
