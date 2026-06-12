package com.waterdashboard.componenttemplate;

import com.waterdashboard.common.response.ApiResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateDetailResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateListResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateRequest;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateResponse;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateVersionRequest;
import com.waterdashboard.componenttemplate.dto.ComponentTemplateVersionResponse;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/platform/component-templates")
public class ComponentTemplateController {

    private final ComponentTemplateService componentTemplateService;

    public ComponentTemplateController(ComponentTemplateService componentTemplateService) {
        this.componentTemplateService = componentTemplateService;
    }

    @GetMapping
    public ApiResponse<ComponentTemplateListResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(componentTemplateService.list(name, templateCode, category, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ComponentTemplateDetailResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(componentTemplateService.get(id));
    }

    @PostMapping
    public ApiResponse<ComponentTemplateResponse> create(@RequestBody ComponentTemplateRequest request) {
        return ApiResponse.success(componentTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ComponentTemplateResponse> update(
            @PathVariable UUID id,
            @RequestBody ComponentTemplateRequest request) {
        return ApiResponse.success(componentTemplateService.update(id, request));
    }

    @PatchMapping("/{id}/enable")
    public ApiResponse<ComponentTemplateResponse> enable(@PathVariable UUID id) {
        return ApiResponse.success(componentTemplateService.enable(id));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<ComponentTemplateResponse> disable(@PathVariable UUID id) {
        return ApiResponse.success(componentTemplateService.disable(id));
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<ComponentTemplateVersionResponse> createVersion(
            @PathVariable UUID id,
            @RequestBody ComponentTemplateVersionRequest request) {
        return ApiResponse.success(componentTemplateService.createVersion(id, request));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<ComponentTemplateVersionResponse>> listVersions(@PathVariable UUID id) {
        return ApiResponse.success(componentTemplateService.listVersions(id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public ApiResponse<ComponentTemplateVersionResponse> getVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId) {
        return ApiResponse.success(componentTemplateService.getVersion(id, versionId));
    }
}
