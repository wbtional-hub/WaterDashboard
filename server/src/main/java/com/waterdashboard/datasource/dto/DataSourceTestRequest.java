package com.waterdashboard.datasource.dto;

public record DataSourceTestRequest(
        String type,
        String host,
        Integer port,
        String database,
        String username,
        String password,
        String schema
) {
}
