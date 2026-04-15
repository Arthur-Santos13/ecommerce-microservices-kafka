package com.ecommerce.notification.dto;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationType;

import java.util.UUID;

public record NotificationRequest(
        UUID recipientId,
        String recipientEmail,
        NotificationType type,
        NotificationChannel channel,
        String subject,
        String body
) {
}
