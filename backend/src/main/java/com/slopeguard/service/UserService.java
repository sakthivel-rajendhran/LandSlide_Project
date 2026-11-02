package com.slopeguard.service;

import com.slopeguard.dto.RegisterRequest;
import com.slopeguard.model.User;

import java.util.Optional;

/**
 * Service contract for user account lifecycle management and authentication.
 */
public interface UserService {

    /**
     * Registers a new citizen account with default CITIZEN role.
     *
     * @param request Validated registration payload
     * @return Created User entity with encrypted password
     */
    User registerCitizen(RegisterRequest request);

    /**
     * Authenticates credentials and returns the matching User.
     * Throws InvalidCredentialsException if email is not found or password does not match.
     */
    User authenticate(String email, String rawPassword);

    /**
     * Finds a user by unique numeric ID.
     */
    Optional<User> findById(Long id);

    /**
     * Finds a user by normalized email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if an email is already registered.
     */
    boolean emailExists(String email);

    /**
     * Provisions an administrative account with ADMIN role.
     * Intended strictly for controlled development initialization.
     */
    User provisionAdmin(String fullName, String email, String rawPassword);

    /**
     * Authenticates or auto-provisions a citizen account verified by an external OAuth2/OIDC provider (Google).
     * Strictly creates CITIZEN role for new users. Prevents account takeover if email exists with a password account.
     */
    User processOAuth2User(String provider, String providerId, String email, String fullName, String pictureUrl);
}
