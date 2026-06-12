package com.waterdashboard.dashboard;

import com.waterdashboard.common.response.ApiResponse;
import com.waterdashboard.dashboard.dto.DashboardCardPreviewRequest;
import com.waterdashboard.dashboard.dto.DashboardCardPreviewResponse;
import com.waterdashboard.dashboard.dto.DashboardCardRequest;
import com.waterdashboard.dashboard.dto.DashboardCardResponse;
import com.waterdashboard.dashboard.dto.DashboardDraftRequest;
import com.waterdashboard.dashboard.dto.DashboardDraftResponse;
import com.waterdashboard.dashboard.dto.DashboardListResponse;
import com.waterdashboard.dashboard.dto.DashboardRequest;
import com.waterdashboard.dashboard.dto.DashboardResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardQueryPreviewService queryPreviewService;

    public DashboardController(DashboardService dashboardService, DashboardQueryPreviewService queryPreviewService) {
        this.dashboardService = dashboardService;
        this.queryPreviewService = queryPreviewService;
    }

    @GetMapping
    public ApiResponse<DashboardListResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String dashboardCode,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(dashboardService.list(name, dashboardCode, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<DashboardResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(dashboardService.get(id));
    }

    @PostMapping
    public ApiResponse<DashboardResponse> create(@RequestBody DashboardRequest request) {
        return ApiResponse.success(dashboardService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DashboardResponse> update(@PathVariable UUID id, @RequestBody DashboardRequest request) {
        return ApiResponse.success(dashboardService.update(id, request));
    }

    @PatchMapping("/{id}/enable")
    public ApiResponse<DashboardResponse> enable(@PathVariable UUID id) {
        return ApiResponse.success(dashboardService.enable(id));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<DashboardResponse> disable(@PathVariable UUID id) {
        return ApiResponse.success(dashboardService.disable(id));
    }

    @GetMapping("/{id}/draft")
    public ApiResponse<DashboardDraftResponse> getDraft(@PathVariable UUID id) {
        return ApiResponse.success(dashboardService.getDraft(id));
    }

    @PutMapping("/{id}/draft")
    public ApiResponse<DashboardDraftResponse> saveDraft(
            @PathVariable UUID id,
            @RequestBody DashboardDraftRequest request) {
        return ApiResponse.success(dashboardService.saveDraft(id, request));
    }

    @GetMapping("/{id}/draft/cards")
    public ApiResponse<List<DashboardCardResponse>> listCards(@PathVariable UUID id) {
        return ApiResponse.success(dashboardService.listCards(id));
    }

    @PostMapping("/{id}/draft/cards")
    public ApiResponse<DashboardCardResponse> createCard(
            @PathVariable UUID id,
            @RequestBody DashboardCardRequest request) {
        return ApiResponse.success(dashboardService.createCard(id, request));
    }

    @PutMapping("/{id}/draft/cards/{cardId}")
    public ApiResponse<DashboardCardResponse> updateCard(
            @PathVariable UUID id,
            @PathVariable UUID cardId,
            @RequestBody DashboardCardRequest request) {
        return ApiResponse.success(dashboardService.updateCard(id, cardId, request));
    }

    @DeleteMapping("/{id}/draft/cards/{cardId}")
    public ApiResponse<Void> deleteCard(@PathVariable UUID id, @PathVariable UUID cardId) {
        dashboardService.deleteCard(id, cardId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/draft/cards/{cardId}/preview-query")
    public ApiResponse<DashboardCardPreviewResponse> previewQuery(
            @PathVariable UUID id,
            @PathVariable UUID cardId,
            @RequestBody(required = false) DashboardCardPreviewRequest request) {
        return ApiResponse.success(queryPreviewService.preview(id, cardId, request));
    }
}
