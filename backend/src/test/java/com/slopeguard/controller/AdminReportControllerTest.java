package com.slopeguard.controller;

import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.dto.ReportResponse;
import com.slopeguard.model.IncidentCategory;
import com.slopeguard.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportService reportService;

    private Long testReportId;

    @BeforeEach
    void setUp() {
        ReportResponse report = reportService.createReport(new ReportCreateRequest(
                "Test Reporter",
                "Fissures spreading along ridge slope near bypass section.",
                27.1234,
                88.5678,
                "Ridge Bypass KM 14",
                IncidentCategory.ROCKFALL,
                LocalDateTime.now()
        ));
        testReportId = report.getId();
    }

    @Test
    @DisplayName("GET /api/admin/reports returns reports with administrative audit details")
    void getAdminReports_shouldReturnAdminDetails() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].status", notNullValue()))
                // In admin view, reporterName field exists
                .andExpect(jsonPath("$[0].locationDescription", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/admin/reports/{id}/status transitions status to VERIFIED")
    void updateReportStatus_whenValid_shouldUpdateStatus() throws Exception {
        String updatePayload = """
                {
                    "status": "VERIFIED",
                    "reviewNotes": "Inspected on-site by SDRF field unit. Retaining wall repair team dispatched."
                }
                """;

        mockMvc.perform(patch("/api/admin/reports/" + testReportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testReportId.intValue())))
                .andExpect(jsonPath("$.status", is("VERIFIED")))
                .andExpect(jsonPath("$.reviewNotes", is("Inspected on-site by SDRF field unit. Retaining wall repair team dispatched.")));
    }

    @Test
    @DisplayName("PATCH /api/admin/reports/{id}/status rejects invalid transition back to PENDING with 409 Conflict")
    void updateReportStatus_whenRevertingToPending_shouldReturn409() throws Exception {
        // Attempting to transition a report to PENDING is disallowed by business rules
        String invalidRevert = """
                {
                    "status": "PENDING",
                    "reviewNotes": "Attempting illegal transition to PENDING"
                }
                """;

        mockMvc.perform(patch("/api/admin/reports/" + testReportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRevert))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("PATCH /api/admin/reports/{id}/status with unknown ID returns 404")
    void updateReportStatus_whenNotFound_shouldReturn404() throws Exception {
        String updatePayload = """
                {
                    "status": "REJECTED",
                    "reviewNotes": "Unknown record"
                }
                """;

        mockMvc.perform(patch("/api/admin/reports/99999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("PATCH /api/admin/reports/{id}/status with null/empty body returns 400")
    void updateReportStatus_whenEmptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/admin/reports/" + testReportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }
}
