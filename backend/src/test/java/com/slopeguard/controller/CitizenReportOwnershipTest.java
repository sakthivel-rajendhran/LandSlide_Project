package com.slopeguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.dto.ReportStatusUpdateRequest;
import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.ReportStatus;
import com.slopeguard.model.User;
import com.slopeguard.service.JwtService;
import com.slopeguard.service.ReportService;
import com.slopeguard.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CitizenReportOwnershipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. Authenticated citizen submits geotagged report with automatic owner assignment")
    void submitReport_whenAuthenticated_shouldAssignOwner() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Lobsang Gyatso",
                "lobsang.gyatso@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        ReportCreateRequest request = new ReportCreateRequest(
                null,
                "Tension crack propagating along the retaining slope above Tawang Monastery access road.",
                27.5861,
                91.8594,
                "Tawang Access Road Km 4",
                IncidentCategory.SLOPE_CRACK,
                null
        );
        request.setObservationTimestamp(LocalDateTime.now().minusHours(1));
        request.setEnvironmentalObservations("Continuous heavy monsoon showers for 36 hours");

        MvcResult result = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.incidentCategory", is("SLOPE_CRACK")))
                .andExpect(jsonPath("$.environmentalObservations", is("Continuous heavy monsoon showers for 36 hours")))
                .andReturn();

        // Verify entity recorded owner ID from JWT
        String location = result.getResponse().getHeader("Location");
        Long reportId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
        org.junit.jupiter.api.Assertions.assertEquals(citizen.getId(), reportService.getReportEntity(reportId).getReporterUserId());
    }

    @Test
    @DisplayName("2. Rejection of invalid coordinates and blank description")
    void submitReport_whenInvalidCoordinatesOrDescription_shouldReturn400() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Valid Citizen",
                "valid.citizen.val@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        // Latitude > 90
        String invalidCoords = """
                {
                    "description": "Valid description length for testing coordinates validation.",
                    "latitude": 99.5,
                    "longitude": 92.5,
                    "locationDescription": "Somewhere invalid",
                    "incidentCategory": "ROCKFALL"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCoords))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors").exists());

        // Blank description
        String blankDescription = """
                {
                    "description": "",
                    "latitude": 25.5,
                    "longitude": 92.5,
                    "locationDescription": "Somewhere",
                    "incidentCategory": "ROCKFALL"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankDescription))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("3. Rejection of observation timestamp implausibly in the future")
    void submitReport_whenFutureTimestamp_shouldReturn400() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Future Reporter",
                "future.reporter@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        ReportCreateRequest request = new ReportCreateRequest(
                null,
                "Valid description for testing future observation timestamp.",
                25.5,
                91.8,
                "Valid hill location",
                IncidentCategory.ROCKFALL,
                null
        );
        request.setObservationTimestamp(LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("cannot be in the future")));
    }

    @Test
    @DisplayName("4. Citizen retrieves their own reports via /api/reports/mine")
    void getMyReports_shouldReturnOnlyOwnerReports() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "My Reports Citizen",
                "my.reports@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        // Submit one report for this citizen
        ReportCreateRequest req = new ReportCreateRequest(
                null,
                "Mudslide blocking roadway terrace behind local high school.",
                25.67,
                94.10,
                "High School Terrace, Kohima",
                IncidentCategory.MUD_SLIDE,
                null
        );
        reportService.createReport(req, citizen.getId(), citizen.getFullName());

        mockMvc.perform(get("/api/reports/mine")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].description", containsString("Mudslide blocking roadway terrace")));
    }

    @Test
    @DisplayName("5. Citizen is denied access (403 Forbidden) to another citizen's private report")
    void getReportById_whenDifferentCitizen_shouldReturn403() throws Exception {
        User citizenA = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Citizen A",
                "citizen.a@example.org",
                "StrongPass123!Secure",
                null
        ));
        User citizenB = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Citizen B",
                "citizen.b@example.org",
                "StrongPass123!Secure",
                null
        ));

        // Citizen A submits a report
        ReportCreateRequest req = new ReportCreateRequest(
                null,
                "Private slope erosion observed behind Citizen A private boundary.",
                25.6,
                91.9,
                "Private property slope",
                IncidentCategory.EROSION,
                null
        );
        var report = reportService.createReport(req, citizenA.getId(), citizenA.getFullName());

        String tokenB = jwtService.generateToken(citizenB);

        // Citizen B attempts to access Citizen A's report
        mockMvc.perform(get("/api/reports/" + report.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("6. Administrator is granted access to another citizen's report")
    void getReportById_whenAdmin_shouldReturn200() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Citizen Author",
                "citizen.author@example.org",
                "StrongPass123!Secure",
                null
        ));
        User admin = userService.provisionAdmin(
                "Admin Reviewer",
                "admin.reviewer@example.org",
                "StrongPass123!Secure"
        );

        ReportCreateRequest req = new ReportCreateRequest(
                null,
                "Debris flow across road sector requiring administrative verification.",
                25.6,
                91.9,
                "Highway sector 12",
                IncidentCategory.DEBRIS_FLOW,
                null
        );
        var report = reportService.createReport(req, citizen.getId(), citizen.getFullName());

        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/reports/" + report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(report.getId().intValue())));
    }

    @Test
    @DisplayName("7. Citizen cannot change report status; rejected with 403 Forbidden")
    void updateReportStatus_whenCitizen_shouldReturn403() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Unauthorized Status Updater",
                "status.updater@example.org",
                "StrongPass123!Secure",
                null
        ));
        String citizenToken = jwtService.generateToken(citizen);

        ReportStatusUpdateRequest update = new ReportStatusUpdateRequest(ReportStatus.VERIFIED, "Trying to self-verify");

        mockMvc.perform(patch("/api/admin/reports/1/status")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("8. Administrator can review report (UNDER_REVIEW -> VERIFIED) and audit log is recorded")
    void updateReportStatus_whenAdmin_shouldTransitionAndAudit() throws Exception {
        User admin = userService.provisionAdmin(
                "Disaster Director",
                "disaster.director@example.org",
                "StrongAdminPass123!"
        );
        String adminToken = jwtService.generateToken(admin);

        // Transition from PENDING to UNDER_REVIEW
        ReportStatusUpdateRequest underReviewReq = new ReportStatusUpdateRequest(
                ReportStatus.UNDER_REVIEW,
                "Field investigation unit dispatched to site"
        );
        mockMvc.perform(patch("/api/admin/reports/2/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(underReviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UNDER_REVIEW")))
                .andExpect(jsonPath("$.reviewNotes", is("Field investigation unit dispatched to site")));

        // Transition from UNDER_REVIEW to VERIFIED
        ReportStatusUpdateRequest verifiedReq = new ReportStatusUpdateRequest(
                ReportStatus.VERIFIED,
                "Site survey completed. Hazard confirmed."
        );
        mockMvc.perform(patch("/api/admin/reports/2/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifiedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VERIFIED")));

        // Check audit log
        var audits = reportService.getAuditLogsForReport(2L);
        org.junit.jupiter.api.Assertions.assertFalse(audits.isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(ReportStatus.VERIFIED, audits.get(audits.size() - 1).getNewStatus());
    }
}
