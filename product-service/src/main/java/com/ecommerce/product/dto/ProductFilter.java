package com.ecommerce.product.dto;

import java.math.BigDecimal;

/**
 * Filtering criteria for product list queries.
 * categoryId will be added when product categories are introduced.
 */
public record ProductFilter(
        String name,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock
) {}
