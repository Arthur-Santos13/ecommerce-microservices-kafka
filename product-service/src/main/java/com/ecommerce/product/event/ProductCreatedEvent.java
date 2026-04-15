package com.ecommerce.product.event;

import com.ecommerce.product.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new product is created.
 * Topic: product.created
 *
 * Standard event envelope fields:
 *   eventId     — unique event identifier (UUID)
 *   eventType   — dot-separated type name (service.action)
 *   source      — originating service
 *   occurredOn  — UTC instant the event was raised
 */
public record ProductCreatedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID productId,
        String name,
        String sku,
        BigDecimal price
) {

    public static ProductCreatedEvent from(Product product) {
        return new ProductCreatedEvent(
                UUID.randomUUID().toString(),
                "product.created",
                "product-service",
                Instant.now(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice()
        );
    }
}
