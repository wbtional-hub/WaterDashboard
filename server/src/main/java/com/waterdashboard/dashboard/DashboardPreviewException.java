package com.waterdashboard.dashboard;

public class DashboardPreviewException extends IllegalArgumentException {

    private final String code;

    public DashboardPreviewException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
