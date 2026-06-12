package com.waterdashboard.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record DashboardCardRequest(
        String cardCode,
        String title,
        UUID templateId,
        String templateCode,
        UUID templateVersionId,
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        Boolean enabled,
        Boolean aiEnabled,
        JsonNode configJson
) {
}
