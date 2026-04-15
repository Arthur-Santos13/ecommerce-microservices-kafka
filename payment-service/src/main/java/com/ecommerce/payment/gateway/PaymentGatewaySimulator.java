package com.ecommerce.payment.gateway;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentGatewaySimulator {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.00");

    /**
     * Simulates an external payment gateway call.
     *
     * Rules:
     * - PIX always succeeds (instant payment rail, no card network involved)
     * - Amounts above 1000 have a 30% failure rate (risk of decline on high-value transactions)
     * - All other cases have a 10% failure rate
     */
    public GatewayResult process(Payment payment) {
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
