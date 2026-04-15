package com.ecommerce.notification.listener;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationType;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.event.PaymentFailedEvent;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFailedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedNotificationHandler.class);

    private final NotificationService notificationService;

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
                "Payment failed — order #" + event.orderId(),
                "Unfortunately your payment could not be processed. Reason: " + event.failureReason()
        ));
    }
}
