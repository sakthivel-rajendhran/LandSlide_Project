package com.slopeguard.controller;

import com.slopeguard.dto.AuthResponse;
import com.slopeguard.dto.ForgotPasswordRequest;
import com.slopeguard.dto.LoginRequest;
import com.slopeguard.dto.RegisterRequest;
import com.slopeguard.dto.RegisterResponse;
import com.slopeguard.dto.ResetPasswordRequest;
import com.slopeguard.dto.UserProfileResponse;
import com.slopeguard.exception.ResourceNotFoundException;
import com.slopeguard.model.User;
import com.slopeguard.service.JwtService;
import com.slopeguard.service.PasswordResetService;
import com.slopeguard.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Controller providing citizen registration, authentication login, profile retrieval, logout, and password recovery.
 * Owned by Module 1 (Ragul).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final com.slopeguard.service.RescueTeamService rescueTeamService;

    public AuthController(
            UserService userService,
            JwtService jwtService,
            PasswordResetService passwordResetService,
            com.slopeguard.service.RescueTeamService rescueTeamService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.rescueTeamService = rescueTeamService;
    }

    /**
     * Self-service citizen account registration.
     * Enforces CITIZEN role assignment.
     */
    /**
     * Self-service specialized Rescue Team registration.
     * Enforces PENDING verification state and INACTIVE operational status.
     */
    @PostMapping("/register-rescue-team")
    public ResponseEntity<Map<String, Object>> registerRescueTeam(@Valid @RequestBody com.slopeguard.dto.RescueTeamRegisterRequest request) {
        com.slopeguard.dto.RescueTeamResponse team = rescueTeamService.registerRescueTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "teamId", team.getId(),
                "teamName", team.getTeamName(),
                "verificationStatus", team.getVerificationStatus(),
                "operationalStatus", team.getOperationalStatus(),
                "message", "Rescue Team registration submitted successfully. Your request is pending verification by the Disaster Management Officer (DMO)."
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerCitizen(request);
        String token = jwtService.generateToken(user);

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                "Citizen account registered successfully. You may now log in.",
                token,
                "Bearer"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates email and password, issuing a signed JWT access token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the profile and role of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account was not found."));

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getPhoneNumber(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Client logout guidance.
     * In a stateless architecture without token blacklisting, the client is instructed to delete
     * the bearer token from its client storage.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of(
                "message", "Logout acknowledged. Stateless access tokens must be discarded by the client.",
                "stateless", true,
                "tokenRevoked", false
        ));
    }

    /**
     * Initiates password recovery.
     * Always returns standard message without revealing whether the email exists.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestReset(request.getEmail()));
    }

    /**
     * Resets account password using a validated single-use token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request.getToken(), request.getNewPassword()));
    }

    /**
     * Completes OAuth2 login by consuming the temporary HttpOnly handoff session cookie.
     * Generates and returns the standard AuthResponse with the user details and JWT access token.
     * Destroys the temporary handoff cookie to prevent replay.
     */
    @GetMapping("/oauth/session")
    public ResponseEntity<?> getOAuthSession(HttpServletRequest request, HttpServletResponse response) {
        Optional<jakarta.servlet.http.Cookie> cookieOpt = com.slopeguard.util.CookieUtils.getCookie(
                request,
                com.slopeguard.security.oauth2.OAuth2AuthenticationSuccessHandler.OAUTH_SESSION_COOKIE_NAME
        );

        if (cookieOpt.isEmpty() || cookieOpt.get().getValue() == null || cookieOpt.get().getValue().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "unauthorized",
                    "message", "No active OAuth handoff session found."
            ));
        }

        String token = cookieOpt.get().getValue();
        // Clear cookie immediately so it can only be consumed once
        com.slopeguard.util.CookieUtils.deleteCookie(
                request,
                response,
                com.slopeguard.security.oauth2.OAuth2AuthenticationSuccessHandler.OAUTH_SESSION_COOKIE_NAME
        );

        try {
            Jwt jwt = jwtService.getJwtDecoder().decode(token);
            Long userId = Long.valueOf(jwt.getSubject());
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account was not found."));

            AuthResponse authResponse = new AuthResponse(
                    token,
                    "Bearer",
                    jwtService.getExpirationSeconds(),
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid_token",
                    "message", "Failed to validate OAuth handoff token: " + e.getMessage()
            ));
        }
    }

    /**
     * Direct API Google Authentication endpoint (supports GIS / Google Identity Services).
     * Validates Google identity attributes, verifies account linking rules, and returns an AuthResponse.
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@RequestBody com.slopeguard.dto.GoogleAuthRequest request) {
        String sub = request.getSub();
        String email = request.getEmail();
        String name = request.getName();
        String picture = request.getPicture();

        // If sub or email is missing, but idToken/token is present, attempt basic parse
        if ((sub == null || email == null) && (request.getIdToken() != null || request.getToken() != null)) {
            String rawToken = request.getIdToken() != null ? request.getIdToken() : request.getToken();
            try {
                String[] parts = rawToken.split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(decoded);
                    if (sub == null && node.has("sub")) sub = node.get("sub").asText();
                    if (email == null && node.has("email")) email = node.get("email").asText();
                    if (name == null && node.has("name")) name = node.get("name").asText();
                    if (picture == null && node.has("picture")) picture = node.get("picture").asText();
                }
            } catch (Exception ignored) {}
        }

        if (sub == null || email == null) {
            throw new IllegalArgumentException("Google identity attributes (sub, email) are required.");
        }

        User user = userService.processOAuth2User("GOOGLE", sub, email, name, picture);
        String token = jwtService.generateToken(user);

        AuthResponse authResponse = new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );

        return ResponseEntity.ok(authResponse);
    }

}
