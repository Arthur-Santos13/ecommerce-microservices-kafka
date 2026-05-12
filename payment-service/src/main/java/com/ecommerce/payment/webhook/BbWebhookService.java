package com.ecommerce.payment.webhook;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.domain.PaymentStatus;
import com.ecommerce.payment.domain.PaymentTransaction;
import com.ecommerce.payment.domain.ProcessedEvent;
import com.ecommerce.payment.event.PaymentConfirmedEvent;
import com.ecommerce.payment.event.PaymentEventPublisher;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.payment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BbWebhookService {

    private static final Logger log = LoggerFactory.getLogger(BbWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public void handle(BbWebhookRequest request) {
        String eventId = "bb-webhook:" + request.notificationId();
        if (processedEventRepository.existsByEventId(eventId)) {
            log.debug("Duplicate bank webhook ignored: notificationId={}", request.notificationId());
            return;
        }

        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new PaymentNotFoundException("orderId", request.orderId()));

        if (request.externalTransactionId() != null
                && payment.getExternalTransactionId() != null
                && !request.externalTransactionId().equals(payment.getExternalTransactionId())) {
            log.warn("Webhook external id mismatch for orderId={}: expected {}, got {}",
                    request.orderId(), payment.getExternalTransactionId(), request.externalTransactionId());
        }

        switch (request.status()) {
            case PAID -> applyPaid(payment);
            case FAILED -> applyFailed(payment, request.failureReason());
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("bb.payment.webhook")
                .build());
    }

    private void applyPaid(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Webhook PAID ignored: payment already settled orderId={}", payment.getOrderId());
            return;
        }
        if (payment.getStatus() != PaymentStatus.AWAITING_PAYMENT) {
            log.warn("Webhook PAID ignored: payment status is {} for orderId={}",
                    payment.getStatus(), payment.getOrderId());
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        recordTransaction(payment);
        eventPublisher.publishPaymentConfirmed(PaymentConfirmedEvent.from(payment));
        log.info("Payment confirmed via bank webhook: orderId={}", payment.getOrderId());
    }

    private void applyFailed(Payment payment, String failureReason) {
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Webhook FAILED ignored: payment already failed orderId={}", payment.getOrderId());
            return;
        }
        if (payment.getStatus() != PaymentStatus.AWAITING_PAYMENT) {
            log.warn("Webhook FAILED ignored: payment status is {} for orderId={}",
                    payment.getStatus(), payment.getOrderId());
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason != null ? failureReason : "Rejected by bank webhook");
        paymentRepository.save(payment);
        recordTransaction(payment);
        eventPublisher.publishPaymentFailed(PaymentFailedEvent.from(payment));
        log.info("Payment failed via bank webhook: orderId={}", payment.getOrderId());
    }

    private void recordTransaction(Payment payment) {
        transactionRepository.save(PaymentTransaction.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build());
    }
}
