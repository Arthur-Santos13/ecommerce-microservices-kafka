package com.ecommerce.product.service;

import com.ecommerce.product.domain.Inventory;
import com.ecommerce.product.dto.InventoryResponse;
import com.ecommerce.product.dto.StockAdjustmentRequest;
import com.ecommerce.product.exception.BusinessRuleViolationException;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl unit tests")
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private UUID productId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .quantityInStock(10)
                .reservedQuantity(2)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── restock ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("restock()")
    class Restock {

        @Test
        @DisplayName("increments stock by request quantity")
        void restock_addsQuantity() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.of(inventory));
            given(inventoryRepository.save(inventory)).willReturn(inventory);

            inventoryService.restock(productId, new StockAdjustmentRequest(5, "Supplier delivery"));

            assertThat(inventory.getQuantityInStock()).isEqualTo(15);
            verify(inventoryRepository).save(inventory);
        }
    }

    // ── reserve ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reserve()")
    class Reserve {

        @Test
        @DisplayName("increases reserved quantity when sufficient stock is available")
        void reserve_success() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.of(inventory));
            given(inventoryRepository.save(inventory)).willReturn(inventory);

            // available = 10 - 2 = 8; reserve 3 → reserved becomes 5
            inventoryService.reserve(productId, new StockAdjustmentRequest(3, "Order reservation"));

            assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("throws InsufficientStockException when requested quantity exceeds available")
        void reserve_insufficientStock() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.of(inventory));

            // available = 8; trying to reserve 9
            assertThatThrownBy(() -> inventoryService.reserve(
                    productId, new StockAdjustmentRequest(9, "Order reservation")))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining(productId.toString());
        }

        @Test
        @DisplayName("throws ProductNotFoundException when inventory does not exist")
        void reserve_notFound() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.reserve(
                    productId, new StockAdjustmentRequest(1, "Order reservation")))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ── release ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("decreases reserved quantity on successful release")
        void release_success() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.of(inventory));
            given(inventoryRepository.save(inventory)).willReturn(inventory);

            // reserved = 2; release 2 → reserved becomes 0
            inventoryService.release(productId, new StockAdjustmentRequest(2, "Order cancelled"));

            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when releasing more than reserved")
        void release_exceedsReserved() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.of(inventory));

            // reserved = 2; trying to release 5
            assertThatThrownBy(() -> inventoryService.release(
                    productId, new StockAdjustmentRequest(5, "Order cancelled")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot release");
        }
    }

    // ── findByProductId ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByProductId()")
    class FindByProductId {

        @Test
        @DisplayName("returns inventory response when product exists")
        void findByProductId_found() {
            given(inventoryRepository.findByProduct_Id(productId)).willReturn(Optional.of(inventory));

            InventoryResponse response = inventoryService.findByProductId(productId);

            assertThat(response).isNotNull();
            assertThat(response.quantityInStock()).isEqualTo(10);
            assertThat(response.reservedQuantity()).isEqualTo(2);
            assertThat(response.availableQuantity()).isEqualTo(8);
        }
    }
}
