package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilter(
        String name,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock,
        UUID categoryId
) {}
