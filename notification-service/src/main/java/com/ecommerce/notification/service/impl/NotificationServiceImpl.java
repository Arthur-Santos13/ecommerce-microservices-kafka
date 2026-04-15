package com.ecommerce.notification.service.impl;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public void send(NotificationRequest request) {
        log.info("Sending {} notification via {} to recipientId={}",
                request.type(), request.channel(), request.recipientId());
    }
}
