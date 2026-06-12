package com.waterdashboard.datasource.dto;

public record DataSourceRequest(
        String name,
        String type,
        String host,
        Integer port,
        String database,
        String username,
        String password,
        String schema,
        String remark,
        Boolean enabled
) {
}
