package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.security.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Global filter that sanitizes incoming requests and adds protective response headers.
 *
 * <h3>Request sanitization</h3>
 * <ul>
 *   <li>Enforces maximum URI length (8192 chars) and maximum header value length (8192 chars).</li>
 *   <li>Rejects requests containing SQL injection patterns in URI query strings.</li>
 *   <li>Rejects requests containing XSS patterns in URI query strings.</li>
 *   <li>Strips {@code X-User-Name} and {@code X-User-Roles} headers from external clients
 *       to prevent header injection attacks — only the gateway itself may set these.</li>
 * </ul>
 *
 * <h3>Security response headers</h3>
 * Adds {@code X-Content-Type-Options}, {@code X-Frame-Options},
 * {@code Referrer-Policy}, and {@code X-XSS-Protection} to every response.
 */
@Component
public class RequestSanitizationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestSanitizationFilter.class);

    private final AuditService auditService;

    public RequestSanitizationFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    private static final int MAX_URI_LENGTH    = 8192;
    private static final int MAX_HEADER_LENGTH = 8192;

    /** Headers that only the gateway may inject — must never come from external clients. */
    private static final List<String> PROTECTED_HEADERS = List.of("X-User-Name", "X-User-Roles");

    /** Rough pattern covering common SQL injection fragments. */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|UNION|EXEC|EXECUTE)\\b" +
            "|--|;\\s*(DROP|SELECT|INSERT|UPDATE|DELETE)\\b" +
            "|'\\s*OR\\s*'|'\\s*AND\\s*')",
            Pattern.CASE_INSENSITIVE);

    /** Rough pattern covering common XSS fragments. */
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<script[^>]*>|</script>|javascript:|on\\w+=|<iframe|<object|<embed|<link|<meta)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // ── URI length ──────────────────────────────────────────────────────
        String uri = request.getURI().toString();
        if (uri.length() > MAX_URI_LENGTH) {
            log.warn("Request rejected: URI too long ({}): {}", uri.length(), request.getPath());
            return rejectRequest(exchange, "Request URI exceeds maximum allowed length.");
        }

        // ── Header value length ─────────────────────────────────────────────
        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                if (value != null && value.length() > MAX_HEADER_LENGTH) {
                    log.warn("Request rejected: header '{}' value too long", entry.getKey());
                    return rejectRequest(exchange, "Request header value exceeds maximum allowed length.");
                }
            }
        }

        // ── SQL injection check on query string ─────────────────────────────
        String query = request.getURI().getRawQuery();
        if (query != null && SQL_INJECTION_PATTERN.matcher(query).find()) {
            log.warn("Request rejected: potential SQL injection in query string: path={}",
                    request.getPath());
            return rejectRequest(exchange, "Request contains potentially malicious content.");
        }

        // ── XSS check on query string ────────────────────────────────────────
        if (query != null && XSS_PATTERN.matcher(query).find()) {
            log.warn("Request rejected: potential XSS in query string: path={}", request.getPath());
            return rejectRequest(exchange, "Request contains potentially malicious content.");
        }

        // ── Strip protected headers from external clients ────────────────────
        ServerHttpRequest sanitizedRequest = request.mutate()
                .headers(headers -> PROTECTED_HEADERS.forEach(headers::remove))
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        // ── Add security response headers ────────────────────────────────────
        return chain.filter(sanitizedExchange)
                .then(Mono.fromRunnable(() -> {
                    ServerHttpResponse response = sanitizedExchange.getResponse();
                    response.getHeaders().set("X-Content-Type-Options", "nosniff");
                    response.getHeaders().set("X-Frame-Options", "DENY");
                    response.getHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
                    response.getHeaders().set("X-XSS-Protection", "1; mode=block");
                }));
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, String message) {
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        auditService.maliciousRequestBlocked(ip, exchange.getRequest().getURI().getPath(), message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"%s\"}", message);
        DataBuffer buf = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buf));
    }

    /** Runs early in the filter chain, right after tracing (HIGHEST_PRECEDENCE + 1). */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
