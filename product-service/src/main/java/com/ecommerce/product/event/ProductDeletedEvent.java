package com.ecommerce.product.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a product is soft-deleted.
 * Topic: product.deleted
 */
public record ProductDeletedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID productId,
        String sku
) {

    public static ProductDeletedEvent of(UUID productId, String sku) {
        return new ProductDeletedEvent(
                UUID.randomUUID().toString(),
                "product.deleted",
                "product-service",
                Instant.now(),
                productId,
                sku
        );
    }
}
