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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.getQuantityInStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
