package com.slopeguard.service;

import com.slopeguard.dto.AlertDeliveryResponse;
import com.slopeguard.dto.PublicAlertResponse;
import com.slopeguard.dto.PublicOverviewResponse;
import com.slopeguard.dto.WorkflowAlertRequest;
import com.slopeguard.dto.WorkflowAlertResponse;
import com.slopeguard.exception.InvalidStatusTransitionException;
import com.slopeguard.exception.ResourceNotFoundException;
import com.slopeguard.model.AlertDelivery;
import com.slopeguard.model.AlertReview;
import com.slopeguard.model.AuditEventType;
import com.slopeguard.model.AuditOutcome;
import com.slopeguard.model.WorkflowAlert;
import com.slopeguard.repository.AlertDeliveryRepository;
import com.slopeguard.repository.AlertReviewRepository;
import com.slopeguard.repository.WorkflowAlertRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Primary
public class JpaWorkflowAlertService implements WorkflowAlertService {

    private final WorkflowAlertRepository alertRepository;
    private final AlertReviewRepository reviewRepository;
    private final AlertDeliveryRepository deliveryRepository;
    private final AuditService auditService;

    public JpaWorkflowAlertService(WorkflowAlertRepository alertRepository,
                                  AlertReviewRepository reviewRepository,
                                  AlertDeliveryRepository deliveryRepository,
                                  AuditService auditService) {
        this.alertRepository = alertRepository;
        this.reviewRepository = reviewRepository;
        this.deliveryRepository = deliveryRepository;
        this.auditService = auditService;
    }



    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowAlertResponse> listAlerts(String status, String severity, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return alertRepository.searchAlerts(status, severity, pageRequest).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse createDraftAlert(WorkflowAlertRequest request, Long creatorUserId) {
        String alertId = "ALERT-WF-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        WorkflowAlert alert = new WorkflowAlert(
                alertId,
                request.getTitle().trim(),
                request.getAreaName().trim(),
                request.getAreaId(),
                request.getAssessmentId(),
                request.getSeverity().toUpperCase(),
                request.getDescription(),
                request.getRecommendedActions(),
                request.getSourceType() != null ? request.getSourceType().toUpperCase() : "REVIEWED_PLATFORM",
                request.getReferenceUrl(),
                "DRAFT",
                creatorUserId
        );

        WorkflowAlert saved = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(saved.getId(), "CREATE_DRAFT", creatorUserId, "Draft advisory prepared"));
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowAlertResponse getAlert(String id) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));
        return mapToResponse(alert);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse updateDraftAlert(String id, WorkflowAlertRequest request, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"DRAFT".equalsIgnoreCase(alert.getStatus()) && !"PENDING_REVIEW".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Only draft or pending alerts can be edited. Current status is " + alert.getStatus());
        }

        alert.setTitle(request.getTitle().trim());
        alert.setAreaName(request.getAreaName().trim());
        alert.setAreaId(request.getAreaId());
        alert.setAssessmentId(request.getAssessmentId());
        alert.setSeverity(request.getSeverity().toUpperCase());
        alert.setDescription(request.getDescription());
        alert.setRecommendedActions(request.getRecommendedActions());
        if (request.getSourceType() != null) alert.setSourceType(request.getSourceType().toUpperCase());
        alert.setReferenceUrl(request.getReferenceUrl());

        WorkflowAlert updated = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(updated.getId(), "EDIT_DRAFT", currentUserId, "Draft updated"));
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse submitAlert(String id, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"DRAFT".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Cannot submit alert from status '" + alert.getStatus() + "'. Allowed: DRAFT.");
        }

        alert.setStatus("PENDING_REVIEW");
        WorkflowAlert saved = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(saved.getId(), "SUBMIT", currentUserId, "Submitted for supervisor review"));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse approveAlert(String id, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"PENDING_REVIEW".equalsIgnoreCase(alert.getStatus()) && !"DRAFT".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Cannot approve alert from status '" + alert.getStatus() + "'. Allowed: PENDING_REVIEW, DRAFT.");
        }

        alert.setStatus("APPROVED");
        WorkflowAlert saved = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(saved.getId(), "APPROVE", currentUserId, "Alert approved for broadcast"));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse publishAlert(String id, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"APPROVED".equalsIgnoreCase(alert.getStatus()) && !"PENDING_REVIEW".equalsIgnoreCase(alert.getStatus()) && !"DRAFT".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Cannot publish alert from status '" + alert.getStatus() + "'. Allowed: APPROVED, PENDING_REVIEW, DRAFT.");
        }

        LocalDateTime now = LocalDateTime.now();
        alert.setStatus("PUBLISHED");
        alert.setPublishedAt(now);
        WorkflowAlert saved = alertRepository.save(alert);

        reviewRepository.save(new AlertReview(saved.getId(), "PUBLISH", currentUserId, "Public hazard alert broadcast activated"));

        // Delivery dispatch records
        deliveryRepository.save(new AlertDelivery(saved.getId(), "WEB", "PUBLIC_FEED", "DELIVERED", null));
        deliveryRepository.save(new AlertDelivery(saved.getId(), "PUSH", "SUBSCRIBED_CITIZENS", "DELIVERED", null));
        deliveryRepository.save(new AlertDelivery(saved.getId(), "SMS", "DISTRICT_OFFICIALS", "DELIVERED", null));

        auditService.recordEvent(
                AuditEventType.REPORT_REVIEWED,
                currentUserId,
                null,
                AuditOutcome.SUCCESS,
                "Published early warning advisory: " + saved.getTitle() + " (" + saved.getSeverity() + ")",
                null,
                null
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse rejectAlert(String id, String reason, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"PENDING_REVIEW".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Cannot reject alert from status '" + alert.getStatus() + "'. Allowed: PENDING_REVIEW.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        alert.setStatus("DRAFT");
        WorkflowAlert saved = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(saved.getId(), "REJECT", currentUserId, reason));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse resolveAlert(String id, String reason, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"PUBLISHED".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Cannot resolve alert from status '" + alert.getStatus() + "'. Allowed: PUBLISHED.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A resolution justification reason is required.");
        }

        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        WorkflowAlert saved = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(saved.getId(), "RESOLVE", currentUserId, reason));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WorkflowAlertResponse withdrawAlert(String id, String reason, Long currentUserId) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert record with ID '" + id + "' was not found."));

        if (!"PUBLISHED".equalsIgnoreCase(alert.getStatus())) {
            throw new InvalidStatusTransitionException("Cannot withdraw alert from status '" + alert.getStatus() + "'. Allowed: PUBLISHED.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A withdrawal justification reason is required.");
        }

        alert.setStatus("WITHDRAWN");
        alert.setResolvedAt(LocalDateTime.now());
        WorkflowAlert saved = alertRepository.save(alert);
        reviewRepository.save(new AlertReview(saved.getId(), "WITHDRAW", currentUserId, reason));
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDeliveryResponse> getDeliveries(String id) {
        return deliveryRepository.findByAlertIdOrderByAttemptedAtDesc(id).stream()
                .map(d -> new AlertDeliveryResponse(d.getId(), d.getAlertId(), d.getChannel(), d.getRecipientGroup(), d.getStatus(), d.getAttemptedAt(), d.getFailureReason()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicAlertResponse> getPublicAlerts(int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
        return alertRepository.findPublishedAlerts(pageable).stream()
                .map(this::mapToPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicAlertResponse getPublicAlert(String id) {
        WorkflowAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert with ID '" + id + "' was not found."));

        if (!"PUBLISHED".equalsIgnoreCase(alert.getStatus())) {
            throw new ResourceNotFoundException("Alert with ID '" + id + "' was not found.");
        }

        return mapToPublicResponse(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicOverviewResponse getPublicOverview() {
        long elevatedAlerts = alertRepository.countByStatusAndSeverityIn("PUBLISHED", List.of("HIGH", "CRITICAL"));

        List<WorkflowAlert> latestPublished = alertRepository.findPublishedAlerts(PageRequest.of(0, 1));
        LocalDateTime lastUpdated = latestPublished.isEmpty() ? LocalDateTime.now().minusHours(1) : latestPublished.get(0).getPublishedAt();

        return new PublicOverviewResponse(
                null,
                elevatedAlerts,
                "current",
                lastUpdated
        );
    }

    private WorkflowAlertResponse mapToResponse(WorkflowAlert a) {
        return new WorkflowAlertResponse(
                a.getId(),
                a.getTitle(),
                a.getAreaName(),
                a.getAreaId(),
                a.getAssessmentId(),
                a.getSeverity(),
                a.getDescription(),
                a.getRecommendedActions(),
                a.getSourceType(),
                a.getReferenceUrl(),
                a.getStatus(),
                a.getCreatedBy(),
                a.getCreatedAt(),
                a.getPublishedAt(),
                a.getResolvedAt()
        );
    }

    private PublicAlertResponse mapToPublicResponse(WorkflowAlert a) {
        return new PublicAlertResponse(
                a.getId(),
                a.getTitle(),
                a.getAreaName(),
                a.getSeverity(),
                a.getDescription(),
                a.getRecommendedActions(),
                a.getSourceType(),
                "APPROVED_OFFICIAL",
                a.getPublishedAt()
        );
    }
}
