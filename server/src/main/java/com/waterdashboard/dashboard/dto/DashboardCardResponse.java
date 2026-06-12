package com.waterdashboard.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DashboardCardResponse(
        UUID cardId,
        UUID dashboardId,
        String cardCode,
        String title,
        String templateCode,
        UUID templateId,
        UUID templateVersionId,
        String renderEngine,
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        Boolean enabled,
        Boolean aiEnabled,
        JsonNode configJson,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
