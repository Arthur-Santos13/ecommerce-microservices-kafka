package com.ecommerce.notification.service.impl;

import com.ecommerce.notification.domain.Notification;
import com.ecommerce.notification.domain.NotificationStatus;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.repository.NotificationRepository;
import com.ecommerce.notification.sender.NotificationSender;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final List<NotificationSender> senders;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void send(NotificationRequest request) {
        log.info("Dispatching {} notification via {} to recipientId={}",
                request.type(), request.channel(), request.recipientId());

        Notification notification = notificationRepository.save(Notification.builder()
                .recipientId(request.recipientId())
                .recipientEmail(request.recipientEmail())
                .type(request.type())
                .channel(request.channel())
                .status(NotificationStatus.PENDING)
                .subject(request.subject())
                .body(request.body())
                .build());

        senders.stream()
                .filter(s -> s.channel() == request.channel())
                .findFirst()
                .ifPresentOrElse(
                        sender -> {
                            try {
                                sender.send(request);
                                notification.setStatus(NotificationStatus.SENT);
                                log.info("Notification sent: id={}, type={}, channel={}",
                                        notification.getId(), notification.getType(), notification.getChannel());
                            } catch (Exception ex) {
                                notification.setStatus(NotificationStatus.FAILED);
                                notification.setErrorMessage(ex.getMessage());
                                log.error("Notification delivery failed: id={}, error={}",
                                        notification.getId(), ex.getMessage());
                                throw ex;
                            } finally {
                                notificationRepository.save(notification);
                            }
                        },
                        () -> {
                            notification.setStatus(NotificationStatus.FAILED);
                            notification.setErrorMessage("No sender registered for channel: " + request.channel());
                            notificationRepository.save(notification);
                            log.warn("No sender registered for channel={}", request.channel());
                        }
                );
    }
}
