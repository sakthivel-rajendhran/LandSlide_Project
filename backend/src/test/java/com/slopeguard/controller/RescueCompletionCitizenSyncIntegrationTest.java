package com.slopeguard.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.dto.RescueMissionAssignRequest;
import com.slopeguard.model.*;
import com.slopeguard.repository.IncidentReportRepository;
import com.slopeguard.repository.RescueAssignmentRepository;
import com.slopeguard.repository.RescueTeamRepository;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RescueCompletionCitizenSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RescueTeamRepository rescueTeamRepository;

    @Autowired
    private RescueAssignmentRepository rescueAssignmentRepository;

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User citizen1;
    private User citizen2;
    private User admin;
    private User rescueUser1;
    private User rescueUser2;

    private RescueTeam rescueTeam1;
    private RescueTeam rescueTeam2;

    private String citizen1Token;
    private String citizen2Token;
    private String adminToken;
    private String rescueToken1;
    private String rescueToken2;

    @BeforeEach
    void setup() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // 1. Citizen 1
        citizen1 = new User();
        citizen1.setEmail("citizen1_" + uid + "@test.org");
        citizen1.setFullName("Citizen One");
        citizen1.setPasswordHash(passwordEncoder.encode("Pass@123456"));
        citizen1.setRole(UserRole.CITIZEN);
        citizen1 = userRepository.save(citizen1);
        citizen1Token = "Bearer " + jwtService.generateToken(citizen1);

        // 2. Citizen 2
        citizen2 = new User();
        citizen2.setEmail("citizen2_" + uid + "@test.org");
        citizen2.setFullName("Citizen Two");
        citizen2.setPasswordHash(passwordEncoder.encode("Pass@123456"));
        citizen2.setRole(UserRole.CITIZEN);
        citizen2 = userRepository.save(citizen2);
        citizen2Token = "Bearer " + jwtService.generateToken(citizen2);

        // 3. Admin
        admin = new User();
        admin.setEmail("admin_" + uid + "@slopeguard.org");
        admin.setFullName("Admin DMO");
        admin.setPasswordHash(passwordEncoder.encode("Pass@123456"));
        admin.setRole(UserRole.ADMIN);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        // 4. Rescue Team 1
        rescueUser1 = new User();
        rescueUser1.setEmail("rescue1_" + uid + "@slopeguard.org");
        rescueUser1.setFullName("Rescue Officer 1");
        rescueUser1.setPasswordHash(passwordEncoder.encode("Pass@123456"));
        rescueUser1.setRole(UserRole.RESCUE_FORCE);
        rescueUser1 = userRepository.save(rescueUser1);
        rescueToken1 = "Bearer " + jwtService.generateToken(rescueUser1);

        rescueTeam1 = new RescueTeam();
        rescueTeam1.setUser(rescueUser1);
        rescueTeam1.setTeamName("NDRF Unit Alpha " + uid);
        rescueTeam1.setTeamCategory("NATIONAL_FORCE");
        rescueTeam1.setTeamLeader("Captain Alpha");
        rescueTeam1.setContactNumber("+91 9876543210");
        rescueTeam1.setBaseAddress("District Headquarters");
        rescueTeam1.setOperatingDistrict("East District");
        rescueTeam1.setOperatingRegion("Northeast");
        rescueTeam1.setLatitude(27.33);
        rescueTeam1.setLongitude(88.61);
        rescueTeam1.setCapabilities(Set.of("SLOPE_STABILIZATION", "SEARCH_AND_RESCUE"));
        rescueTeam1.setVerificationStatus("APPROVED");
        rescueTeam1.setOperationalStatus("ACTIVE");
        rescueTeam1.setAvailability("AVAILABLE");
        rescueTeam1 = rescueTeamRepository.save(rescueTeam1);

        // 5. Rescue Team 2
        rescueUser2 = new User();
        rescueUser2.setEmail("rescue2_" + uid + "@slopeguard.org");
        rescueUser2.setFullName("Rescue Officer 2");
        rescueUser2.setPasswordHash(passwordEncoder.encode("Pass@123456"));
        rescueUser2.setRole(UserRole.RESCUE_FORCE);
        rescueUser2 = userRepository.save(rescueUser2);
        rescueToken2 = "Bearer " + jwtService.generateToken(rescueUser2);

        rescueTeam2 = new RescueTeam();
        rescueTeam2.setUser(rescueUser2);
        rescueTeam2.setTeamName("SDRF Unit Bravo " + uid);
        rescueTeam2.setTeamCategory("STATE_FORCE");
        rescueTeam2.setTeamLeader("Captain Bravo");
        rescueTeam2.setContactNumber("+91 9876543211");
        rescueTeam2.setBaseAddress("Secondary Post");
        rescueTeam2.setOperatingDistrict("East District");
        rescueTeam2.setOperatingRegion("Northeast");
        rescueTeam2.setLatitude(27.35);
        rescueTeam2.setLongitude(88.63);
        rescueTeam2.setCapabilities(Set.of("MEDICAL_EVACUATION"));
        rescueTeam2.setVerificationStatus("APPROVED");
        rescueTeam2.setOperationalStatus("ACTIVE");
        rescueTeam2.setAvailability("AVAILABLE");
        rescueTeam2 = rescueTeamRepository.save(rescueTeam2);
    }

    @Test
    @DisplayName("Complete end-to-end workflow: Submit report -> Assign team -> ACCEPTED -> EN_ROUTE -> ON_SCENE -> COMPLETED -> Citizen reflects completed rescue status")
    void testCompleteRescueWorkflowAndCitizenReflectsCompletedStatus() throws Exception {
        // Step 1: Citizen 1 submits an incident report
        ReportCreateRequest reportRequest = new ReportCreateRequest();
        reportRequest.setReporterName("Citizen One");
        reportRequest.setDescription("Active landslide blocking highway corridor");
        reportRequest.setLatitude(27.3389);
        reportRequest.setLongitude(88.6065);
        reportRequest.setLocationDescription("National Highway 10 Mile 12");
        reportRequest.setIncidentCategory(IncidentCategory.MUD_SLIDE);
        reportRequest.setReportedTimestamp(LocalDateTime.now());

        MvcResult createResult = mockMvc.perform(post("/api/reports")
                        .header("Authorization", citizen1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode createdReport = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reportId = createdReport.get("id").asLong();

        // Step 2: Admin assigns Rescue Team 1
        String assignPayload = objectMapper.writeValueAsString(Map.of(
                "reportId", reportId,
                "teamId", rescueTeam1.getId(),
                "priority", "HIGH",
                "instructions", "Deploy emergency debris clearance and inspect slope stability."
        ));

        MvcResult assignResult = mockMvc.perform(post("/api/admin/rescue-teams/assign")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ASSIGNED"))
                .andReturn();

        Long assignmentId = objectMapper.readTree(assignResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify Citizen report reflects ASSIGNED
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rescueStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.rescueTeamName").value(rescueTeam1.getTeamName()));

        // Step 3: Rescue Team 1 accepts assignment
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/accept")
                        .header("Authorization", rescueToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ACCEPTED"));

        // Verify Citizen report reflects ACCEPTED
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rescueStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.rescueAcceptedAt").isNotEmpty());

        // Step 4: Rescue Team 1 updates to EN_ROUTE
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/status")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "EN_ROUTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("EN_ROUTE"));

        // Verify Citizen report reflects RESPONDING / EN_ROUTE
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rescueStatus").value("EN_ROUTE"))
                .andExpect(jsonPath("$.status").value("RESPONDING"));

        // Step 5: Rescue Team 1 updates to ON_SCENE
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/status")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ON_SCENE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ON_SCENE"));

        // Verify Citizen report reflects ON_SCENE
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rescueStatus").value("ON_SCENE"));

        // Step 6: Rescue Team 1 marks assignment COMPLETED with remarks
        String completionRemarks = "Culverts cleared, road reopened for single-lane traffic. Slope movement arrested.";
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/complete")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", completionRemarks))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.responseNotes").value(completionRemarks));

        // Step 7: Log in as original Citizen and verify COMPLETED status in report details
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.rescueStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.rescueTeamName").value(rescueTeam1.getTeamName()))
                .andExpect(jsonPath("$.rescueCompletedAt").isNotEmpty())
                .andExpect(jsonPath("$.rescueNotes").value(completionRemarks))
                .andExpect(jsonPath("$.rescueAssignment.status").value("COMPLETED"))
                .andExpect(jsonPath("$.rescueAssignment.responseNotes").value(completionRemarks));

        // Step 8: Verify in Citizen's My Reports list (/api/reports/mine)
        mockMvc.perform(get("/api/reports/mine")
                        .header("Authorization", citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$[0].rescueStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[0].rescueTeamName").value(rescueTeam1.getTeamName()));
    }

    @Test
    @DisplayName("Security & Validation tests: unauthorized access, duplicate completion, missing notes, foreign citizen report access")
    void testSecurityAndValidationConstraints() throws Exception {
        // 1. Create report and assign
        ReportCreateRequest reportRequest = new ReportCreateRequest();
        reportRequest.setReporterName("Citizen One");
        reportRequest.setDescription("Boulder fall blocking side access road");
        reportRequest.setLatitude(27.34);
        reportRequest.setLongitude(88.62);
        reportRequest.setLocationDescription("Ridge Side Trail");
        reportRequest.setIncidentCategory(IncidentCategory.ROCKFALL);
        reportRequest.setReportedTimestamp(LocalDateTime.now());
        MvcResult repRes = mockMvc.perform(post("/api/reports")
                        .header("Authorization", citizen1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reportId = objectMapper.readTree(repRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult assignRes = mockMvc.perform(post("/api/admin/rescue-teams/assign")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reportId", reportId,
                                "teamId", rescueTeam1.getId(),
                                "priority", "MEDIUM"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Long assignmentId = objectMapper.readTree(assignRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Another rescue team (Team 2) attempting to complete Team 1's assignment -> 403 Forbidden
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/complete")
                        .header("Authorization", rescueToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", "Intruder attempting completion"))))
                .andExpect(status().isForbidden());

        // 3. Citizen attempting to complete rescue assignment -> 403 Forbidden
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/complete")
                        .header("Authorization", citizen1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", "Citizen attempting completion"))))
                .andExpect(status().isForbidden());

        // 4. Missing completion remarks -> 400 Bad Request
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/complete")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", "   "))))
                .andExpect(status().isBadRequest());

        // 5. Valid completion by assigned team
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/complete")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", "Operation complete"))))
                .andExpect(status().isOk());

        // 6. Duplicate completion request -> 400 Bad Request (IllegalStateException)
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/complete")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", "Trying again"))))
                .andExpect(status().isBadRequest());

        // 7. Non-existent assignment ID -> 404 Not Found
        mockMvc.perform(post("/api/rescue/assignments/999999/complete")
                        .header("Authorization", rescueToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responseNotes", "Missing ID"))))
                .andExpect(status().isNotFound());

        // 8. Citizen 2 attempting to view Citizen 1's report -> 403 Forbidden
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", citizen2Token))
                .andExpect(status().isForbidden());
    }
}
