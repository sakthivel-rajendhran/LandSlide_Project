package com.slopeguard.exception;

/**
 * Thrown on failed authentication without disclosing whether the email or password was wrong.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
