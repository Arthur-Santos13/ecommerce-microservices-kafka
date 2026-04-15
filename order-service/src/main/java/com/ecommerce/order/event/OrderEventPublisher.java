package com.ecommerce.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(orderCreatedTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order.created event for order {}: {}",
                                event.orderId(), ex.getMessage());
                    } else {
                        log.info("Published order.created event for order {} to partition {}",
                                event.orderId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
