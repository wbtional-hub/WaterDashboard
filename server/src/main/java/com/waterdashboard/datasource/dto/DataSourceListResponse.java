package com.waterdashboard.datasource.dto;

import java.util.List;

public record DataSourceListResponse(
        List<DataSourceResponse> items,
        int total
) {
}
