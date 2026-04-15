package com.ecommerce.order.event;

import com.ecommerce.order.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemEvent(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {

    public static OrderItemEvent from(OrderItem item) {
        return new OrderItemEvent(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
