package com.slopeguard.controller;

import com.slopeguard.dto.ApiInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller providing discovery information and route catalog for the SlopeGuard MVP REST API.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final List<String> ENDPOINTS = List.of(
            "GET   /api - API discovery and service metadata",
            "GET   /api/health - System health check",
            "POST  /api/auth/register - Citizen user account registration (assigns CITIZEN role)",
            "POST  /api/auth/login - User authentication; issues signed JWT bearer token",
            "GET   /api/auth/me - Authenticated user profile and role verification [Requires Bearer Token]",
            "POST  /api/auth/logout - Client token discard acknowledgement",
            "PUT   /api/users/me - Update authenticated user full name [Requires Bearer Token]",
            "PUT   /api/users/me/password - Change authenticated user password [Requires Bearer Token]",
            "GET   /api/public/alerts - Active published official hazard advisories [Public]",
            "POST  /api/reports - Submit a citizen landslide hazard incident report [Requires Bearer Token]",
            "GET   /api/reports - List recent incident reports (public privacy-filtered view)",
            "GET   /api/reports/mine - List incident reports submitted by authenticated citizen [Requires Bearer Token]",
            "GET   /api/reports/{id} - Get report details (enforces citizen ownership or ADMIN) [Requires Bearer Token]",
            "POST  /api/reports/{id}/photos - Upload geotagged incident photograph [Requires Bearer Token (Owner or Admin)]",
            "GET   /api/reports/{id}/photos - List photo attachments metadata [Requires Bearer Token (Owner or Admin)]",
            "GET   /api/reports/{id}/photos/{photoId} - Stream photo binary safely [Requires Bearer Token (Owner or Admin)]",
            "GET   /api/admin/reports - Administrative review queue with filtering and pagination [Requires ADMIN Role]",
            "GET   /api/admin/reports/{id} - Administrative report detail inspection [Requires ADMIN Role]",
            "GET   /api/admin/reports/pending - Filtered review queue for pending reports [Requires ADMIN Role]",
            "GET   /api/admin/reports/{id}/photos - Inspect associated photo attachments [Requires ADMIN Role]",
            "PATCH /api/admin/reports/{id}/status - Adjudicate report status with transition rules [Requires ADMIN Role]",
            "POST  /api/admin/reports/{id}/review - Submit administrative review notes [Requires ADMIN Role]",
            "GET   /api/admin/alerts - List early-warning alert advisories in the administrative workflow [Requires ADMIN Role]",
            "POST  /api/admin/alerts - Create a new alert draft [Requires ADMIN Role]",
            "GET   /api/admin/alerts/{id} - Get alert details by ID [Requires ADMIN Role]",
            "PUT   /api/admin/alerts/{id} - Update alert draft [Requires ADMIN Role]",
            "POST  /api/admin/alerts/{id}/publish - Publish official hazard advisory to public feed [Requires ADMIN Role]",
            "POST  /api/admin/alerts/{id}/resolve - Resolve published hazard advisory [Requires ADMIN Role]",
            "POST  /api/admin/alerts/{id}/withdraw - Withdraw public advisory [Requires ADMIN Role]",
            "GET   /api/rescue/missions - List assigned missions for authenticated Rescue Force team [Requires RESCUE_FORCE Role]",
            "POST  /api/rescue/missions/{id}/assign - Assign incident report to Rescue Force team [Requires ADMIN Role]",
            "POST  /api/rescue/missions/{id}/decision - Accept or decline assigned mission [Requires RESCUE_FORCE Role]",
            "POST  /api/rescue/missions/{id}/start-journey - Update mission status to En Route [Requires RESCUE_FORCE Role]",
            "POST  /api/rescue/missions/{id}/arrive - Mark arrival on scene [Requires RESCUE_FORCE Role]",
            "POST  /api/rescue/missions/{id}/progress - Submit on-site progress updates [Requires RESCUE_FORCE Role]",
            "POST  /api/rescue/missions/{id}/outcome - Submit final response outcome report for closure [Requires RESCUE_FORCE Role]"
    );

    @GetMapping
    public ResponseEntity<ApiInfoResponse> getApiInfo() {
        ApiInfoResponse info = new ApiInfoResponse(
                "SlopeGuard Backend API",
                "1.0.0-mvp",
                "SlopeGuard Minimum Viable Product: Incident Reporting, Official Alerts, and Rescue Coordination",
                "Landslide incident reporting, official alert management, and rescue coordination for Northeast India",
                ENDPOINTS
        );
        return ResponseEntity.ok(info);
    }
}
