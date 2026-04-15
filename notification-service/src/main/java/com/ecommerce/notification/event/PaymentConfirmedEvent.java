package com.ecommerce.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount
) {
}
