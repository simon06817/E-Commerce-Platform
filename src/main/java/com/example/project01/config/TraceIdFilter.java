package com.example.project01.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.project01.observability.TraceContext;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds a request-scoped trace id to MDC and the response header. An incoming
 * trace id is accepted only when it has a safe, bounded format.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = TraceContext.TRACE_ID_HEADER;
    public static final String TRACE_ID_MDC_KEY = TraceContext.TRACE_ID_MDC_KEY;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceContext.normalizeOrCreate(request.getHeader(TRACE_ID_HEADER));
        response.setHeader(TRACE_ID_HEADER, traceId);
        try (MDC.MDCCloseable ignored = TraceContext.withTraceId(traceId)) {
            filterChain.doFilter(request, response);
        }
    }
}
