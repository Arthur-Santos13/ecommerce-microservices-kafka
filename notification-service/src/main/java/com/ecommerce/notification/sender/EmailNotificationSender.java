package com.ecommerce.notification.sender;

import com.ecommerce.notification.domain.NotificationChannel;
import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.exception.NotificationDeliveryException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Delivers notifications via email using Spring Mail.
 * In development/test environments this targets a local SMTP mock
 * (e.g. Mailpit on port 1025) configured via MAIL_HOST / MAIL_PORT.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;

    @Override
    public void send(NotificationRequest request) {
        if (request.recipientEmail() == null || request.recipientEmail().isBlank()) {
            log.warn("Email notification skipped — no recipientEmail for recipientId={}",
                    request.recipientId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.recipientEmail());
            message.setSubject(request.subject());
            message.setText(request.body());
            mailSender.send(message);

            log.info("Email sent: type={}, to={}, subject={}",
                    request.type(), request.recipientEmail(), request.subject());
        } catch (Exception ex) {
            log.error("Failed to send email: type={}, to={}, error={}",
                    request.type(), request.recipientEmail(), ex.getMessage());
            throw new NotificationDeliveryException(
                    "Failed to deliver email to " + request.recipientEmail() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }
}
