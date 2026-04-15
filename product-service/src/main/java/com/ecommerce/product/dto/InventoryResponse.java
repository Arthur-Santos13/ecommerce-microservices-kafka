package com.ecommerce.product.dto;

import com.ecommerce.product.domain.Inventory;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        Integer quantityInStock,
        Integer reservedQuantity,
        Integer availableQuantity,
        LocalDateTime updatedAt
) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getQuantityInStock(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getUpdatedAt()
        );
    }
}
