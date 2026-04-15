package com.ecommerce.payment.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        kafkaTemplate.send(paymentConfirmedTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment.confirmed event for order {}: {}",
                                event.orderId(), ex.getMessage());
                    } else {
                        log.info("Published payment.confirmed event for order {} to partition {}",
                                event.orderId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(paymentFailedTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment.failed event for order {}: {}",
                                event.orderId(), ex.getMessage());
                    } else {
                        log.info("Published payment.failed event for order {} to partition {}",
                                event.orderId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
