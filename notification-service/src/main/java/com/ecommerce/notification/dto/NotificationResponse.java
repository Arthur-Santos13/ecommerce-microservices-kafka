package com.ecommerce.notification.dto;

import com.ecommerce.notification.domain.Notification;
import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.domain.NotificationStatus;
import com.ecommerce.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientId,
        String recipientEmail,
        NotificationType type,
        NotificationChannel channel,
        NotificationStatus status,
        String subject,
        String body,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getRecipientEmail(),
                n.getType(),
                n.getChannel(),
                n.getStatus(),
                n.getSubject(),
                n.getBody(),
                n.getErrorMessage(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
