package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productsFallback(ServerWebExchange exchange) {
        log.warn("Circuit breaker triggered for product-service: path={}",
                exchange.getRequest().getPath());
        return buildFallback("product-service");
    }

    @RequestMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> ordersFallback(ServerWebExchange exchange) {
        log.warn("Circuit breaker triggered for order-service: path={}",
                exchange.getRequest().getPath());
        return buildFallback("order-service");
    }

    @RequestMapping("/payments")
    public Mono<ResponseEntity<Map<String, Object>>> paymentsFallback(ServerWebExchange exchange) {
        log.warn("Circuit breaker triggered for payment-service: path={}",
                exchange.getRequest().getPath());
        return buildFallback("payment-service");
    }

    @RequestMapping("/notifications")
    public Mono<ResponseEntity<Map<String, Object>>> notificationsFallback(ServerWebExchange exchange) {
        log.warn("Circuit breaker triggered for notification-service: path={}",
                exchange.getRequest().getPath());
        return buildFallback("notification-service");
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallback(String service) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", "The " + service + " is temporarily unavailable. Please try again later.",
                "service", service
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
