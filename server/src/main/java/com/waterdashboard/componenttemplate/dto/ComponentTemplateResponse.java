package com.waterdashboard.componenttemplate.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComponentTemplateResponse(
        UUID id,
        String templateCode,
        String name,
        String category,
        String renderEngine,
        String currentVersion,
        String status,
        Boolean enabled,
        String description,
        Integer minWidth,
        Integer minHeight,
        Integer defaultWidth,
        Integer defaultHeight,
        String licenseScope,
        String signature,
        String checksum,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
