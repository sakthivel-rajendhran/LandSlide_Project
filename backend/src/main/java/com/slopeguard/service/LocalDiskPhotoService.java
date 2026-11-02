package com.slopeguard.service;

import com.slopeguard.dto.ReportPhotoResponse;
import com.slopeguard.exception.FileStorageException;
import com.slopeguard.exception.InvalidFileException;
import com.slopeguard.exception.PhotoLimitExceededException;
import com.slopeguard.exception.ResourceNotFoundException;
import com.slopeguard.model.IncidentReport;
import com.slopeguard.model.ReportPhotoMetadata;
import com.slopeguard.repository.ReportPhotoRepository;
import com.slopeguard.util.ImageValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Service managing incident photograph disk storage, database metadata persistence,
 * and authorized retrieval with transactional compensation.
 * <p>
 * Security Policy:
 * 1. Validates magic bytes (JPEG, PNG, WebP) without trusting client headers.
 * 2. Writes files to a dedicated local directory using randomized UUID filenames.
 * 3. Enforces strict report ownership or ROLE_ADMIN before upload or retrieval.
 * 4. Resolves and normalizes paths to completely prevent path traversal vulnerabilities.
 * 5. Calculates SHA-256 integrity checksums.
 * 6. Compensation: Removes disk files if database persistence fails.
 */
@Service
public class LocalDiskPhotoService implements PhotoService {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskPhotoService.class);

    private final ReportService reportService;
    private final ReportPhotoRepository photoRepository;
    private final Path uploadPath;
    private final long maxFileSizeBytes;
    private final int maxPhotosPerReport;

    public LocalDiskPhotoService(
            ReportService reportService,
            ReportPhotoRepository photoRepository,
            @Value("${slopeguard.upload.dir:./data/uploads}") String uploadDir,
            @Value("${slopeguard.upload.max-file-size:5242880}") long maxFileSizeBytes,
            @Value("${slopeguard.upload.max-photos-per-report:5}") int maxPhotosPerReport) {
        this.reportService = reportService;
        this.photoRepository = photoRepository;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxPhotosPerReport = maxPhotosPerReport;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadPath);
            log.info("Initialized local photo storage directory: {}", uploadPath);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize upload storage directory at: " + uploadPath, e);
        }
    }

    @Override
    @Transactional
    public ReportPhotoResponse uploadPhoto(Long reportId, MultipartFile file, Long uploaderUserId, boolean isAdmin) {
        IncidentReport report = reportService.getReportEntity(reportId);

        // Enforce ownership: only the author citizen or an administrator can upload photos
        verifyReportOwnershipOrAdmin(report, uploaderUserId, isAdmin, "upload photos for");

        long currentCount = photoRepository.countByReportId(reportId);
        if (currentCount >= maxPhotosPerReport) {
            throw new PhotoLimitExceededException(
                    "Maximum limit of " + maxPhotosPerReport + " photographs reached for report #" + reportId + ".");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty. Please provide a valid photograph.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException(
                    "File size (" + file.getSize() + " bytes) exceeds maximum allowable limit of "
                            + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        // Validate image magic bytes and integrity
        ImageValidator.ValidatedImage validated = ImageValidator.validate(file);

        // Generate random, secure storage filename
        String photoId = UUID.randomUUID().toString();
        String storageFilename = photoId + validated.getExtension();

        Path destination = uploadPath.resolve(storageFilename).normalize();
        if (!destination.startsWith(uploadPath)) {
            throw new SecurityException("Potential directory traversal exploit attempt detected.");
        }

        // Compute SHA-256 checksum while copying
        String checksum;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(in, md)) {
                Files.copy(dis, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            checksum = HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            // Delete incomplete file if write failed
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {}
            throw new FileStorageException("Failed to store uploaded photograph to disk: " + e.getMessage(), e);
        }

        // Persist metadata to database with compensation on failure
        ReportPhotoMetadata metadata = new ReportPhotoMetadata(
                photoId,
                reportId,
                storageFilename,
                file.getOriginalFilename(),
                validated.getContentType(),
                file.getSize(),
                checksum,
                uploaderUserId
        );

        try {
            ReportPhotoMetadata saved = photoRepository.save(metadata);
            log.info("Successfully stored and persisted photograph {} for report #{}", photoId, reportId);
            return mapToResponse(saved);
        } catch (Exception ex) {
            // Compensation: Clean up disk file so no orphaned physical files remain
            log.error("Database persistence failed for photo {}. Executing compensation file cleanup.", photoId, ex);
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ioEx) {
                log.error("Failed to delete orphaned file {} during compensation: {}", destination, ioEx.getMessage());
            }
            throw new FileStorageException("Failed to persist photograph metadata to database: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportPhotoResponse> getPhotosForReport(Long reportId, Long requesterUserId, boolean isAdmin) {
        IncidentReport report = reportService.getReportEntity(reportId);
        verifyReportOwnershipOrAdmin(report, requesterUserId, isAdmin, "view photos of");

        List<ReportPhotoMetadata> list = photoRepository.findByReportIdOrderByUploadedAtAsc(reportId);
        List<ReportPhotoResponse> responses = new ArrayList<>();
        for (ReportPhotoMetadata m : list) {
            responses.add(mapToResponse(m));
        }
        return Collections.unmodifiableList(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadPhotoResource(Long reportId, String photoId, Long requesterUserId, boolean isAdmin) {
        IncidentReport report = reportService.getReportEntity(reportId);
        verifyReportOwnershipOrAdmin(report, requesterUserId, isAdmin, "access photo of");

        ReportPhotoMetadata metadata = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photograph with ID " + photoId + " not found for report #" + reportId));

        if (!metadata.getReportId().equals(reportId)) {
            throw new ResourceNotFoundException("Photograph with ID " + photoId + " does not belong to report #" + reportId);
        }

        Path filePath = uploadPath.resolve(metadata.getStorageFilename()).normalize();
        if (!filePath.startsWith(uploadPath) || !Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new ResourceNotFoundException("Photograph file was not found on server storage: " + photoId);
        }

        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new FileStorageException("Error loading photograph file resource: " + photoId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getPhotoContentType(Long reportId, String photoId, Long requesterUserId, boolean isAdmin) {
        IncidentReport report = reportService.getReportEntity(reportId);
        verifyReportOwnershipOrAdmin(report, requesterUserId, isAdmin, "access photo content type of");

        ReportPhotoMetadata metadata = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photograph with ID " + photoId + " not found for report #" + reportId));

        return metadata.getContentType();
    }

    private void verifyReportOwnershipOrAdmin(IncidentReport report, Long requesterUserId, boolean isAdmin, String action) {
        if (isAdmin) {
            return;
        }
        if (requesterUserId == null || report.getReporterUserId() == null
                || !report.getReporterUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("Access is denied: You do not have permission to " + action + " this incident report.");
        }
    }

    public static ReportPhotoResponse mapToResponse(ReportPhotoMetadata metadata) {
        String downloadUrl = "/api/reports/" + metadata.getReportId() + "/photos/" + metadata.getId();
        return new ReportPhotoResponse(
                metadata.getId(),
                metadata.getReportId(),
                metadata.getContentType(),
                metadata.getFileSize(),
                metadata.getUploadedAt(),
                downloadUrl
        );
    }
}
