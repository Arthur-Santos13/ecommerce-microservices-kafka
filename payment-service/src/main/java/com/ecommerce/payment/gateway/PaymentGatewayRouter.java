package com.ecommerce.payment.gateway;

import com.ecommerce.payment.config.PaymentGatewayProperties;
import com.ecommerce.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final PaymentGatewayProperties properties;
    private final PaymentGatewaySimulator simulator;
    private final BbPaymentGatewayClient bbPaymentGatewayClient;

    public GatewayResult process(Payment payment) {
        if ("bb".equalsIgnoreCase(properties.getGateway())) {
            return bbPaymentGatewayClient.processCharge(payment);
        }
        return simulator.process(payment);
    }
}
