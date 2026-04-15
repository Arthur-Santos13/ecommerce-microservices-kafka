package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentMethod;
import com.ecommerce.payment.domain.PaymentStatus;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.exception.BusinessRuleViolationException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.gateway.GatewayResult;
import com.ecommerce.payment.gateway.PaymentGatewaySimulator;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator gatewaySimulator;

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
    @Transactional
    public PaymentResponse processFromEvent(OrderCreatedEvent event) {
        return paymentRepository.findByOrderId(event.orderId())
                .map(existing -> {
                    log.warn("Payment already exists for orderId={}, skipping processing", event.orderId());
                    return PaymentResponse.from(existing);
                })
                .orElseGet(() -> doProcess(event));
    }

    private PaymentResponse doProcess(OrderCreatedEvent event) {
        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        GatewayResult result = gatewaySimulator.process(payment);

        if (result.approved()) {
            payment.setStatus(PaymentStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.message());
        }

        payment = paymentRepository.save(payment);
        log.info("Payment processed: orderId={}, status={}, message={}",
                event.orderId(), payment.getStatus(), result.message());

        return PaymentResponse.from(payment);
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