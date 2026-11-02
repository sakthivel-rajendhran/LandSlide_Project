package com.slopeguard.service;

import com.slopeguard.dto.ReportPhotoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service contract for incident photograph uploads, authorization verification, and streaming.
 */
public interface PhotoService {

    /**
     * Uploads and attaches a validated photograph to an incident report.
     */
    ReportPhotoResponse uploadPhoto(Long reportId, MultipartFile file, Long uploaderUserId, boolean isAdmin);

    /**
     * Lists photograph metadata for an incident report, enforcing ownership checks.
     */
    List<ReportPhotoResponse> getPhotosForReport(Long reportId, Long requesterUserId, boolean isAdmin);

    /**
     * Streams the binary image resource from storage, enforcing ownership checks.
     */
    Resource loadPhotoResource(Long reportId, String photoId, Long requesterUserId, boolean isAdmin);

    /**
     * Returns the validated MIME content type of the stored image.
     */
    String getPhotoContentType(Long reportId, String photoId, Long requesterUserId, boolean isAdmin);
}
