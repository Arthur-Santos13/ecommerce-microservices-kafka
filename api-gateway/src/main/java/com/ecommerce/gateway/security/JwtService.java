package com.ecommerce.gateway.security;

import com.ecommerce.gateway.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Handles JWT token generation and validation for the gateway.
 *
 * <p>Tokens are signed with HMAC-SHA256 and carry the following claims:
 * <ul>
 *   <li>{@code sub}   — username</li>
 *   <li>{@code roles} — list of role strings (e.g. ["ADMIN", "USER"])</li>
 *   <li>{@code iat}   — issued-at timestamp</li>
 *   <li>{@code exp}   — expiry timestamp</li>
 * </ul>
 * </p>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    // ── Token generation ──────────────────────────────────────────────────────

    public String generateToken(String username, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + appProperties.getJwt().getExpirationMs()))
                .signWith(signingKey())
                .compact();
    }

    // ── Token validation ──────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // ── Claims extraction ─────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaims(token).get("roles", List.class);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
