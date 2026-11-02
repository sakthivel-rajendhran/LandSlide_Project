package com.slopeguard.exception;

/**
 * Thrown when an external environmental data provider (such as Open-Meteo) fails,
 * times out, or returns a non-successful response.
 */
public class EnvironmentalProviderException extends RuntimeException {

    public EnvironmentalProviderException(String message) {
        super(message);
    }

    public EnvironmentalProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
