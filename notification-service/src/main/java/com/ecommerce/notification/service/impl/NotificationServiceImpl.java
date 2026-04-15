package com.ecommerce.notification.service.impl;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.sender.NotificationSender;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final List<NotificationSender> senders;

    @Override
    public void send(NotificationRequest request) {
        log.info("Dispatching {} notification via {} to recipientId={}",
                request.type(), request.channel(), request.recipientId());

        senders.stream()
                .filter(s -> s.channel() == request.channel())
                .findFirst()
                .ifPresentOrElse(
                        s -> s.send(request),
                        () -> log.warn("No sender registered for channel={}", request.channel())
                );
    }
}
