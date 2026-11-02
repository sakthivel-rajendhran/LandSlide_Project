package com.slopeguard.controller;

import com.slopeguard.dto.ReportCreateRequest;
import com.slopeguard.model.IncidentCategory;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhotoUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ReportService reportService;

    private byte[] createSampleImageBytes(String format) throws Exception {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("1. Valid PNG photograph upload attaches to report and returns 201 Created")
    void uploadPhoto_whenValidPng_shouldSucceed() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Photo Uploader",
                "photo.uploader@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Erosion ravine expanding along the perimeter embankment.",
                25.6,
                91.8,
                "Perimeter embankment",
                IncidentCategory.EROSION,
                null
        ), citizen.getId(), citizen.getFullName());

        byte[] pngBytes = createSampleImageBytes("png");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "slope_crack.png",
                "image/png",
                pngBytes
        );

        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.reportId", is(report.getId().intValue())))
                .andExpect(jsonPath("$.contentType", is("image/png")))
                .andExpect(jsonPath("$.downloadUrl", containsString("/api/reports/" + report.getId() + "/photos/")));
    }

    @Test
    @DisplayName("2. Valid JPEG photograph upload attaches to report")
    void uploadPhoto_whenValidJpeg_shouldSucceed() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Jpeg Uploader",
                "jpeg.uploader@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Active rockfall photos attached for technical verification.",
                25.7,
                91.9,
                "Hill curve 5",
                IncidentCategory.ROCKFALL,
                null
        ), citizen.getId(), citizen.getFullName());

        byte[] jpgBytes = createSampleImageBytes("jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rockfall.jpg",
                "image/jpeg",
                jpgBytes
        );

        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType", is("image/jpeg")));
    }

    @Test
    @DisplayName("3. Rejection of empty file upload with 400 Bad Request")
    void uploadPhoto_whenEmptyFile_shouldReturn400() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Empty Uploader",
                "empty.uploader@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Testing empty photo upload rejection handling.",
                25.7,
                91.9,
                "Test sector",
                IncidentCategory.SLOPE_CRACK,
                null
        ), citizen.getId(), citizen.getFullName());

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("empty")));
    }

    @Test
    @DisplayName("4. Rejection of unsupported or spoofed image types (e.g. text file renamed to .png)")
    void uploadPhoto_whenSpoofedImage_shouldReturn400() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Spoof Uploader",
                "spoof.uploader@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Testing spoofed image file rejection handling.",
                25.7,
                91.9,
                "Test sector",
                IncidentCategory.SLOPE_CRACK,
                null
        ), citizen.getId(), citizen.getFullName());

        // Text file content disguised with PNG extension
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "THIS_IS_NOT_AN_IMAGE_FILE_PAYLOAD".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(spoofedFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Unsupported or corrupted image file format")));
    }

    @Test
    @DisplayName("5. Enforce maximum photo count limit per report (rejecting beyond 5)")
    void uploadPhoto_whenExceedsMaxPhotos_shouldReturn400() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Quota Uploader",
                "quota.uploader@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Testing maximum photo count limit per report.",
                25.7,
                91.9,
                "Test sector",
                IncidentCategory.SLOPE_CRACK,
                null
        ), citizen.getId(), citizen.getFullName());

        byte[] pngBytes = createSampleImageBytes("png");

        // Upload 5 allowed photos
        for (int i = 1; i <= 5; i++) {
            MockMultipartFile file = new MockMultipartFile("file", "photo" + i + ".png", "image/png", pngBytes);
            mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }

        // Attempt 6th photo upload - should be rejected with 400
        MockMultipartFile sixthFile = new MockMultipartFile("file", "photo6.png", "image/png", pngBytes);
        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(sixthFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Maximum limit of 5 photographs reached")));
    }

    @Test
    @DisplayName("6. Unauthorized photo access and upload rejected (403 Forbidden)")
    void uploadAndAccessPhoto_whenUnauthorizedCitizen_shouldReturn403() throws Exception {
        User owner = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Photo Owner",
                "photo.owner@example.org",
                "StrongPass123!Secure",
                null
        ));
        User attacker = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Photo Attacker",
                "photo.attacker@example.org",
                "StrongPass123!Secure",
                null
        ));

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Private report with attached media.",
                25.7,
                91.9,
                "Private location",
                IncidentCategory.ROCKFALL,
                null
        ), owner.getId(), owner.getFullName());

        String attackerToken = jwtService.generateToken(attacker);

        // Attacker attempts upload to owner's report
        MockMultipartFile file = new MockMultipartFile("file", "attack.png", "image/png", createSampleImageBytes("png"));
        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // Attacker attempts listing photos
        mockMvc.perform(get("/api/reports/" + report.getId() + "/photos")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("7. Safe photo binary retrieval with correct Content-Type")
    void downloadPhoto_whenAuthorized_shouldStreamImage() throws Exception {
        User citizen = userService.registerCitizen(new com.slopeguard.dto.RegisterRequest(
                "Download Citizen",
                "download.citizen@example.org",
                "StrongPass123!Secure",
                null
        ));
        String token = jwtService.generateToken(citizen);

        var report = reportService.createReport(new ReportCreateRequest(
                null,
                "Report with downloadable photograph attachment.",
                25.7,
                91.9,
                "Download test area",
                IncidentCategory.SLOPE_CRACK,
                null
        ), citizen.getId(), citizen.getFullName());

        byte[] pngBytes = createSampleImageBytes("png");
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", pngBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/reports/" + report.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        String location = uploadResult.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(location);

        // Download and verify content type
        mockMvc.perform(get(location)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().exists("Content-Disposition"));
    }
}
