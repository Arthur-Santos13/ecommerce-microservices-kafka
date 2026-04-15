package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse create(PaymentRequest request);

    PaymentResponse findById(UUID id);

    PaymentResponse findByOrderId(UUID orderId);

    List<PaymentResponse> findByCustomer(UUID customerId);
}
