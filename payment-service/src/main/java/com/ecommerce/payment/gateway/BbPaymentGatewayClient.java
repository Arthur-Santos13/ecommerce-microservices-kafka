package com.ecommerce.payment.gateway;

import com.ecommerce.payment.domain.Payment;

/**
 * Port for bank (BB) charge APIs. Production wiring uses HTTPS client + env-held secrets.
 */
public interface BbPaymentGatewayClient {

    GatewayResult processCharge(Payment payment);
}
