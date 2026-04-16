package com.ecommerce.gateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive WebFilter that validates the Bearer JWT token on every incoming request.
 *
 * <p>On a valid token it:
 * <ol>
 *   <li>Builds a {@link UsernamePasswordAuthenticationToken} with the user's roles.</li>
 *   <li>Populates {@link ReactiveSecurityContextHolder} so Spring Security can enforce
 *       the access rules configured in {@link com.ecommerce.gateway.config.SecurityConfig}.</li>
 *   <li>Mutates the request to inject {@code X-User-Name} and {@code X-User-Roles} headers
 *       so downstream services receive the authenticated user context without re-validating
 *       the JWT themselves.</li>
 * </ol>
 * </p>
 *
 * <p>This filter is registered inside Spring Security's filter chain via
 * {@link org.springframework.security.config.web.server.SecurityWebFiltersOrder#AUTHENTICATION}
 * — not as a standalone {@code WebFilter} bean — to ensure proper integration with
 * Spring Security's exception handling.</p>
 */
public class JwtAuthenticationFilter implements WebFilter {

    static final String USER_NAME_HEADER  = "X-User-Name";
    static final String USER_ROLES_HEADER = "X-User-Roles";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractBearerToken(exchange.getRequest());

        if (token == null || !jwtService.validateToken(token)) {
            return chain.filter(exchange);
        }

        String username = jwtService.extractUsername(token);
        List<String> roles = jwtService.extractRoles(token);

        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);

        // Forward user context as headers so downstream services don't need to re-validate JWT.
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(USER_NAME_HEADER,  username)
                .header(USER_ROLES_HEADER, String.join(",", roles))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private String extractBearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
