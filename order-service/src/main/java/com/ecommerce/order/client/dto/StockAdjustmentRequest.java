package com.ecommerce.order.client.dto;

public record StockAdjustmentRequest(int quantity, String reason) {
}
