package com.ecommerce.product.event;

import com.ecommerce.product.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a product's data is updated.
 * Topic: product.updated
 */
public record ProductUpdatedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID productId,
        String name,
        String sku,
        BigDecimal price
) {

    public static ProductUpdatedEvent from(Product product) {
        return new ProductUpdatedEvent(
                UUID.randomUUID().toString(),
                "product.updated",
                "product-service",
                Instant.now(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice()
        );
    }
}
