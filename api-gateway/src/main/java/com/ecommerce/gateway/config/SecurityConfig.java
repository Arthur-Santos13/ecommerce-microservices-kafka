package com.ecommerce.gateway.config;

import com.ecommerce.gateway.security.JwtAuthenticationFilter;
import com.ecommerce.gateway.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security configuration for the API Gateway (WebFlux / reactive stack).
 *
 * <h3>Access rules</h3>
 * <ul>
 *   <li>{@code /auth/**}         — public (login endpoint)</li>
 *   <li>{@code /actuator/health/**} — public (liveness / readiness probes)</li>
 *   <li>{@code /fallback/**}     — public (circuit-breaker fallbacks)</li>
 *   <li>GET {@code /api/v1/products/**} — requires USER or ADMIN role</li>
 *   <li>POST/PUT/DELETE {@code /api/v1/products/**} — requires ADMIN role</li>
 *   <li>{@code /api/v1/orders/**}   — requires USER or ADMIN role</li>
 *   <li>{@code /api/v1/payments/**} — requires USER or ADMIN role</li>
 *   <li>Everything else           — requires authentication</li>
 * </ul>
 *
 * <h3>Users (Basic Security)</h3>
 * Loaded from {@code app.users} in application.yml / environment variables.
 * A user database will replace this in Phase 12 Part 2 (Advanced Security).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                          JwtAuthenticationFilter jwtFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        // ── Public endpoints ───────────────────────────────────────────────
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/fallback/**").permitAll()

                        // ── Product service: read = any authenticated, write = ADMIN ───────
                        .pathMatchers(HttpMethod.GET, "/api/v1/products/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1/products/**").hasRole("ADMIN")

                        // ── Order service: authenticated users ─────────────────────────────
                        .pathMatchers("/api/v1/orders/**").hasAnyRole("USER", "ADMIN")

                        // ── Payment service: authenticated users ───────────────────────────
                        .pathMatchers("/api/v1/payments/**").hasAnyRole("USER", "ADMIN")

                        // ── Catch-all: require authentication ──────────────────────────────
                        .anyExchange().authenticated()
                )

                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            String body = """
                                    {"status":401,"error":"Unauthorized","message":"Authentication required. Please provide a valid Bearer token."}
                                    """;
                            DataBuffer buf = exchange.getResponse().bufferFactory()
                                    .wrap(body.strip().getBytes(StandardCharsets.UTF_8));
                            return exchange.getResponse().writeWith(Mono.just(buf));
                        })
                        .accessDeniedHandler((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            String body = """
                                    {"status":403,"error":"Forbidden","message":"You do not have permission to access this resource."}
                                    """;
                            DataBuffer buf = exchange.getResponse().bufferFactory()
                                    .wrap(body.strip().getBytes(StandardCharsets.UTF_8));
                            return exchange.getResponse().writeWith(Mono.just(buf));
                        })
                )
                .build();
    }

    /**
     * In-memory user store loaded from {@code app.users} configuration.
     * Passwords are BCrypt-encoded at startup.
     */
    @Bean
    public ReactiveUserDetailsService userDetailsService(AppProperties props,
                                                          PasswordEncoder passwordEncoder) {
        List<UserDetails> users = props.getUsers().stream()
                .map(u -> User.withUsername(u.getUsername())
                        .password(passwordEncoder.encode(u.getPassword()))
                        .roles(u.getRoles().toArray(String[]::new))
                        .build())
                .toList();
        return new MapReactiveUserDetailsService(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
