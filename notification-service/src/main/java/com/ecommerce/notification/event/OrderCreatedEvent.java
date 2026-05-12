package com.ecommerce.notification.event;

import com.ecommerce.notification.domain.PaymentMethod;

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
        List<OrderItemEvent> items,
        PaymentMethod paymentMethod
) {
}
