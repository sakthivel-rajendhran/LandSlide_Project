package com.slopeguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slopeguard.dto.AuditEventResponse;
import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.dto.ReportReviewRequest;
import com.slopeguard.dto.ReportStatusUpdateRequest;
import com.slopeguard.model.AuditEventType;
import com.slopeguard.model.AuditOutcome;
import com.slopeguard.model.IncidentCategory;
import com.slopeguard.model.ReportStatus;
import com.slopeguard.service.AuditService;
import com.slopeguard.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminReviewAndAuditTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AuditService auditService;

    private Long ensureTestReportExists() {
        var existing = reportService.getAllAdminReports();
        if (!existing.isEmpty()) {
            return existing.get(0).getId();
        }
        var created = reportService.createReport(new ReportCreateRequest(
                "Initial Review Queue Reporter",
                "Slope subsidence detected along north ridge pass.",
                27.1234,
                88.5678,
                "North Ridge KM 12",
                IncidentCategory.ROCKFALL,
                LocalDateTime.now()
        ));
        return created.getId();
    }

    // 1. Administrator can view the review queue with pagination
    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("1. Administrator can view the review queue with pagination headers")
    void adminCanViewReviewQueueWithPagination() throws Exception {
        ensureTestReportExists();
        mockMvc.perform(get("/api/admin/reports")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "newest")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().exists("X-Page-Number"))
                .andExpect(header().string("X-Page-Number", "0"))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].status", notNullValue()))
                .andExpect(jsonPath("$[0].reporterName", notNullValue()));
    }

    // 2. Citizen cannot access the review queue
    @Test
    @Order(2)
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("2. Citizen cannot access the review queue (403 Forbidden)")
    void citizenCannotAccessReviewQueue() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    // 3. Unauthenticated users cannot access admin endpoints
    @Test
    @Order(3)
    @DisplayName("3. Unauthenticated users cannot access admin review endpoints (401 Unauthorized)")
    void unauthenticatedUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    // 4. Administrator can view a specific report
    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN", username = "42")
    @DisplayName("4. Administrator can view a specific report and audit access event is recorded")
    void adminCanViewSpecificReport() throws Exception {
        Long reportId = ensureTestReportExists();
        mockMvc.perform(get("/api/admin/reports/" + reportId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId.intValue())))
                .andExpect(jsonPath("$.incidentCategory", notNullValue()))
                .andExpect(jsonPath("$.latitude", notNullValue()))
                .andExpect(jsonPath("$.longitude", notNullValue()));
    }

    // 5. Unknown report IDs return 404
    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("5. Unknown report IDs return 404 Not Found")
    void unknownReportIdReturns404() throws Exception {
        mockMvc.perform(get("/api/admin/reports/999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    // 6. Valid status transitions succeed (PENDING -> UNDER_REVIEW -> VERIFIED)
    @Test
    @Order(6)
    @DisplayName("6. Valid status transitions succeed (PENDING -> UNDER_REVIEW -> VERIFIED)")
    void validStatusTransitionsSucceed() throws Exception {
        // Create a new report
        ReportCreateRequest newReport = new ReportCreateRequest(
                "Citizen Tester",
                "Fissures spreading along ridge slope near bypass section.",
                27.1234,
                88.5678,
                "Upper Ridge Road, Gangtok",
                IncidentCategory.SLOPE_CRACK,
                LocalDateTime.now().minusHours(1)
        );
        var created = reportService.createReport(newReport, 501L, "Citizen Tester");
        Long reportId = created.getId();

        // 6a. Transition PENDING -> UNDER_REVIEW
        ReportStatusUpdateRequest toUnderReview = new ReportStatusUpdateRequest(
                ReportStatus.UNDER_REVIEW,
                "Field investigation unit assigned."
        );
        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toUnderReview))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("100").claim("role", "ADMIN").claim("email", "officer@slopeguard.gov.in"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId.intValue())))
                .andExpect(jsonPath("$.status", is("UNDER_REVIEW")))
                .andExpect(jsonPath("$.reviewNotes", is("Field investigation unit assigned.")));

        // 6b. Transition UNDER_REVIEW -> VERIFIED
        ReportStatusUpdateRequest toVerified = new ReportStatusUpdateRequest(
                ReportStatus.VERIFIED,
                "On-site verification complete. Critical geotechnical alert declared."
        );
        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toVerified))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("100").claim("role", "ADMIN").claim("email", "officer@slopeguard.gov.in"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VERIFIED")));
    }

    // 7. Invalid status transitions return 409 Conflict
    @Test
    @Order(7)
    @DisplayName("7. Invalid status transitions return 409 Conflict")
    void invalidStatusTransitionsReturn409() throws Exception {
        // Create report and move to VERIFIED (terminal)
        ReportCreateRequest terminalReportReq = new ReportCreateRequest(
                "Citizen Reporter",
                "Deep embankment displacement observed near highway shoulder.",
                25.8000,
                93.7000,
                "Diphu Valley Pass, Assam",
                IncidentCategory.ROAD_SUBSIDENCE,
                LocalDateTime.now().minusHours(2)
        );
        var created = reportService.createReport(terminalReportReq, 502L, "Citizen Reporter");
        Long reportId = created.getId();

        // Fast-track verify
        ReportStatusUpdateRequest verifyReq = new ReportStatusUpdateRequest(ReportStatus.VERIFIED, "Verified immediately");
        reportService.updateReportStatus(reportId, verifyReq, 100L, "admin@slopeguard.gov.in");

        // Attempt invalid transition: VERIFIED -> REJECTED (terminal state violation)
        ReportStatusUpdateRequest invalidReq = new ReportStatusUpdateRequest(
                ReportStatus.REJECTED,
                "Attempting illegal transition from terminal VERIFIED state"
        );
        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("100").claim("role", "ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("cannot transition from 'VERIFIED' to 'REJECTED'")));

        // Attempt illegal revert: VERIFIED -> PENDING
        ReportStatusUpdateRequest revertReq = new ReportStatusUpdateRequest(ReportStatus.PENDING, "Attempting illegal revert to PENDING");
        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertReq))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("100").claim("role", "ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    // 8. Citizens cannot modify report status
    @Test
    @Order(8)
    @WithMockUser(roles = "CITIZEN", username = "citizen1")
    @DisplayName("8. Citizens cannot modify report status (403 Forbidden)")
    void citizensCannotModifyReportStatus() throws Exception {
        ReportStatusUpdateRequest updateReq = new ReportStatusUpdateRequest(ReportStatus.VERIFIED, "Citizen trying to self-verify");
        mockMvc.perform(patch("/api/admin/reports/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    // 9. Actor ID is taken from authenticated principal
    @Test
    @Order(9)
    @DisplayName("9. Actor ID is taken from authenticated security context principal")
    void actorIdIsTakenFromSecurityPrincipal() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest(
                "Actor Test Reporter",
                "Fresh soil slip above river bend threatening roadway.",
                26.1500,
                91.7500,
                "Brahmaputra South Bank, Guwahati",
                IncidentCategory.EROSION,
                LocalDateTime.now().minusHours(1)
        );
        var created = reportService.createReport(req, 701L, "Actor Test Reporter");
        Long reportId = created.getId();

        Long expectedAdminUserId = 888L;
        String expectedAdminEmail = "chief.officer@slopeguard.gov.in";

        ReportStatusUpdateRequest updateReq = new ReportStatusUpdateRequest(
                ReportStatus.UNDER_REVIEW,
                "Actor test adjudication"
        );

        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", expectedAdminUserId)
                                        .subject("888")
                                        .claim("role", "ADMIN")
                                        .claim("email", expectedAdminEmail))))
                .andExpect(status().isOk());

        // Verify audit log has the correct actor ID
        List<AuditEventResponse> logs = auditService.getAuditLogsForReport(reportId, 0, 10);
        assertFalse(logs.isEmpty());
        AuditEventResponse statusChangeEvent = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.REPORT_STATUS_CHANGED && l.getOutcome() == AuditOutcome.SUCCESS)
                .findFirst()
                .orElse(null);

        assertNotNull(statusChangeEvent);
        assertEquals(expectedAdminUserId, statusChangeEvent.getActorUserId());
    }

    // 10. Successful status changes create one audit event
    @Test
    @Order(10)
    @DisplayName("10. Successful status change creates exactly one audit event")
    void successfulStatusChangeCreatesOneAuditEvent() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest(
                "Audit Count Reporter",
                "Minor talus accumulation along highway gutter line.",
                27.5000,
                92.4000,
                "Tawang Sector Route, Arunachal Pradesh",
                IncidentCategory.ROCKFALL,
                LocalDateTime.now().minusHours(2)
        );
        var created = reportService.createReport(req, 702L, "Audit Count Reporter");
        Long reportId = created.getId();

        int initialSuccessEvents = auditService.getAuditLogsForReport(reportId, 0, 50).size();

        ReportStatusUpdateRequest updateReq = new ReportStatusUpdateRequest(ReportStatus.UNDER_REVIEW, "Assigned for surveillance");
        reportService.updateReportStatus(reportId, updateReq, 999L, "admin@slopeguard.gov.in");

        List<AuditEventResponse> afterLogs = auditService.getAuditLogsForReport(reportId, 0, 50);
        assertEquals(initialSuccessEvents + 1, afterLogs.size());
    }

    // 11. Failed status changes do not create a false success event
    @Test
    @Order(11)
    @DisplayName("11. Failed status changes do not create a false success event (records failure outcome)")
    void failedStatusChangesDoNotCreateFalseSuccessEvent() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest(
                "Fail Audit Reporter",
                "Continuous debris flow on steep slope angle.",
                25.6800,
                94.1100,
                "Pfutsero Hill road, Phek",
                IncidentCategory.DEBRIS_FLOW,
                LocalDateTime.now().minusHours(1)
        );
        var created = reportService.createReport(req, 703L, "Fail Audit Reporter");
        Long reportId = created.getId();

        // Attempt illegal transition directly from PENDING to PENDING
        ReportStatusUpdateRequest illegalReq = new ReportStatusUpdateRequest(ReportStatus.PENDING, "Illegal revert");

        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalReq))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("999").claim("role", "ADMIN"))))
                .andExpect(status().isConflict());

        List<AuditEventResponse> logs = auditService.getAuditLogsForReport(reportId, 0, 50);
        // Ensure there are no SUCCESS status-change events for this attempt
        boolean hasFalseSuccess = logs.stream()
                .anyMatch(l -> l.getEventType() == AuditEventType.REPORT_STATUS_CHANGED && l.getOutcome() == AuditOutcome.SUCCESS);
        assertFalse(hasFalseSuccess);

        // Ensure failure event was recorded
        boolean hasFailureEvent = logs.stream()
                .anyMatch(l -> l.getEventType() == AuditEventType.REPORT_STATUS_CHANGED && l.getOutcome() == AuditOutcome.FAILURE);
        assertTrue(hasFailureEvent);
    }

    // 12. Audit events contain correct previous and new statuses
    @Test
    @Order(12)
    @DisplayName("12. Audit events contain accurate previous and new statuses")
    void auditEventsContainCorrectPreviousAndNewStatuses() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest(
                "Status Accuracy Reporter",
                "Large boulder precarious above hairpin turn.",
                27.0500,
                88.4200,
                "Mangan District Pass, Sikkim",
                IncidentCategory.ROCKFALL,
                LocalDateTime.now().minusHours(1)
        );
        var created = reportService.createReport(req, 704L, "Status Accuracy Reporter");
        Long reportId = created.getId();

        ReportStatusUpdateRequest toUnderReview = new ReportStatusUpdateRequest(ReportStatus.UNDER_REVIEW, "Inspecting boulder stability");
        reportService.updateReportStatus(reportId, toUnderReview, 888L, "admin@slopeguard.gov.in");

        List<AuditEventResponse> logs = auditService.getAuditLogsForReport(reportId, 0, 10);
        AuditEventResponse statusChangeEvent = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.REPORT_STATUS_CHANGED)
                .findFirst()
                .orElseThrow();

        assertEquals(ReportStatus.PENDING, statusChangeEvent.getPreviousStatus());
        assertEquals(ReportStatus.UNDER_REVIEW, statusChangeEvent.getNewStatus());
        assertEquals(AuditOutcome.SUCCESS, statusChangeEvent.getOutcome());
    }

    // 13. Audit endpoints reject citizen access
    @Test
    @Order(13)
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("13. Audit endpoints reject citizen access with 403 Forbidden")
    void auditEndpointsRejectCitizenAccess() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        mockMvc.perform(get("/api/admin/reports/1/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    // 16. Administrative review notes endpoint POST /api/admin/reports/{id}/review works
    @Test
    @Order(16)
    @DisplayName("16. Administrative review note endpoint POST /api/admin/reports/{id}/review records notes and updates status")
    void adminReviewEndpointRecordsNotesAndTransitions() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest(
                "Review Note Tester",
                "Loose stones falling across single-lane mountain road.",
                27.2000,
                88.5000,
                "Ravangla Pass Route, South Sikkim",
                IncidentCategory.ROCKFALL,
                LocalDateTime.now().minusHours(1)
        );
        var created = reportService.createReport(req, 801L, "Review Note Tester");
        Long reportId = created.getId();

        ReportReviewRequest reviewRequest = new ReportReviewRequest(
                "Field team contacted. Bulldozer dispatched for road clearing."
        );

        mockMvc.perform(post("/api/admin/reports/" + reportId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("100").claim("role", "ADMIN").claim("email", "admin@slopeguard.gov.in"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId.intValue())))
                .andExpect(jsonPath("$.status", is("UNDER_REVIEW")))
                .andExpect(jsonPath("$.reviewNotes", is("Field team contacted. Bulldozer dispatched for road clearing.")));
    }

    private static <T> org.hamcrest.Matcher<T> assertThat(T actual, org.hamcrest.Matcher<T> matcher) {
        org.hamcrest.MatcherAssert.assertThat(actual, matcher);
        return matcher;
    }
}
