package com.ecommerce.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Local copy of the PaymentConfirmedEvent contract.
 * Published by payment-service on topic: payment.confirmed
 */
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
