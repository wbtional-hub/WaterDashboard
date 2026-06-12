package com.waterdashboard.componenttemplate.dto;

import java.util.List;

public record ComponentTemplateListResponse(
        List<ComponentTemplateResponse> items,
        int total
) {
}
