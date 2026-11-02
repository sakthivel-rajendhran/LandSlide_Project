package com.slopeguard.dto;

import com.slopeguard.model.UserRole;

/**
 * Authentication response containing signed JWT access token and public account profile.
 * Never includes credentials or password hashes.
 */
public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, long expiresIn, Long userId,
                        String email, String fullName, UserRole role) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
