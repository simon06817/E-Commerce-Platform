package com.ecommerce.platform.observability;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/** 链路追踪上下文。 */
public final class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** 禁止实例化工具类。 */
    private TraceContext() {
    }

    /** 获取或生成当前链路 ID。 */
    public static String currentOrCreate() {
        return normalizeOrCreate(MDC.get(TRACE_ID_MDC_KEY));
    }

    /** 规范化或生成链路 ID。 */
    public static String normalizeOrCreate(String traceId) {
        if (StringUtils.hasText(traceId) && SAFE_TRACE_ID.matcher(traceId).matches()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 将链路 ID 写入 MDC。 */
    public static MDC.MDCCloseable withTraceId(String traceId) {
        return MDC.putCloseable(TRACE_ID_MDC_KEY, normalizeOrCreate(traceId));
    }
}
