package com.ecommerce.order.domain;

/**
 * Payment rail selected at checkout. Values align with {@code payment-service} JSON contract.
 */
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    PIX,
    BANK_SLIP
}
