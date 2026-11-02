package com.slopeguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slopeguard.dto.*;
import com.slopeguard.model.User;
import com.slopeguard.model.UserRole;
import com.slopeguard.repository.PasswordResetTokenRepository;
import com.slopeguard.repository.UserRepository;
import com.slopeguard.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RagulModule1WorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User adminUser;
    private User officerUser;
    private User regularUser;

    private String adminToken;
    private String officerToken;
    private String userToken;

    @BeforeEach
    void setupUsers() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = new User();
        adminUser.setEmail("admin_" + uniqueSuffix + "@slopeguard.org");
        adminUser.setFullName("Admin Officer");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPass123!Safe"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        officerUser = new User();
        officerUser.setEmail("officer_" + uniqueSuffix + "@slopeguard.org");
        officerUser.setFullName("Field Officer");
        officerUser.setPasswordHash(passwordEncoder.encode("OfficerPass123!Safe"));
        officerUser.setRole(UserRole.OFFICER);
        officerUser = userRepository.save(officerUser);
        officerToken = "Bearer " + jwtService.generateToken(officerUser);

        regularUser = new User();
        regularUser.setEmail("user_" + uniqueSuffix + "@slopeguard.org");
        regularUser.setFullName("Citizen User");
        regularUser.setPasswordHash(passwordEncoder.encode("UserPass123!Safe"));
        regularUser.setRole(UserRole.CITIZEN);
        regularUser = userRepository.save(regularUser);
        userToken = "Bearer " + jwtService.generateToken(regularUser);
    }

    @Test
    @DisplayName("Password Recovery Flow: Forgot Password and Reset Password")
    void forgotAndResetPasswordFlow() throws Exception {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(regularUser.getEmail());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If an account exists for this email address")));

        // Check token generated in repository
        var tokens = passwordResetTokenRepository.findAll();
        assertFalse(tokens.isEmpty());
        var tokenRecord = tokens.stream()
                .filter(t -> t.getUserId().equals(regularUser.getId()))
                .findFirst()
                .orElseThrow();

        // Reset password with token
        ResetPasswordRequest resetReq = new ResetPasswordRequest(tokenRecord.getTokenHash(), "BrandNewSecurePassword999!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Your password has been updated successfully")));

        // Verify user can login with new password
        LoginRequest loginReq = new LoginRequest(regularUser.getEmail(), "BrandNewSecurePassword999!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    @DisplayName("Public API: Overview, Data Sources, and Public Alerts isolation")
    void publicApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/public/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data_status", notNullValue()));

        mockMvc.perform(get("/api/public/data-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        mockMvc.perform(get("/api/public/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }



    @Test
    @DisplayName("Alert Workflow: Draft -> Submit -> Self-Approval Prevention (403) -> Approve -> Publish -> Public Check")
    void alertWorkflowAndSelfApprovalEnforcement() throws Exception {
        // Step 1: Officer creates a Draft alert
        WorkflowAlertRequest draftReq = new WorkflowAlertRequest(
                "Nilgiris Escarpment High Soil Moisture",
                "Nilgiris West Slope",
                null,
                "HIGH",
                "Continuous rainfall exceeded 150mm threshold; slope failure risk elevated.",
                "Evacuate vulnerable lower slope habitations and monitor culverts.",
                "REVIEWED_PLATFORM",
                "https://slopeguard.org/monitoring/nilgiris-west"
        );

        MvcResult createResult = mockMvc.perform(post("/api/admin/alerts")
                        .header("Authorization", officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.created_by", is(officerUser.getId().intValue())))
                .andReturn();

        WorkflowAlertResponse draft = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                WorkflowAlertResponse.class
        );

        // Step 2: Verify DRAFT is NOT visible in public alerts
        mockMvc.perform(get("/api/public/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + draft.getId() + "')]", empty()));

        // Step 3: Officer submits draft for review
        mockMvc.perform(post("/api/admin/alerts/" + draft.getId() + "/submit")
                        .header("Authorization", officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AlertTransitionRequest("Ready for review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_REVIEW")));



        // Step 5: Admin (different user) approves the alert
        mockMvc.perform(post("/api/admin/alerts/" + draft.getId() + "/approve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AlertTransitionRequest("Reviewed meteorological data and approved."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Step 6: Publish the approved alert
        mockMvc.perform(post("/api/admin/alerts/" + draft.getId() + "/publish")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AlertTransitionRequest("Published to citizen portal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")));

        // Step 7: Now the alert MUST appear in public alerts with proper source badge & review status
        mockMvc.perform(get("/api/public/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + draft.getId() + "')]", not(empty())))
                .andExpect(jsonPath("$[?(@.id == '" + draft.getId() + "')].source_type", hasItem("REVIEWED_PLATFORM")))
                .andExpect(jsonPath("$[?(@.id == '" + draft.getId() + "')].severity", hasItem("HIGH")));

        // Step 8: Check delivery rows exist
        mockMvc.perform(get("/api/admin/alerts/" + draft.getId() + "/delivery")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].channel", hasItem("WEB")));

        // Step 9: Resolve the alert
        mockMvc.perform(post("/api/admin/alerts/" + draft.getId() + "/resolve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AlertTransitionRequest("Rainfall subsided, soil moisture normalized."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));
    }
}
