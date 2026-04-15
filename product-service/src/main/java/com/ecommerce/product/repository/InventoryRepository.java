package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProduct_Id(UUID productId);

    boolean existsByProduct_Id(UUID productId);
}
