package com.ecommerce.payment.gateway;

import com.ecommerce.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Routes payment processing to the appropriate {@link GatewayProvider}
 * based on the payment method. Acts as a facade over provider-specific simulators.
 */
@Service
@RequiredArgsConstructor
public class PaymentGatewaySimulator {

    private final List<GatewayProvider> providers;

    public GatewayResult process(Payment payment) {
        return providers.stream()
                .filter(p -> p.supportedMethods().contains(payment.getMethod()))
                .findFirst()
                .map(p -> p.process(payment))
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No gateway provider found for method: " + payment.getMethod()));
    }
}

