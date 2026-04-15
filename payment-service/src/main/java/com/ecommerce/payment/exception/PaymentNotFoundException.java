package com.ecommerce.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID id) {
        super("Payment not found: " + id);
    }

    public PaymentNotFoundException(String field, UUID value) {
        super("Payment not found for " + field + ": " + value);
    }
}
