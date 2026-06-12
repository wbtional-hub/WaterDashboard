package com.waterdashboard.componenttemplate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ComponentTemplateVersionResponse(
        UUID id,
        UUID templateId,
        String version,
        String schemaVersion,
        JsonNode dataContractJson,
        JsonNode defaultConfigJson,
        JsonNode fieldMappingSchemaJson,
        String checksum,
        String createdBy,
        OffsetDateTime createdAt
) {
}
