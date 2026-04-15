package com.ecommerce.notification.listener;

import com.ecommerce.notification.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedNotificationHandler.class);

    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: orderId={}, customerId={}, reason={}",
                event.orderId(), event.customerId(), event.failureReason());
    }
}
