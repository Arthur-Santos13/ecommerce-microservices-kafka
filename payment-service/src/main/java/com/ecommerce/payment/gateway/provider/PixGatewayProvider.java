package com.ecommerce.payment.gateway.provider;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import com.ecommerce.payment.gateway.GatewayProvider;
import com.ecommerce.payment.gateway.GatewayResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Simulates the Brazilian instant payment rail (PIX via Banco Central).
 *
 * Behaviour:
 * - 1% chance of transient unavailability (BACEN SPI is highly reliable)
 * - All other transactions are approved instantly
 */
@Component
public class PixGatewayProvider implements GatewayProvider {

    private static final double UNAVAILABLE_RATE = 0.01;

    @Override
    public GatewayResult process(Payment payment) {
        if (Math.random() < UNAVAILABLE_RATE) {
            throw new GatewayUnavailableException(
                    "PIX rail temporarily unavailable — orderId=" + payment.getOrderId());
        }

        return GatewayResult.approved("PIX payment confirmed instantly via SPI");
    }

    @Override
    public Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.PIX);
    }
}
