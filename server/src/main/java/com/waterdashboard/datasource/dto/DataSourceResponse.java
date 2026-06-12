package com.waterdashboard.datasource.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DataSourceResponse(
        UUID id,
        String name,
        String type,
        String host,
        Integer port,
        String database,
        String username,
        String schema,
        String remark,
        String status,
        Boolean enabled,
        OffsetDateTime lastTestTime,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
