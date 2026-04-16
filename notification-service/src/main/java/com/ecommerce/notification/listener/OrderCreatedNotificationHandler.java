package com.ecommerce.notification.listener;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationType;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.event.OrderCreatedEvent;
import com.ecommerce.notification.exception.NotificationDeliveryException;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedNotificationHandler.class);

    private final NotificationService notificationService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            include = {NotificationDeliveryException.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, customerId={}, totalAmount={}",
                event.orderId(), event.customerId(), event.totalAmount());

        notificationService.send(new NotificationRequest(
                event.customerId(),
                null,
                NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL,
                "Order confirmed - #" + event.orderId(),
                "Your order has been confirmed. Total: " + event.totalAmount()
        ));
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, OrderCreatedEvent> record, Exception ex) {
        log.error("DLT: OrderCreatedEvent could not be processed after retries. topic={}, key={}, error={}",
                record.topic(), record.key(), ex.getMessage());
    }
}