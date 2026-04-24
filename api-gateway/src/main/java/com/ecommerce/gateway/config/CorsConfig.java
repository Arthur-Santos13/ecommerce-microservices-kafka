package com.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Programmatic CORS configuration for the API Gateway.
 *
 * <p>Replaces the permissive {@code globalcors.allowedOrigins: "*"} from the Basic Security
 * phase with explicitly configured allowed origins loaded from {@code app.cors.*} properties.
 * In production the allowed origins should be overridden via environment variables.</p>
 *
 * <p>Using a {@link CorsWebFilter} bean gives full control over pre-flight handling
 * and is the recommended approach for reactive (WebFlux) applications.</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter(AppProperties props) {
        AppProperties.Cors corsProps = props.getCors();

        CorsConfiguration config = new CorsConfiguration();
        corsProps.getAllowedOrigins().forEach(config::addAllowedOrigin);
        corsProps.getAllowedMethods().forEach(config::addAllowedMethod);
        corsProps.getAllowedHeaders().forEach(config::addAllowedHeader);
        config.setAllowCredentials(corsProps.isAllowCredentials());
        config.setMaxAge(corsProps.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
