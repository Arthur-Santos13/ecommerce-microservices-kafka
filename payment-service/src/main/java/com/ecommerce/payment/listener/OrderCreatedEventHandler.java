package com.ecommerce.payment.listener;

import com.ecommerce.payment.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, customerId={}, totalAmount={}, items={}",
                event.orderId(),
                event.customerId(),
                event.totalAmount(),
                event.items() != null ? event.items().size() : 0);
    }
}
