package com.waterdashboard.componenttemplate.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ComponentTemplateVersionRequest(
        String version,
        JsonNode dataContractJson,
        JsonNode defaultConfigJson,
        JsonNode fieldMappingSchemaJson,
        String checksum
) {
}
