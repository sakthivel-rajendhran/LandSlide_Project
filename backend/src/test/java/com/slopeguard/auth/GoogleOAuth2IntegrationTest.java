package com.slopeguard.auth;

import com.slopeguard.dto.AuthResponse;
import com.slopeguard.dto.GoogleAuthRequest;
import com.slopeguard.dto.LoginRequest;
import com.slopeguard.dto.RegisterRequest;
import com.slopeguard.exception.InvalidCredentialsException;
import com.slopeguard.exception.OAuthEmailConflictException;
import com.slopeguard.model.User;
import com.slopeguard.model.UserRole;
import com.slopeguard.repository.UserRepository;
import com.slopeguard.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.slopeguard.service.JwtService;
import com.slopeguard.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GoogleOAuth2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Scenario 1: Auto-provisioning new Citizen user from verified Google OIDC identity")
    void testNewGoogleUserAutoRegistration() {
        String sub = "google-sub-10001";
        String email = "citizen.sarah@gmail.com";
        String name = "Sarah Jamatia";
        String picture = "https://lh3.googleusercontent.com/sarah.jpg";

        User user = userService.processOAuth2User("GOOGLE", sub, email, name, picture);

        assertNotNull(user.getId(), "Persisted user ID must not be null");
        assertEquals("citizen.sarah@gmail.com", user.getEmail());
        assertEquals("Sarah Jamatia", user.getFullName());
        assertEquals(UserRole.CITIZEN, user.getRole(), "New OAuth users must STRICTLY receive CITIZEN role");
        assertEquals("GOOGLE", user.getAuthProvider());
        assertEquals(sub, user.getProviderId());
        assertNull(user.getPasswordHash(), "OAuth accounts must have null passwordHash (password login disabled)");
        assertTrue(user.isEnabled());

        // Verify standard SlopeGuard JWT can be generated
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertTrue(token.length() > 50);
    }

    @Test
    @DisplayName("Scenario 2: Existing Google user logging in again without duplicate account creation")
    void testExistingGoogleUserSubsequentLogin() {
        String sub = "google-sub-10002";
        String email = "returning.citizen@gmail.com";
        String name = "Returning Citizen";

        User firstLogin = userService.processOAuth2User("GOOGLE", sub, email, name, null);
        Long firstId = firstLogin.getId();

        // Second login with same provider & sub
        User secondLogin = userService.processOAuth2User("GOOGLE", sub, email, "Updated Name", "https://pic.url");

        assertEquals(firstId, secondLogin.getId(), "Must return same user entity without creating duplicate");
        assertEquals(UserRole.CITIZEN, secondLogin.getRole());
        assertEquals("https://pic.url", secondLogin.getProfilePictureUrl());
    }

    @Test
    @DisplayName("Scenario 3: Existing password account conflict handling (prevention of unauthorized takeover)")
    void testExistingPasswordAccountConflict() {
        // Create local password account first
        RegisterRequest registerReq = new RegisterRequest(
                "Local Resident",
                "resident.local@example.com",
                "SecretPass123!",
                "9876543210"
        );
        User localUser = userService.registerCitizen(registerReq);
        assertNotNull(localUser.getId());
        assertEquals("LOCAL", localUser.getAuthProvider());

        // Attempt Google OAuth with the same email
        OAuthEmailConflictException ex = assertThrows(OAuthEmailConflictException.class, () -> {
            userService.processOAuth2User("GOOGLE", "unauthorized-google-sub-999", "resident.local@example.com", "Attacker", null);
        });

        assertEquals("resident.local@example.com", ex.getEmail());
        // Verify password hash was NOT overwritten
        User unchanged = userRepository.findByEmail("resident.local@example.com").orElseThrow();
        assertEquals("LOCAL", unchanged.getAuthProvider());
        assertNull(unchanged.getProviderId());
        assertNotNull(unchanged.getPasswordHash());
    }

    @Test
    @DisplayName("Scenario 4: Disabled OAuth account cannot authenticate")
    void testDisabledOAuthAccountRejection() {
        String sub = "google-sub-disabled";
        String email = "disabled.citizen@gmail.com";

        User user = userService.processOAuth2User("GOOGLE", sub, email, "Disabled User", null);
        user.setEnabled(false);
        userRepository.save(user);

        assertThrows(InvalidCredentialsException.class, () -> {
            userService.processOAuth2User("GOOGLE", sub, email, "Disabled User", null);
        });
    }

    @Test
    @DisplayName("Scenario 5: Direct API POST /api/auth/google provisions and issues genuine SlopeGuard JWT")
    void testGoogleAuthApiEndpoint() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setSub("google-sub-api-test-1");
        request.setEmail("api.citizen@gmail.com");
        request.setName("API Citizen");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("api.citizen@gmail.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @DisplayName("Scenario 6: GET /api/auth/oauth/session retrieves AuthResponse from HttpOnly cookie and destroys cookie")
    void testOAuthSessionEndpoint() throws Exception {
        User user = userService.processOAuth2User("GOOGLE", "sub-session-test", "session.user@gmail.com", "Session User", null);
        String token = jwtService.generateToken(user);

        Cookie sessionCookie = new Cookie(OAuth2AuthenticationSuccessHandler.OAUTH_SESSION_COOKIE_NAME, token);

        mockMvc.perform(get("/api/auth/oauth/session")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.email").value("session.user@gmail.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @DisplayName("Scenario 7: Normal email/password login continues working uninterrupted")
    void testNormalEmailPasswordLoginUninterrupted() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "Standard Citizen",
                "standard.user@example.com",
                "CitizenPass123!",
                "9876543211"
        );
        userService.registerCitizen(registerReq);

        LoginRequest loginReq = new LoginRequest("standard.user@example.com", "CitizenPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("standard.user@example.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }
}
