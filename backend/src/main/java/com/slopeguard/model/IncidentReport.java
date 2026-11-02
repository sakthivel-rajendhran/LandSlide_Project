package com.slopeguard.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a citizen landslide incident report.
 * Persisted in MySQL database via Spring Data JPA.
 */
@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_user_id")
    private Long reporterUserId;

    @Column(name = "reporter_name", length = 150)
    private String reporterName;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "location_description")
    private String locationDescription;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "incident_category", nullable = false, length = 50)
    private IncidentCategory incidentCategory;

    @Column(name = "reported_timestamp", nullable = false)
    private LocalDateTime reportedTimestamp;

    @Column(name = "observation_timestamp")
    private LocalDateTime observationTimestamp;

    @Column(name = "environmental_observations", columnDefinition = "TEXT")
    private String environmentalObservations;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "report_id", insertable = false, updatable = false)
    private List<ReportPhotoMetadata> photos = new ArrayList<>();

    public IncidentReport() {
        this.status = ReportStatus.PENDING;
        this.reportedTimestamp = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.statusUpdatedAt = LocalDateTime.now();
    }

    public IncidentReport(Long id, String reporterName, String description, Double latitude, Double longitude,
                          String locationDescription, IncidentCategory incidentCategory,
                          LocalDateTime reportedTimestamp) {
        this(id, null, reporterName, description, latitude, longitude, locationDescription, incidentCategory, reportedTimestamp);
    }

    public IncidentReport(Long id, Long reporterUserId, String reporterName, String description, Double latitude, Double longitude,
                          String locationDescription, IncidentCategory incidentCategory,
                          LocalDateTime reportedTimestamp) {
        this.id = id;
        this.reporterUserId = reporterUserId;
        this.reporterName = reporterName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationDescription = locationDescription;
        this.incidentCategory = incidentCategory;
        this.reportedTimestamp = (reportedTimestamp != null) ? reportedTimestamp : LocalDateTime.now();
        this.status = ReportStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
        this.statusUpdatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.reportedTimestamp == null) {
            this.reportedTimestamp = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public IncidentCategory getIncidentCategory() {
        return incidentCategory;
    }

    public void setIncidentCategory(IncidentCategory incidentCategory) {
        this.incidentCategory = incidentCategory;
    }

    public LocalDateTime getReportedTimestamp() {
        return reportedTimestamp;
    }

    public void setReportedTimestamp(LocalDateTime reportedTimestamp) {
        this.reportedTimestamp = reportedTimestamp;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        this.statusUpdatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt != null ? statusUpdatedAt : updatedAt;
    }

    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getObservationTimestamp() {
        return observationTimestamp;
    }

    public void setObservationTimestamp(LocalDateTime observationTimestamp) {
        this.observationTimestamp = observationTimestamp;
    }

    public String getEnvironmentalObservations() {
        return environmentalObservations;
    }

    public void setEnvironmentalObservations(String environmentalObservations) {
        this.environmentalObservations = environmentalObservations;
    }

    public List<ReportPhotoMetadata> getPhotos() {
        return photos;
    }

    public void setPhotos(List<ReportPhotoMetadata> photos) {
        this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "IncidentReport{" +
                "id=" + id +
                ", reporterName='" + reporterName + '\'' +
                ", incidentCategory=" + incidentCategory +
                ", locationDescription='" + locationDescription + '\'' +
                ", status=" + status +
                ", reportedTimestamp=" + reportedTimestamp +
                '}';
    }
}
