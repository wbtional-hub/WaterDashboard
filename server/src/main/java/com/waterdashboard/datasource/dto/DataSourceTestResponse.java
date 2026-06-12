package com.waterdashboard.datasource.dto;

public record DataSourceTestResponse(
        boolean success,
        String code,
        String message,
        String traceId,
        Long latencyMs,
        String databaseType,
        String databaseVersion,
        String errorSummary
) {
}
