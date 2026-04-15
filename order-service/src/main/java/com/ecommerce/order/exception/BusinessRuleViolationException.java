package com.ecommerce.order.exception;

public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
