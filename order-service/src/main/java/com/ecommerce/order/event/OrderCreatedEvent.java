package com.ecommerce.order.event;

import com.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a new order is confirmed.
 * Topic: order.created
 *
 * Standard event envelope fields:
 *   eventId     — unique event identifier (UUID)
 *   eventType   — dot-separated type name (service.action)
 *   source      — originating service
 *   occurredOn  — UTC instant the event was raised
 */
public record OrderCreatedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount,
        List<OrderItemEvent> items
) {

    public static OrderCreatedEvent from(Order order) {
        List<OrderItemEvent> items = order.getItems().stream()
                .map(OrderItemEvent::from)
                .toList();
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "order.created",
                "order-service",
                Instant.now(),
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                items
        );
    }
}
