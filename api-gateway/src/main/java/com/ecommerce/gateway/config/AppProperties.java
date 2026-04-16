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
    private final List<UserDefinition> users = new ArrayList<>();

    public Jwt getJwt() {
        return jwt;
    }

    public List<UserDefinition> getUsers() {
        return users;
    }

    // ── JWT settings ──────────────────────────────────────────────────────────

    public static class Jwt {
        /** HMAC-SHA256 signing secret — must be ≥ 32 bytes. Override via JWT_SECRET env var. */
        private String secret = "ecommerce-microservices-dev-jwt-secret-key-for-development-only";
        /** Token validity in milliseconds (default: 24 h). */
        private long expirationMs = 86_400_000L;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
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
}
