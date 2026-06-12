package com.waterdashboard.dashboard.dto;

import java.util.List;
import java.util.Map;

public record DashboardCardPreviewResponse(
        List<Column> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long durationMs,
        String sqlHash,
        boolean truncated
) {
    public record Column(
            String name,
            String type
    ) {
    }
}
