package com.slopeguard.repository;

import com.slopeguard.model.ReportPhotoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ReportPhotoMetadata} entities.
 */
@Repository
public interface ReportPhotoRepository extends JpaRepository<ReportPhotoMetadata, String> {

    List<ReportPhotoMetadata> findByReportIdOrderByUploadedAtAsc(Long reportId);

    long countByReportId(Long reportId);
}
