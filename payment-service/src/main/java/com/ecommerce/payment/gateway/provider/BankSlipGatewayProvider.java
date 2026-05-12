package com.ecommerce.payment.gateway.provider;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import com.ecommerce.payment.gateway.GatewayProvider;
import com.ecommerce.payment.gateway.GatewayResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Simulates boleto registration; settlement is asynchronous.
 */
@Component
public class BankSlipGatewayProvider implements GatewayProvider {

    private static final double UNAVAILABLE_RATE = 0.02;

    @Override
    public GatewayResult process(Payment payment) {
        if (Math.random() < UNAVAILABLE_RATE) {
            throw new GatewayUnavailableException(
                    "Bank slip issuer temporarily unavailable — orderId=" + payment.getOrderId());
        }

        String slipId = "sim-slip-" + payment.getOrderId();
        String line = "34191.79001 01043.510047 91020.150008 1 843500" + payment.getAmount().toPlainString();
        return GatewayResult.awaitingSettlement(
                slipId,
                line,
                "Bank slip registered — awaiting settlement");
    }

    @Override
    public Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.BANK_SLIP);
    }
}
