package com.ecommerce.gateway.security;

import com.ecommerce.gateway.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages opaque refresh tokens with a fixed TTL.
 *
 * <p>Storage is in-memory (appropriate for Phase 12 Part 2 — single gateway instance).
 * A distributed store (Redis) will replace this in a later operational phase when
 * horizontal scaling is introduced.</p>
 *
 * <p>Each refresh token entry stores the associated username, roles, and expiry time.
 * A scheduled task runs every hour to evict expired tokens and prevent unbounded growth.</p>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /** token → metadata. ConcurrentHashMap is safe for concurrent gateway requests. */
    private final Map<String, TokenEntry> store = new ConcurrentHashMap<>();

    public RefreshTokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates and stores a new refresh token for the given user.
     *
     * @return the opaque refresh token string
     */
    public String createRefreshToken(String username, List<String> roles) {
        String token = generateToken();
        long expiresAt = System.currentTimeMillis() + appProperties.getJwt().getRefreshExpirationMs();
        store.put(token, new TokenEntry(username, roles, expiresAt));
        log.debug("Refresh token created for user={}", username);
        return token;
    }

    /**
     * Validates the token and returns its entry, or {@code null} if invalid/expired.
     */
    public TokenEntry validate(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            store.remove(token);
            return null;
        }
        return entry;
    }

    /**
     * Revokes a specific refresh token (used on logout).
     */
    public void revoke(String token) {
        TokenEntry removed = store.remove(token);
        if (removed != null) {
            log.info("Refresh token revoked for user={}", removed.username());
        }
    }

    /**
     * Revokes all refresh tokens belonging to a user (used on password change / force logout).
     */
    public void revokeAll(String username) {
        long count = store.entrySet().removeIf(e -> e.getValue().username().equals(username))
                ? store.size() : 0;
        log.info("All refresh tokens revoked for user={}", username);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    /** Evicts expired tokens once per hour to prevent unbounded memory growth. */
    @Scheduled(fixedDelay = 3_600_000)
    public void evictExpired() {
        int before = store.size();
        store.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("Evicted {} expired refresh token(s)", removed);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ── Token entry ───────────────────────────────────────────────────────────

    public record TokenEntry(String username, List<String> roles, long expiresAtMs) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
        public Instant expiresAt() {
            return Instant.ofEpochMilli(expiresAtMs);
        }
    }
}
