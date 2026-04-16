package com.ecommerce.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Structured audit logging for security-relevant events.
 *
 * <p>Audit events are written to a dedicated {@code AUDIT} logger so they can be
 * routed to a separate file or log stream (e.g., a Logstash index named
 * {@code ecommerce-audit-YYYY.MM.dd}) independently of application logs.</p>
 *
 * <p>Every log line is a structured JSON-friendly string containing:</p>
 * <ul>
 *   <li>{@code event}   — the audit event type (e.g., LOGIN_SUCCESS)</li>
 *   <li>{@code user}    — username or "anonymous"</li>
 *   <li>{@code ip}      — client IP address</li>
 *   <li>{@code path}    — request path (when applicable)</li>
 *   <li>{@code detail}  — additional context</li>
 * </ul>
 */
@Service
public class AuditService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    // ── Auth events ───────────────────────────────────────────────────────────

    public void loginSuccess(String username, String ip) {
        audit.info("[AUDIT] event=LOGIN_SUCCESS user={} ip={}", username, ip);
    }

    public void loginFailure(String username, String ip) {
        audit.warn("[AUDIT] event=LOGIN_FAILURE user={} ip={}", username, ip);
    }

    public void tokenRefreshed(String username, String ip) {
        audit.info("[AUDIT] event=TOKEN_REFRESHED user={} ip={}", username, ip);
    }

    public void logout(String username, String ip) {
        audit.info("[AUDIT] event=LOGOUT user={} ip={}", username, ip);
    }

    // ── Authorization events ──────────────────────────────────────────────────

    public void accessDenied(String username, String ip, String path) {
        audit.warn("[AUDIT] event=ACCESS_DENIED user={} ip={} path={}", username, ip, path);
    }

    public void unauthorized(String ip, String path) {
        audit.warn("[AUDIT] event=UNAUTHORIZED_ACCESS ip={} path={}", ip, path);
    }

    // ── Admin actions ─────────────────────────────────────────────────────────

    public void adminAction(String username, String ip, String path, String method) {
        audit.info("[AUDIT] event=ADMIN_ACTION user={} ip={} method={} path={}",
                username, ip, method, path);
    }

    // ── Request security events ───────────────────────────────────────────────

    public void maliciousRequestBlocked(String ip, String path, String reason) {
        audit.warn("[AUDIT] event=MALICIOUS_REQUEST_BLOCKED ip={} path={} reason={}",
                ip, path, reason);
    }
}
