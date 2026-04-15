package com.ecommerce.payment.gateway;

public record GatewayResult(boolean approved, String message) {

    public static GatewayResult approved(String message) {
        return new GatewayResult(true, message);
    }

    public static GatewayResult declined(String message) {
        return new GatewayResult(false, message);
    }
}
