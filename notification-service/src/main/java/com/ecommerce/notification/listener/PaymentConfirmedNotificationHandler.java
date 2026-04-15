package com.ecommerce.notification.listener;

import com.ecommerce.notification.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedNotificationHandler.class);

    @KafkaListener(
            topics = "${kafka.topics.payment-confirmed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(PaymentConfirmedEvent event) {
        log.info("Received PaymentConfirmedEvent: orderId={}, customerId={}, amount={}",
                event.orderId(), event.customerId(), event.amount());
    }
}
