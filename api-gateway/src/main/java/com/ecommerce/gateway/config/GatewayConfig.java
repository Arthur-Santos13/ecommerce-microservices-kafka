package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic gateway route configuration.
 *
 * <p>Routes and circuit breaker filters are declared in {@code application.yml}.
 * This class is the anchor for routes that require complex logic not expressible
 * in YAML — e.g., custom predicates, conditional routing, or dynamic route
 * registration planned for later phases.</p>
 *
 * <p>Future additions (do NOT implement ahead of schedule):
 * <ul>
 *   <li>Phase 9.5 — lb:// URIs after service discovery is introduced</li>
 *   <li>Phase 12 — JWT authentication filter</li>
 * </ul>
 * </p>
 */
@Configuration
public class GatewayConfig {

    /**
     * Returns an empty RouteLocator so Spring Cloud Gateway does not complain
     * about a missing bean. All active routes are defined in application.yml.
     */
    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }
}
