package com.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed configuration properties for application-level settings.
 * Bound from the {@code app.*} prefix in application.yml / environment variables.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final List<UserDefinition> users = new ArrayList<>();

    public Jwt getJwt() { return jwt; }
    public Cors getCors() { return cors; }
    public List<UserDefinition> getUsers() { return users; }

    // ── JWT settings ──────────────────────────────────────────────────────────

    public static class Jwt {
        /** HMAC-SHA256 signing secret — must be ≥ 32 bytes. Override via JWT_SECRET env var. */
        private String secret = "ecommerce-microservices-dev-jwt-secret-key-for-development-only";
        /** Access token validity in milliseconds (default: 15 min). */
        private long expirationMs = 900_000L;
        /** Refresh token validity in milliseconds (default: 7 days). */
        private long refreshExpirationMs = 604_800_000L;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }

        public long getRefreshExpirationMs() { return refreshExpirationMs; }
        public void setRefreshExpirationMs(long refreshExpirationMs) { this.refreshExpirationMs = refreshExpirationMs; }
    }

    // ── In-memory users (Basic Security — replaced by DB in Advanced phase) ───

    public static class UserDefinition {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    // ── CORS settings ─────────────────────────────────────────────────────────

    public static class Cors {
        /** Allowed origins — never use * in production. */
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:4200");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        private List<String> allowedHeaders = List.of(
                "Authorization", "Content-Type", "X-Correlation-Id", "X-Api-Version");
        private boolean allowCredentials = true;
        private long maxAge = 3600L;

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }

        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }

        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }
}
