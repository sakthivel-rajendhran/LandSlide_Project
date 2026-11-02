package com.slopeguard.controller;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.User;
import com.slopeguard.model.UserRole;
import com.slopeguard.service.JwtService;
import com.slopeguard.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Value("${slopeguard.security.jwt.secret}")
    private String jwtSecret;

    @Value("${slopeguard.security.jwt.issuer:slopeguard-backend}")
    private String jwtIssuer;

    @Test
    @DisplayName("1. Expired JWT token is rejected with 401 Unauthorized")
    void getMe_whenExpiredToken_shouldReturn401() throws Exception {
        // Construct an expired token using the active signing secret
        SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        Instant past = Instant.now().minusSeconds(1800);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(past.minusSeconds(900))
                .expiresAt(past)
                .subject("99")
                .claim("userId", 99L)
                .claim("email", "expired.token@example.org")
                .claim("role", "CITIZEN")
                .build();

        String expiredToken = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("2. Citizen role is denied access to admin reports (403 Forbidden)")
    void adminEndpoints_whenCitizenRole_shouldReturn403Forbidden() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Citizen Tester",
                "citizen.access.denied@example.org",
                "CitizenPass123!Test",
                null
        ));
        String citizenToken = jwtService.generateToken(citizen);

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + citizenToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", containsString("insufficient role privileges")));
    }

    @Test
    @DisplayName("3. Unauthenticated request to admin reports returns 401 Unauthorized")
    void adminEndpoints_whenUnauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("4. Administrator role is allowed access to administrative reports")
    void adminEndpoints_whenAdminRole_shouldReturn200Ok() throws Exception {
        User admin = userService.provisionAdmin(
                "Super Admin",
                "admin.authorized@example.org",
                "AdminPassword123!Secure"
        );
        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. Unauthenticated user cannot submit report (401 Unauthorized)")
    void submitReport_whenUnauthenticated_shouldReturn401() throws Exception {
        String reportJson = """
                {
                    "description": "Crack on slope without authentication",
                    "latitude": 25.5,
                    "longitude": 91.8,
                    "locationDescription": "Shillong slope",
                    "incidentCategory": "SLOPE_CRACK"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("6. Authenticated citizen can submit report (201 Created)")
    void submitReport_whenAuthenticatedCitizen_shouldReturn201() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Submitting Citizen",
                "submitting.citizen@example.org",
                "StrongCitizenPass123!",
                null
        ));
        String token = jwtService.generateToken(citizen);

        String reportJson = """
                {
                    "description": "Active rockfall observed along the road cutting after rainfall.",
                    "latitude": 25.5788,
                    "longitude": 91.8933,
                    "locationDescription": "Mawlai link road",
                    "incidentCategory": "ROCKFALL"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.incidentCategory", is("ROCKFALL")));
    }



    @Test
    @DisplayName("8. Public reports list remains accessible without authentication")
    void publicReports_remainAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("9. CORS preflight request for configured origin is allowed")
    void corsPreflight_forAllowedOrigin_shouldReturn200() throws Exception {
        mockMvc.perform(options("/api/reports")
                        .header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"));
    }
}
