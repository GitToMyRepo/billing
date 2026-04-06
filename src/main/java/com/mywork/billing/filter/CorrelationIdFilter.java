package com.mywork.billing.filter;

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
 * Servlet filter that ensures every request has a correlation ID.
 *
 * If the incoming request has an X-Correlation-ID header (passed from an upstream
 * service), that value is reused. Otherwise a new UUID is generated.
 *
 * The correlation ID is:
 * - Added to MDC so it appears in every log line for this request automatically
 * - Returned in the X-Correlation-ID response header so the caller can reference it
 *
 * This enables end-to-end request tracing across microservices.
 */
@Component
@Order(1) // run before other filters
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // Use existing correlation ID from upstream service, or generate a new one
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put in MDC - SLF4J will include this in every log line for this request
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        // Return in response header so caller can reference it
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent memory leaks in thread pool environments
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
