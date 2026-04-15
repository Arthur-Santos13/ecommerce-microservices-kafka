package com.ecommerce.product.dto;

import com.ecommerce.product.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Integer quantityInStock,
        Integer reservedQuantity,
        Integer availableQuantity,
        CategoryResponse category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponse from(Product product) {
        Integer qty      = product.getInventory() != null ? product.getInventory().getQuantityInStock() : 0;
        Integer reserved = product.getInventory() != null ? product.getInventory().getReservedQuantity() : 0;
        CategoryResponse cat = product.getCategory() != null
                ? CategoryResponse.from(product.getCategory()) : null;
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                qty,
                reserved,
                qty - reserved,
                cat,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
