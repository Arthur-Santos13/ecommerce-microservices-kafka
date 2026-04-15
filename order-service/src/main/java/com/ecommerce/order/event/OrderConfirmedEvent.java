package com.ecommerce.order.event;

import com.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an order is confirmed by payment.
 * Topic: order.confirmed
 */
public record OrderConfirmedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount
) {

    public static OrderConfirmedEvent from(Order order) {
        return new OrderConfirmedEvent(
                UUID.randomUUID().toString(),
                "order.confirmed",
                "order-service",
                Instant.now(),
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount()
        );
    }
}
