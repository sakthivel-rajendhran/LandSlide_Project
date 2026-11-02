package com.slopeguard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an OAuth provider login attempts to use an email address that belongs
 * to an existing local password-authenticated account without explicit authentication linking.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OAuthEmailConflictException extends RuntimeException {

    private final String email;

    public OAuthEmailConflictException(String email) {
        super("An account with email '" + email + "' already exists using password authentication. Please log in with your email and password.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

