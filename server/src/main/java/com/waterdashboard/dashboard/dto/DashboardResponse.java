package com.waterdashboard.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DashboardResponse(
        UUID id,
        String dashboardCode,
        String name,
        String status,
        Boolean enabled,
        String description,
        Integer screenWidth,
        Integer screenHeight,
        JsonNode backgroundConfigJson,
        JsonNode themeConfigJson,
        UUID currentPublishedVersionId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
