package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter applied to every request passing through the gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Injects {@code X-Correlation-Id} — unique trace identifier for distributed tracing.</li>
 *   <li>Injects {@code X-Api-Version} — communicates the current API version to downstream services.</li>
 *   <li>Logs {@code [-->]} / {@code [<--]} lines with method, path, HTTP status, elapsed time,
 *       correlation ID, and the authenticated username (when available).</li>
 * </ul>
 * </p>
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
        long start = System.currentTimeMillis();

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(API_VERSION_HEADER, CURRENT_API_VERSION)
                .build();

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication() != null
                        ? ctx.getAuthentication().getName()
                        : "anonymous")
                .defaultIfEmpty("anonymous")
                .flatMap(username -> {
                    log.info("[-->] {} {} correlationId={} user={}",
                            mutatedRequest.getMethod(),
                            mutatedRequest.getURI().getPath(),
                            correlationId, username);

                    return chain.filter(exchange.mutate().request(mutatedRequest).build())
                            .doFinally(signal -> {
                                long elapsed = System.currentTimeMillis() - start;
                                int status = exchange.getResponse().getStatusCode() != null
                                        ? exchange.getResponse().getStatusCode().value() : 0;
                                log.info("[<--] {} {} status={} elapsed={}ms correlationId={} user={}",
                                        mutatedRequest.getMethod(),
                                        mutatedRequest.getURI().getPath(),
                                        status, elapsed, correlationId, username);
                            });
                });
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        return (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
