package com.waterdashboard.common.exception;

import com.waterdashboard.common.response.ApiResponse;
import com.waterdashboard.common.trace.TraceIdContext;
import com.waterdashboard.dashboard.DashboardPreviewException;
import com.waterdashboard.sqlsecurity.SqlSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException exception) {
        log.warn("Bad request, traceId={}, message={}", TraceIdContext.currentTraceId(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(SqlSecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSqlSecurity(SqlSecurityException exception) {
        log.warn("SQL security rejected request, traceId={}, code={}, message={}",
                TraceIdContext.currentTraceId(), exception.code(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(DashboardPreviewException.class)
    public ResponseEntity<ApiResponse<Void>> handlePreview(DashboardPreviewException exception) {
        log.warn("Dashboard preview rejected request, traceId={}, code={}, message={}",
                TraceIdContext.currentTraceId(), exception.code(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EmptyResultDataAccessException exception) {
        log.warn("Resource not found, traceId={}", TraceIdContext.currentTraceId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled request exception, traceId={}", TraceIdContext.currentTraceId(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "服务暂不可用，请稍后重试"));
    }
}
