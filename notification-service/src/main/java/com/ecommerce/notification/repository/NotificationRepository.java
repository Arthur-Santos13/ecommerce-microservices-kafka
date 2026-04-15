package com.ecommerce.notification.repository;

import com.ecommerce.notification.domain.Notification;
import com.ecommerce.notification.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientId(UUID recipientId);

    List<Notification> findByStatus(NotificationStatus status);
}
