package com.ecommerce.payment.gateway;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentGatewaySimulator {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.00");
    private static final double GATEWAY_UNAVAILABLE_RATE = 0.05;

    /**
     * Simulates an external payment gateway call.
     *
     * Rules:
     * - 5% chance of transient gateway unavailability (triggers retry)
     * - PIX always succeeds (instant payment rail)
     * - Amounts above 1000 have a 30% decline rate
     * - All other cases have a 10% decline rate
     */
    public GatewayResult process(Payment payment) {
        if (Math.random() < GATEWAY_UNAVAILABLE_RATE) {
            throw new GatewayUnavailableException(
                    "Payment gateway temporarily unavailable — orderId=" + payment.getOrderId());
        }

        if (payment.getMethod() == PaymentMethod.PIX) {
            return GatewayResult.approved("PIX payment confirmed instantly");
        }

        double failureRate = payment.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0 ? 0.30 : 0.10;
        boolean approved = Math.random() >= failureRate;

        if (approved) {
            return GatewayResult.approved("Payment approved by gateway");
        }

        return GatewayResult.declined("Payment declined: insufficient funds or card refused");
    }
}
