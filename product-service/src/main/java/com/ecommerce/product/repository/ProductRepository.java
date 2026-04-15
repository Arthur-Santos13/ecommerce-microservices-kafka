package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    @Query(value = "SELECT * FROM products WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Product> findDeletedById(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE products SET deleted_at = NULL, updated_at = NOW() WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") UUID id);
}
