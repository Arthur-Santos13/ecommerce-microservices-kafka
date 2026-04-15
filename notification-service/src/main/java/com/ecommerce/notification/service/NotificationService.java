package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.NotificationRequest;

public interface NotificationService {

    void send(NotificationRequest request);
}
