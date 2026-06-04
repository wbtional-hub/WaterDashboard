package com.waterdashboard.common.trace;

import org.slf4j.MDC;

public final class TraceIdContext {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceIdContext() {
    }

    public static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId == null ? "" : traceId;
    }
}

