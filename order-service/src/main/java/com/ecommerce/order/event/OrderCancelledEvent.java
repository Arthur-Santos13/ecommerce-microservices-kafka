package com.ecommerce.order.event;

import com.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an order is cancelled (e.g. after payment failure compensation).
 * Topic: order.cancelled
 */
public record OrderCancelledEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount,
        String failureReason
) {

    public static OrderCancelledEvent from(Order order) {
        return new OrderCancelledEvent(
                UUID.randomUUID().toString(),
                "order.cancelled",
                "order-service",
                Instant.now(),
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getFailureReason()
        );
    }
}
