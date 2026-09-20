package com.example.project01.observability;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Shared helpers for request, scheduled-task and RabbitMQ trace correlation.
 */
public final class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private TraceContext() {
    }

    public static String currentOrCreate() {
        return normalizeOrCreate(MDC.get(TRACE_ID_MDC_KEY));
    }

    public static String normalizeOrCreate(String traceId) {
        if (StringUtils.hasText(traceId) && SAFE_TRACE_ID.matcher(traceId).matches()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static MDC.MDCCloseable withTraceId(String traceId) {
        return MDC.putCloseable(TRACE_ID_MDC_KEY, normalizeOrCreate(traceId));
    }
}
