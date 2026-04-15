package com.ecommerce.payment.gateway;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;

import java.util.Set;

public interface GatewayProvider {

    GatewayResult process(Payment payment);

    Set<PaymentMethod> supportedMethods();
}
