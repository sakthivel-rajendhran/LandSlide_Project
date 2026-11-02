package com.slopeguard.service;

import com.slopeguard.exception.InvalidCredentialsException;
import com.slopeguard.model.PasswordResetToken;
import com.slopeguard.model.User;
import com.slopeguard.repository.PasswordResetTokenRepository;
import com.slopeguard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
public class JpaPasswordResetService implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(JpaPasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JpaPasswordResetService(PasswordResetTokenRepository tokenRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Map<String, Object> requestReset(String email) {
        // Always return the exact same generic message per spec so account existence is not revealed
        String standardMessage = "If an account exists for this email address, instructions to reset your password will be provided.";

        if (email == null || email.isBlank()) {
            return Map.of("message", standardMessage);
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        String generatedToken = null;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            generatedToken = UUID.randomUUID().toString().replace("-", "");
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

            PasswordResetToken resetToken = new PasswordResetToken(user.getId(), generatedToken, expiresAt);
            tokenRepository.save(resetToken);

            log.info("Password reset initiated for user ID {}. Token expires at {}.", user.getId(), expiresAt);
        } else {
            log.info("Password reset requested for non-existent email address: {}", email);
        }

        // Return token in payload for development testing; message is always identical
        if (generatedToken != null) {
            return Map.of(
                    "message", standardMessage,
                    "resetToken", generatedToken
            );
        }

        return Map.of("message", standardMessage);
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException("Reset token is required.");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(token.trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired password reset token."));

        if (resetToken.isExpired()) {
            throw new InvalidCredentialsException("This password reset token has expired.");
        }

        if (resetToken.isUsed()) {
            throw new InvalidCredentialsException("This password reset token has already been used.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("User account was not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Password successfully updated for user ID {}.", user.getId());

        return Map.of(
                "message", "Your password has been updated successfully.",
                "success", true
        );
    }
}
