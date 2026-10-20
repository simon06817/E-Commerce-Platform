package com.ecommerce.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 请求日志过滤器。 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** 执行请求过滤处理。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("{} {} -> {} [{} ms]", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), cost);
        }
    }
}
