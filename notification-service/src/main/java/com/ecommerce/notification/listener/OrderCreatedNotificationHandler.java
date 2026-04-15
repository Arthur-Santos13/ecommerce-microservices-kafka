package com.ecommerce.notification.listener;

import com.ecommerce.notification.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedNotificationHandler.class);

    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, customerId={}, totalAmount={}",
                event.orderId(), event.customerId(), event.totalAmount());
    }
}
