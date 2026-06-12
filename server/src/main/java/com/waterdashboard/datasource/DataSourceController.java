package com.waterdashboard.datasource;

import com.waterdashboard.common.response.ApiResponse;
import com.waterdashboard.datasource.dto.DataSourceListResponse;
import com.waterdashboard.datasource.dto.DataSourceRequest;
import com.waterdashboard.datasource.dto.DataSourceResponse;
import com.waterdashboard.datasource.dto.DataSourceTestRequest;
import com.waterdashboard.datasource.dto.DataSourceTestResponse;
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
@RequestMapping("/api/platform/data-sources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public ApiResponse<DataSourceListResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(dataSourceService.list(name, type, status, enabled));
    }

    @GetMapping("/{id}")
    public ApiResponse<DataSourceResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(dataSourceService.get(id));
    }

    @PostMapping
    public ApiResponse<DataSourceResponse> create(@RequestBody DataSourceRequest request) {
        return ApiResponse.success(dataSourceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DataSourceResponse> update(@PathVariable UUID id, @RequestBody DataSourceRequest request) {
        return ApiResponse.success(dataSourceService.update(id, request));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<DataSourceResponse> disable(@PathVariable UUID id) {
        return ApiResponse.success(dataSourceService.disable(id));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<DataSourceTestResponse> testSaved(@PathVariable UUID id) {
        DataSourceTestResponse result = dataSourceService.testSaved(id);
        return new ApiResponse<>(result.success(), result.code(), result.message(), result, result.traceId());
    }

    @PostMapping("/test-temp")
    public ApiResponse<DataSourceTestResponse> testTemp(@RequestBody DataSourceTestRequest request) {
        DataSourceTestResponse result = dataSourceService.testTemp(request);
        return new ApiResponse<>(result.success(), result.code(), result.message(), result, result.traceId());
    }
}
