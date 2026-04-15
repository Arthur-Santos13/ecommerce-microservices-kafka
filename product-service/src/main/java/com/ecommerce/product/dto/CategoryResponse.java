package com.ecommerce.product.dto;

import com.ecommerce.product.domain.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
