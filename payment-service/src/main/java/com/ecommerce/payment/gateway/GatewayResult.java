package com.ecommerce.payment.gateway;

public record GatewayResult(
        Outcome outcome,
        String message,
        String externalTransactionId,
        String paymentInstructions
) {

    public enum Outcome {
        APPROVED,
        DECLINED,
        AWAITING_SETTLEMENT
    }

    public boolean approved() {
        return outcome == Outcome.APPROVED;
    }

    public boolean awaitingSettlement() {
        return outcome == Outcome.AWAITING_SETTLEMENT;
    }

    public static GatewayResult approved(String message) {
        return new GatewayResult(Outcome.APPROVED, message, null, null);
    }

    public static GatewayResult declined(String message) {
        return new GatewayResult(Outcome.DECLINED, message, null, null);
    }

    public static GatewayResult awaitingSettlement(
            String externalTransactionId,
            String paymentInstructions,
            String message) {
        return new GatewayResult(Outcome.AWAITING_SETTLEMENT, message, externalTransactionId, paymentInstructions);
    }
}
