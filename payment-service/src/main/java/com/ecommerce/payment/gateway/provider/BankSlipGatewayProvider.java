package com.ecommerce.payment.gateway.provider;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import com.ecommerce.payment.gateway.GatewayProvider;
import com.ecommerce.payment.gateway.GatewayResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Simulates a bank slip (boleto bancário) issuer.
 *
 * Behaviour:
 * - 2% chance of transient unavailability (PSP outage)
 * - Slip generation always succeeds when the provider is reachable;
 *   actual payment confirmation happens offline (out of scope for this simulation)
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

        return GatewayResult.approved("Bank slip generated successfully");
    }

    @Override
    public Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.BANK_SLIP);
    }
}
