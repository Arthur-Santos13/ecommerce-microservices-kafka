package com.ecommerce.payment.event;

import com.ecommerce.payment.exception.KafkaPublishException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        try {
            var result = kafkaTemplate
                    .send(paymentConfirmedTopic, event.orderId().toString(), event)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Published payment.confirmed event for order {} to partition {}",
                    event.orderId(), result.getRecordMetadata().partition());
        } catch (Exception ex) {
            log.error("Failed to publish payment.confirmed event for order {}: {}",
                    event.orderId(), ex.getMessage());
            throw new KafkaPublishException(
                    "Failed to publish payment.confirmed event for order " + event.orderId(), ex);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            var result = kafkaTemplate
                    .send(paymentFailedTopic, event.orderId().toString(), event)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Published payment.failed event for order {} to partition {}",
                    event.orderId(), result.getRecordMetadata().partition());
        } catch (Exception ex) {
            log.error("Failed to publish payment.failed event for order {}: {}",
                    event.orderId(), ex.getMessage());
            throw new KafkaPublishException(
                    "Failed to publish payment.failed event for order " + event.orderId(), ex);
        }
    }
}