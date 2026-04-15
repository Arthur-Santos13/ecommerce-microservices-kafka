package com.ecommerce.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must be zero or greater")
        BigDecimal price,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$",
                message = "SKU must contain only letters, numbers, hyphens and underscores")
        String sku,

        @NotNull(message = "Quantity in stock is required")
        @Min(value = 0, message = "Quantity in stock must be zero or greater")
        Integer quantityInStock,

        /** Optional — links the product to a category. Null is allowed. */
        UUID categoryId
) {}
