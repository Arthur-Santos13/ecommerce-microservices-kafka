package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void send(NotificationRequest request);

    List<NotificationResponse> findAll();

    List<NotificationResponse> findByRecipientId(UUID recipientId);
}
