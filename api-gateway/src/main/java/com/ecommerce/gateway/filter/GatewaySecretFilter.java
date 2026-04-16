package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.config.AppProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Injects the {@code X-Gateway-Secret} header into every proxied upstream request,
 * allowing downstream services to verify that the request originated from the gateway.
 *
 * <p>External clients cannot forge this header because
 * {@link RequestSanitizationFilter} runs first and strips it.</p>
 */
@Component
public class GatewaySecretFilter implements GlobalFilter, Ordered {

    private final String gatewaySecret;

    public GatewaySecretFilter(AppProperties appProperties) {
        this.gatewaySecret = appProperties.getGatewaySecret();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header("X-Gateway-Secret", gatewaySecret))
                .build();
        return chain.filter(mutated);
    }

    /** Runs after sanitization (HIGHEST_PRECEDENCE + 2) so the secret is never externally injectable. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
