package com.waterdashboard.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DashboardDraftResponse(
        UUID id,
        UUID dashboardId,
        Long revision,
        String schemaVersion,
        JsonNode configJson,
        String updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
