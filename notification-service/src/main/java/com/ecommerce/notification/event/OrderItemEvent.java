package com.ecommerce.notification.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemEvent(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
