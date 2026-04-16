package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Provides rate-limiting key resolvers for Spring Cloud Gateway's {@code RequestRateLimiter}.
 *
 * <p>Two resolvers are registered:
 * <ul>
 *   <li><strong>userKeyResolver</strong> (primary) — uses the authenticated username from
 *       {@code X-User-Name} header (injected by {@link com.ecommerce.gateway.security.JwtAuthenticationFilter}).
 *       Falls back to the client IP when the header is absent (unauthenticated requests).</li>
 *   <li><strong>ipKeyResolver</strong> — uses the remote client IP address, used on public
 *       endpoints like {@code /auth/**} where no user identity is available yet.</li>
 * </ul>
 * </p>
 */
@Configuration
public class RateLimitConfig {

    /**
     * Primary resolver: user identity for authenticated routes.
     * Falls back to remote IP for anonymous callers.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("X-User-Name");
            if (user != null && !user.isBlank()) {
                return Mono.just("user:" + user);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * IP-based resolver used for unauthenticated endpoints (e.g. {@code /auth/login}).
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
