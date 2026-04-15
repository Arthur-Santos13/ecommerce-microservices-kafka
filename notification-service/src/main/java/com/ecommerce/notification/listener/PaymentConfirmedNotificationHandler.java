package com.ecommerce.notification.listener;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationType;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.event.PaymentConfirmedEvent;
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
public class PaymentConfirmedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedNotificationHandler.class);

    private final NotificationService notificationService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            include = {NotificationDeliveryException.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "${kafka.topics.payment-confirmed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(PaymentConfirmedEvent event) {
        log.info("Received PaymentConfirmedEvent: orderId={}, customerId={}, amount={}",
                event.orderId(), event.customerId(), event.amount());

        notificationService.send(new NotificationRequest(
                event.customerId(),
                null,
                NotificationType.PAYMENT_CONFIRMED,
                NotificationChannel.EMAIL,
                "Payment confirmed - order #" + event.orderId(),
                "Your payment of " + event.amount() + " has been confirmed."
        ));
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, PaymentConfirmedEvent> record, Exception ex) {
        log.error("DLT: PaymentConfirmedEvent could not be processed after retries. topic={}, key={}, error={}",
                record.topic(), record.key(), ex.getMessage());
    }
}