package com.ecommerce.payment.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BbWebhookRequest(
        @NotBlank String notificationId,
        @NotNull UUID orderId,
        @NotNull BbWebhookStatus status,
        String failureReason,
        String externalTransactionId
) {
}
