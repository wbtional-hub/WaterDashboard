package com.waterdashboard.componenttemplate.dto;

public record ComponentTemplateRequest(
        String templateCode,
        String name,
        String category,
        String renderEngine,
        String description,
        Integer minWidth,
        Integer minHeight,
        Integer defaultWidth,
        Integer defaultHeight,
        String licenseScope,
        String signature,
        String checksum,
        Boolean enabled
) {
}
