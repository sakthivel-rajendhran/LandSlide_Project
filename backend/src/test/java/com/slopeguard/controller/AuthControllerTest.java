package com.slopeguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slopeguard.dto.LoginRequest;
import com.slopeguard.dto.RegisterRequest;
import com.slopeguard.model.User;
import com.slopeguard.model.UserRole;
import com.slopeguard.service.JwtService;
import com.slopeguard.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. Successful citizen registration assigns CITIZEN role and hides password hash")
    void register_whenValidCitizen_shouldCreateAccount() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Tenzing Norbu",
                "tenzing.norbu@example.org",
                "StrongPass123!Safe",
                "+919876543210"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName", is("Tenzing Norbu")))
                .andExpect(jsonPath("$.email", is("tenzing.norbu@example.org")))
                .andExpect(jsonPath("$.role", is("CITIZEN")))
                .andExpect(jsonPath("$.message", containsString("successfully")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        // 4. Verify response body text never leaks password or hash substring
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("StrongPass123!Safe"));
        assertFalse(body.contains("$2a$"));
    }

    @Test
    @DisplayName("2. Duplicate email registration is rejected with 400 Bad Request")
    void register_whenDuplicateEmail_shouldRejectWith400() throws Exception {
        RegisterRequest request1 = new RegisterRequest(
                "Duplicate Citizen",
                "duplicate.check@example.org",
                "SafePassword123!",
                null
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Attempt second registration with same email
        RegisterRequest request2 = new RegisterRequest(
                "Another Name",
                "duplicate.check@example.org",
                "AnotherPassword123!",
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("3. Passwords must be hashed with BCrypt and not stored in plaintext")
    void register_shouldHashPasswordWithBcrypt() {
        String email = "hashing.test@example.org";
        String rawPassword = "TestPassword1234!";

        RegisterRequest request = new RegisterRequest("Hash Tester", email, rawPassword, null);
        User user = userService.registerCitizen(request);

        // Verification: stored password hash is not plaintext
        assertNotEquals(rawPassword, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("WrongPassword123!", user.getPasswordHash()));
    }

    @Test
    @DisplayName("4. Citizen cannot self-assign the ADMIN role during registration")
    void register_cannotEscalateRoleToAdmin() throws Exception {
        // Exploit payload attempting to inject role=ADMIN
        String exploitJson = """
                {
                    "fullName": "Malicious User",
                    "email": "exploit.role@example.org",
                    "password": "SecretPassword123!",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exploitJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("CITIZEN")));

        Optional<User> createdUser = userService.findByEmail("exploit.role@example.org");
        assertTrue(createdUser.isPresent());
        assertTrue(createdUser.get().getRole() == UserRole.CITIZEN);
    }

    @Test
    @DisplayName("5. Successful login returns signed JWT access token and user claims")
    void login_whenValidCredentials_shouldReturnToken() throws Exception {
        String email = "login.success@example.org";
        String rawPass = "ValidPassword123!";
        userService.registerCitizen(new RegisterRequest("Login Success", email, rawPass, null));

        LoginRequest loginRequest = new LoginRequest(email, rawPass);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("CITIZEN")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertFalse(responseContent.contains(rawPass));
    }

    @Test
    @DisplayName("6. Failed login returns 401 Unauthorized without revealing email existence")
    void login_whenInvalidCredentials_shouldReturn401() throws Exception {
        // Non-existent email
        LoginRequest unknownEmail = new LoginRequest("unknown.user@example.org", "RandomPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));

        // Existing email with wrong password
        String email = "existing.user@example.org";
        userService.registerCitizen(new RegisterRequest("Existing", email, "RightPassword123!", null));

        LoginRequest wrongPass = new LoginRequest(email, "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }

    @Test
    @DisplayName("7. GET /api/auth/me requires valid bearer token; returns 401 when missing")
    void getMe_whenMissingToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("8. GET /api/auth/me rejects invalid bearer token with 401")
    void getMe_whenInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.jwtToken")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("9. GET /api/auth/me with valid bearer token returns user profile")
    void getMe_whenValidToken_shouldReturnProfile() throws Exception {
        String email = "me.profile@example.org";
        User user = userService.registerCitizen(new RegisterRequest("Profile Citizen", email, "SecurePassword123!", "+919999988888"));
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.fullName", is("Profile Citizen")))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("CITIZEN")))
                .andExpect(jsonPath("$.phoneNumber", is("+919999988888")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("10. POST /api/auth/logout acknowledges client token discard")
    void logout_shouldAcknowledgeStatelessDiscard() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateless", is(true)))
                .andExpect(jsonPath("$.tokenRevoked", is(false)));
    }
}
