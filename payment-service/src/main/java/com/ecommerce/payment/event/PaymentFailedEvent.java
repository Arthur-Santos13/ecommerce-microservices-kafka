package com.ecommerce.payment.event;

import com.ecommerce.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        String eventId,
        String eventType,
        String source,
        Instant occurredOn,
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String failureReason
) {

    public static PaymentFailedEvent from(Payment payment) {
        return new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                "payment.failed",
                "payment-service",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getFailureReason()
        );
    }
}
