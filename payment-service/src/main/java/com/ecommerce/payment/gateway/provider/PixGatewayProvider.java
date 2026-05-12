package com.ecommerce.payment.gateway.provider;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import com.ecommerce.payment.gateway.GatewayProvider;
import com.ecommerce.payment.gateway.GatewayResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Simulates PIX charge creation; settlement is asynchronous.
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

        String txid = "sim-pix-" + payment.getOrderId();
        String emv = "00020126580014br.gov.bcb.pix0136" + txid + "5204000053039865802BR5925Ecommerce6009SaoPaulo62070503***6304ABCD";
        return GatewayResult.awaitingSettlement(
                txid,
                emv,
                "PIX charge created — awaiting payer confirmation");
    }

    @Override
    public Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.PIX);
    }
}
