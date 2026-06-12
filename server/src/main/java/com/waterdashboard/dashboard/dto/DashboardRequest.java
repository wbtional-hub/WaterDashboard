package com.waterdashboard.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DashboardRequest(
        String dashboardCode,
        String name,
        String description,
        Integer screenWidth,
        Integer screenHeight,
        JsonNode backgroundConfigJson,
        JsonNode themeConfigJson,
        Boolean enabled
) {
}
