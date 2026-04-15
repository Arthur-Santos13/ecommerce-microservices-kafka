package com.ecommerce.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
}
