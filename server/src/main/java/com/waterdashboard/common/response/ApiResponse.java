package com.waterdashboard.common.response;

import com.waterdashboard.common.trace.TraceIdContext;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String traceId
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "操作成功", data, TraceIdContext.currentTraceId());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, TraceIdContext.currentTraceId());
    }
}

