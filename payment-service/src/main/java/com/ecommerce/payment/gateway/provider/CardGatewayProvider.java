package com.ecommerce.payment.gateway.provider;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import com.ecommerce.payment.gateway.GatewayProvider;
import com.ecommerce.payment.gateway.GatewayResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Simulates a card acquirer (e.g. Cielo/Adyen) for CREDIT_CARD and DEBIT_CARD payments.
 *
 * Behaviour:
 * - 4% chance of transient gateway unavailability (triggers retry)
 * - CREDIT_CARD: 10% decline rate; 30% for amounts above R$1,000
 * - DEBIT_CARD: 8% decline rate; 20% for amounts above R$1,000 (lower risk, funds are guaranteed)
 */
@Component
public class CardGatewayProvider implements GatewayProvider {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.00");
    private static final double UNAVAILABLE_RATE = 0.04;

    @Override
    public GatewayResult process(Payment payment) {
        if (Math.random() < UNAVAILABLE_RATE) {
            throw new GatewayUnavailableException(
                    "Card acquirer temporarily unavailable — orderId=" + payment.getOrderId());
        }

        boolean highValue = payment.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0;

        double declineRate = switch (payment.getMethod()) {
            case CREDIT_CARD -> highValue ? 0.30 : 0.10;
            case DEBIT_CARD  -> highValue ? 0.20 : 0.08;
            default          -> 0.10;
        };

        if (Math.random() < declineRate) {
            return GatewayResult.declined("Card declined: insufficient funds or issuer refused");
        }

        return GatewayResult.approved("Card payment approved by acquirer");
    }

    @Override
    public Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD);
    }
}
