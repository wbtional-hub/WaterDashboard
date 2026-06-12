package com.waterdashboard.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DashboardDraftRequest(
        JsonNode configJson
) {
}
