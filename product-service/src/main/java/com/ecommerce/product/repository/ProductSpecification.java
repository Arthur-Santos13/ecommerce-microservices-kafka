package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.domain.Inventory;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.ProductFilter;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> nameLike(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> {
            Join<Product, Category> cat = root.join("category", JoinType.INNER);
            return cb.equal(cat.get("id"), categoryId);
        };
    }

    /**
     * Filters products where availableQuantity (quantityInStock - reservedQuantity) > 0.
     * Uses INNER JOIN with inventory so products without inventory records are excluded.
     */
    public static Specification<Product> inStock() {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Product, Inventory> inv = root.join("inventory", JoinType.INNER);
            Expression<Integer> available = cb.diff(
                    inv.<Integer>get("quantityInStock"),
                    inv.<Integer>get("reservedQuantity")
            );
            return cb.greaterThan(available, 0);
        };
    }

    public static Specification<Product> fromFilter(ProductFilter filter) {
        return Specification
                .where(filter.name() != null ? nameLike(filter.name()) : null)
                .and(filter.minPrice() != null ? priceGreaterThanOrEqual(filter.minPrice()) : null)
                .and(filter.maxPrice() != null ? priceLessThanOrEqual(filter.maxPrice()) : null)
                .and(Boolean.TRUE.equals(filter.inStock()) ? inStock() : null)
                .and(filter.categoryId() != null ? hasCategory(filter.categoryId()) : null);
    }
}
