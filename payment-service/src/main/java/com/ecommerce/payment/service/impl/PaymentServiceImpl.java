package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentStatus;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.exception.BusinessRuleViolationException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new BusinessRuleViolationException(
                    "A payment already exists for order: " + request.orderId());
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .customerId(request.customerId())
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        return PaymentResponse.from(
                paymentRepository.findById(id)
                        .orElseThrow(() -> new PaymentNotFoundException(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByOrderId(UUID orderId) {
        return PaymentResponse.from(
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new PaymentNotFoundException("orderId", orderId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findByCustomer(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
