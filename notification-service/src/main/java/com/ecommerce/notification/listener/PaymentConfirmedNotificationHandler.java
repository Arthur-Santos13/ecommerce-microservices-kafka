package com.ecommerce.notification.listener;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationType;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.event.PaymentConfirmedEvent;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConfirmedNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedNotificationHandler.class);

    private final NotificationService notificationService;

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
                "Payment confirmed — order #" + event.orderId(),
                "Your payment of " + event.amount() + " has been confirmed."
        ));
    }
}
