package com.ecommerce.payment.gateway;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub implementation for {@code payment.gateway=bb}: no real HTTP calls; exercises the same domain outcomes as integration tests.
 */
@Component
public class BbPaymentGatewayStub implements BbPaymentGatewayClient {

    private static final BigDecimal HIGH_VALUE = new BigDecimal("1000.00");

    @Override
    public GatewayResult processCharge(Payment payment) {
        return switch (payment.getMethod()) {
            case PIX -> GatewayResult.awaitingSettlement(
                    "bb-stub-pix-" + payment.getOrderId(),
                    "00020126580014br.gov.bcb.pix0136bbstub5204000053039865802BR5925Stub6009BR62070503***6304STUB",
                    "Stub: PIX charge registered");
            case BANK_SLIP -> GatewayResult.awaitingSettlement(
                    "bb-stub-slip-" + payment.getOrderId(),
                    "34191.79001 01043.510047 91020.150008 1 843500" + payment.getAmount().toPlainString(),
                    "Stub: boleto registered");
            case CREDIT_CARD, DEBIT_CARD -> simulateCard(payment);
        };
    }

    private GatewayResult simulateCard(Payment payment) {
        boolean high = payment.getAmount().compareTo(HIGH_VALUE) > 0;
        double decline = payment.getMethod() == PaymentMethod.CREDIT_CARD
                ? (high ? 0.30 : 0.10)
                : (high ? 0.20 : 0.08);
        if (Math.random() < decline) {
            return GatewayResult.declined("Stub: card declined");
        }
        return GatewayResult.approved("Stub: card captured");
    }
}
