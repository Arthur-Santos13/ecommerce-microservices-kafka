package com.ecommerce.payment.event;

import com.ecommerce.payment.domain.Payment;

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

    public static PaymentConfirmedEvent from(Payment payment) {
        return new PaymentConfirmedEvent(
                UUID.randomUUID().toString(),
                "payment.confirmed",
                "payment-service",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount()
        );
    }
}
