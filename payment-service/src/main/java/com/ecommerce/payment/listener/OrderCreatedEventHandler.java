package com.ecommerce.payment.listener;

import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, customerId={}, totalAmount={}",
                event.orderId(), event.customerId(), event.totalAmount());
        paymentService.processFromEvent(event);
    }
}
