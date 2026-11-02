package com.slopeguard.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.slopeguard.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Service responsible for creating and validating stateless signed JWT access tokens.
 * <p>
 * Security Policy:
 * 1. HMAC-SHA256 (HS256) signing algorithm with a required 256-bit (minimum 32 bytes) key.
 * 2. Fail safely at startup if secret is absent or insufficient in entropy.
 * 3. Short default expiration lifetime (15 minutes).
 * 4. Tokens and credentials are never logged.
 */
@Service
public class JwtService {

    private final String secretString;
    private final String issuer;
    private final long expirationSeconds;

    private SecretKey secretKey;
    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;

    public JwtService(
            @Value("${slopeguard.security.jwt.secret:}") String secretString,
            @Value("${slopeguard.security.jwt.issuer:slopeguard-backend}") String issuer,
            @Value("${slopeguard.security.jwt.expiration-seconds:900}") long expirationSeconds) {
        this.secretString = secretString;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    @PostConstruct
    public void validateAndInitialize() {
        if (secretString == null || secretString.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT signing secret is missing. Configure 'JWT_SECRET' environment variable or 'slopeguard.security.jwt.secret' property.");
        }

        byte[] secretBytes = secretString.trim().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT signing secret must be at least 256 bits (32 bytes) for HS256 security compliance. Provided secret length: "
                            + secretBytes.length + " bytes.");
        }

        this.secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(this.secretKey));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(this.secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> defaultWithIssuer = JwtValidators.createDefaultWithIssuer(this.issuer);
        decoder.setJwtValidator(defaultWithIssuer);
        this.jwtDecoder = decoder;
    }

    /**
     * Generates a signed JWT access token for an authenticated user.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(this.expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(this.issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Decodes and validates a signed token.
     */
    public Jwt decodeToken(String token) {
        return this.jwtDecoder.decode(token);
    }

    public JwtDecoder getJwtDecoder() {
        return this.jwtDecoder;
    }

    public long getExpirationSeconds() {
        return this.expirationSeconds;
    }

    public String getIssuer() {
        return this.issuer;
    }
}
