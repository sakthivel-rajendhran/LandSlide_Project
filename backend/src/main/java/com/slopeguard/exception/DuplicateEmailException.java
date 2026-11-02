package com.slopeguard.exception;

/**
 * Thrown when registration is attempted with an email that already exists.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
