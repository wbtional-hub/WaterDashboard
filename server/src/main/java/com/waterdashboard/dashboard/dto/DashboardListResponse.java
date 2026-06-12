package com.waterdashboard.dashboard.dto;

import java.util.List;

public record DashboardListResponse(
        List<DashboardResponse> items,
        int total
) {
}
