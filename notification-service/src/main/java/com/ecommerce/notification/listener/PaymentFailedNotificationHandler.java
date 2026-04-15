package com.ecommerce.notification.listener;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationType;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.event.PaymentFailedEvent;
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
public class PaymentFailedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedNotificationHandler.class);

    private final NotificationService notificationService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            include = {NotificationDeliveryException.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: orderId={}, customerId={}, reason={}",
                event.orderId(), event.customerId(), event.failureReason());

        notificationService.send(new NotificationRequest(
                event.customerId(),
                null,
                NotificationType.PAYMENT_FAILED,
                NotificationChannel.EMAIL,
                "Payment failed - order #" + event.orderId(),
                "Unfortunately your payment could not be processed. Reason: " + event.failureReason()
        ));
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, PaymentFailedEvent> record, Exception ex) {
        log.error("DLT: PaymentFailedEvent could not be processed after retries. topic={}, key={}, error={}",
                record.topic(), record.key(), ex.getMessage());
    }
}