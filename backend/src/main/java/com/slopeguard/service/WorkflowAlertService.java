package com.slopeguard.service;

import com.slopeguard.dto.AlertDeliveryResponse;
import com.slopeguard.dto.PublicAlertResponse;
import com.slopeguard.dto.PublicOverviewResponse;
import com.slopeguard.dto.WorkflowAlertRequest;
import com.slopeguard.dto.WorkflowAlertResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface WorkflowAlertService {

    Page<WorkflowAlertResponse> listAlerts(String status, String severity, int page, int size);

    WorkflowAlertResponse createDraftAlert(WorkflowAlertRequest request, Long creatorUserId);

    WorkflowAlertResponse getAlert(String id);

    WorkflowAlertResponse updateDraftAlert(String id, WorkflowAlertRequest request, Long currentUserId);

    WorkflowAlertResponse submitAlert(String id, Long currentUserId);

    WorkflowAlertResponse approveAlert(String id, Long currentUserId);

    WorkflowAlertResponse publishAlert(String id, Long currentUserId);

    WorkflowAlertResponse rejectAlert(String id, String reason, Long currentUserId);

    WorkflowAlertResponse resolveAlert(String id, String reason, Long currentUserId);

    WorkflowAlertResponse withdrawAlert(String id, String reason, Long currentUserId);

    List<AlertDeliveryResponse> getDeliveries(String id);

    List<PublicAlertResponse> getPublicAlerts(int limit);

    PublicAlertResponse getPublicAlert(String id);

    PublicOverviewResponse getPublicOverview();
}
