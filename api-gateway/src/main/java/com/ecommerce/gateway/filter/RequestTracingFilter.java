package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter applied to every request passing through the gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Injects {@code X-Correlation-Id} — unique trace identifier for distributed tracing.
 *       Re-uses the value if the client already sent one (e.g., from a front-end that generates its own).</li>
 *   <li>Injects {@code X-Api-Version} — communicates the current API version to downstream services.</li>
 * </ul>
 * </p>
 *
 * <p>Both headers are forwarded to downstream services so they can be included
 * in logs and error responses, enabling end-to-end request correlation.</p>
 *
 * <p>Note: full distributed tracing (Micrometer + Zipkin) is planned for phase 11.</p>
 */
@Component
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String API_VERSION_HEADER    = "X-Api-Version";
    private static final String CURRENT_API_VERSION   = "v1";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(API_VERSION_HEADER, CURRENT_API_VERSION)
                .build();

        log.debug("Routing [{}] {} — correlationId={}",
                mutatedRequest.getMethod(),
                mutatedRequest.getURI().getPath(),
                correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Returns the correlation ID from the incoming request if present,
     * otherwise generates a new UUID.
     */
    private String resolveCorrelationId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        return (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    }

    /** Runs before most other filters to ensure headers are set early in the chain. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
