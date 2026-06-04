package com.waterdashboard.health;

import com.waterdashboard.common.response.ApiResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;
    private final String applicationVersion;

    public HealthController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.version}") String applicationVersion
    ) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("currentTime", OffsetDateTime.now(ZoneOffset.UTC));
        data.put("applicationName", applicationName);
        data.put("version", applicationVersion);
        return ApiResponse.success(data);
    }

    @GetMapping("/error-demo")
    public ApiResponse<Void> errorDemo() {
        throw new IllegalStateException("Health error demo");
    }
}

