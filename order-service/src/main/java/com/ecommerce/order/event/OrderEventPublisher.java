package com.ecommerce.order.event;

import com.ecommerce.order.exception.KafkaPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            var sendResult = kafkaTemplate
                    .send(orderCreatedTopic, event.orderId().toString(), event)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Published order.created event for order {} to partition {}",
                    event.orderId(),
                    sendResult.getRecordMetadata().partition());
        } catch (Exception ex) {
            log.error("Failed to publish order.created event for order {}: {}",
                    event.orderId(), ex.getMessage());
            throw new KafkaPublishException(
                    "Failed to publish order.created event for order " + event.orderId(), ex);
        }
    }
}
