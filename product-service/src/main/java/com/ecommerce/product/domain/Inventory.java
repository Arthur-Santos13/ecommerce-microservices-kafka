package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inventory aggregate — purposefully kept as an inner module of product-service.
 *
 * It can be extracted into a dedicated inventory-service when one or more of these
 * criteria are met:
 *  - Distinct team ownership (logistics vs. catalog)
 *  - Write throughput significantly higher than the product catalog
 *  - External ERP / warehouse integrations are required
 *  - Multi-warehouse / RFID complexity grows beyond this service
 *
 * Until then, collocating Inventory with Product gives transactional consistency
 * at zero additional operational cost.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    /** Total physical units in warehouse. */
    @Column(nullable = false)
    private Integer quantityInStock;

    /** Units currently reserved for pending orders. Cannot exceed quantityInStock. */
    @Column(nullable = false)
    private Integer reservedQuantity;

    /** Computed: units available for new reservations. Not persisted. */
    @Transient
    public Integer getAvailableQuantity() {
        return quantityInStock - reservedQuantity;
    }

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
