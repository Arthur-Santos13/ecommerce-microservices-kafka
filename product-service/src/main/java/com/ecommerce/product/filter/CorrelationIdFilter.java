package com.ecommerce.product.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that reads the X-Correlation-Id header (set by the API Gateway)
 * and stores it in the MDC so it appears in every log line for this request.
 * Also logs the incoming request and outgoing response at INFO level.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_CORRELATION_KEY   = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long start = System.currentTimeMillis();
        try {
            logger.info(String.format("[-->] %s %s correlationId=%s",
                    request.getMethod(), request.getRequestURI(), correlationId));

            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            logger.info(String.format("[<--] %s %s status=%d elapsed=%dms correlationId=%s",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsed, correlationId));
            MDC.remove(MDC_CORRELATION_KEY);
        }
    }
}
