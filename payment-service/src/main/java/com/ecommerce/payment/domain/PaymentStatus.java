package com.ecommerce.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    AWAITING_PAYMENT,
    PAID,
    FAILED,
    REFUNDED
}
