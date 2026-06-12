package com.waterdashboard.componenttemplate.dto;

import java.util.List;

public record ComponentTemplateDetailResponse(
        ComponentTemplateResponse template,
        List<ComponentTemplateVersionResponse> versions
) {
}
