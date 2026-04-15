package com.ecommerce.notification.sender;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS sender stub — logs the notification payload.
 * Designed to be replaced by a real SMS provider (e.g. Twilio, AWS SNS)
 * without changing any caller code: just swap this bean for a real implementation.
 */
@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public void send(NotificationRequest request) {
        log.info("[SMS MOCK] type={}, recipientId={}, body={}",
                request.type(), request.recipientId(), request.body());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }
}
