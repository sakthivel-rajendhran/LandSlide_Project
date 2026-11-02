package com.slopeguard.controller;

import com.slopeguard.dto.ChangePasswordRequest;
import com.slopeguard.dto.UpdateProfileRequest;
import com.slopeguard.dto.UserProfileResponse;
import com.slopeguard.exception.InvalidCredentialsException;
import com.slopeguard.exception.ResourceNotFoundException;
import com.slopeguard.model.User;
import com.slopeguard.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for authenticated user profile updates and password changes.
 */
@RestController
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves profile details for the currently authenticated user.
     */
    @GetMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account was not found."));

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getPhoneNumber(),
                user.getCreatedAt()
        ));
    }

    /**
     * Updates full name of authenticated user.
     */
    @PutMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account was not found."));

        user.setFullName(request.getFullName().trim());
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        return ResponseEntity.ok(new UserProfileResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getPhoneNumber(),
                saved.getCreatedAt()
        ));
    }

    /**
     * Changes password of authenticated user, verifying current password.
     */
    @PutMapping("/api/users/me/password")
    public ResponseEntity<Map<String, Object>> changePassword(@AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account was not found."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid current password provided.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Your password has been updated successfully.",
                "success", true
        ));
    }
}
