package com.ecommerce.notification.controller;

import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Returns notifications.
     * - ADMIN + no recipientId  → returns all notifications
     * - ADMIN + recipientId     → returns notifications for that recipient
     * - USER  + recipientId     → returns notifications for that recipient
     * - USER  + no recipientId  → 403
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> findNotifications(
            @RequestParam(required = false) UUID recipientId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        boolean isAdmin = rolesHeader != null
                && Arrays.asList(rolesHeader.split(",")).contains("ADMIN");

        if (recipientId == null) {
            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(notificationService.findAll());
        }

        return ResponseEntity.ok(notificationService.findByRecipientId(recipientId));
    }
}
