package com.ecommerce.notification.sender;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.dto.NotificationRequest;

public interface NotificationSender {

    void send(NotificationRequest request);

    NotificationChannel channel();
}
