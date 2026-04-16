package com.ecommerce.payment.listener;

import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.exception.GatewayUnavailableException;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    private final PaymentService paymentService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            dltTopicSuffix = "-dlt",
            autoCreateTopics = "true",
            include = {GatewayUnavailableException.class}
    )
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

    @DltHandler
    public void handleDlt(OrderCreatedEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Event exhausted all retries — DLT: topic={}, orderId={}, customerId={}",
                topic, event.orderId(), event.customerId());
        paymentService.processFromDlt(event);
    }
}
