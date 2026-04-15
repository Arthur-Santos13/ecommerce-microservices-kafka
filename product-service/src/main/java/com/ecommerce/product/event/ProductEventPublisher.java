package com.ecommerce.product.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.product-created}")
    private String productCreatedTopic;

    @Value("${kafka.topics.product-updated}")
    private String productUpdatedTopic;

    @Value("${kafka.topics.product-deleted}")
    private String productDeletedTopic;

    public void publishProductCreated(ProductCreatedEvent event) {
        kafkaTemplate.send(productCreatedTopic, event.productId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish product.created event for product {}: {}",
                                event.productId(), ex.getMessage());
                    } else {
                        log.info("Published product.created event for product {} to partition {}",
                                event.productId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    public void publishProductUpdated(ProductUpdatedEvent event) {
        kafkaTemplate.send(productUpdatedTopic, event.productId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish product.updated event for product {}: {}",
                                event.productId(), ex.getMessage());
                    } else {
                        log.info("Published product.updated event for product {} to partition {}",
                                event.productId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    public void publishProductDeleted(ProductDeletedEvent event) {
        kafkaTemplate.send(productDeletedTopic, event.productId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish product.deleted event for product {}: {}",
                                event.productId(), ex.getMessage());
                    } else {
                        log.info("Published product.deleted event for product {} to partition {}",
                                event.productId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
