package com.ecommerce.product.service;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.domain.Inventory;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.event.ProductEventPublisher;
import com.ecommerce.product.exception.BusinessRuleViolationException;
import com.ecommerce.product.exception.DuplicateSkuException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl unit tests")
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductEventPublisher eventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;
    private ProductRequest validRequest;
    private Product existingProduct;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        validRequest = new ProductRequest(
                "Test Product",
                "A test description",
                new BigDecimal("99.90"),
                "SKU-001",
                10,
                null
        );

        inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .quantityInStock(10)
                .reservedQuantity(0)
                .updatedAt(LocalDateTime.now())
                .build();

        existingProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .description("A test description")
                .price(new BigDecimal("99.90"))
                .sku("SKU-001")
                .inventory(inventory)
                .version(0L)
                .build();
        existingProduct.onCreate();
        inventory.setProduct(existingProduct);
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates product and inventory, publishes event, returns response")
        void create_success() {
            given(productRepository.existsBySku("SKU-001")).willReturn(false);
            given(productRepository.save(any(Product.class))).willAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(productId);
                p.onCreate();
                return p;
            });
            given(inventoryRepository.save(any(Inventory.class))).willAnswer(inv -> {
                Inventory inv2 = inv.getArgument(0);
                inv2.setId(UUID.randomUUID());
                return inv2;
            });

            ProductResponse response = productService.create(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Test Product");
            assertThat(response.sku()).isEqualTo("SKU-001");
            assertThat(response.quantityInStock()).isEqualTo(10);

            verify(productRepository).save(any(Product.class));
            verify(inventoryRepository).save(any(Inventory.class));
            verify(eventPublisher).publishProductCreated(any());
        }

        @Test
        @DisplayName("throws DuplicateSkuException when SKU already exists")
        void create_duplicateSku_throwsException() {
            given(productRepository.existsBySku("SKU-001")).willReturn(true);

            assertThatThrownBy(() -> productService.create(validRequest))
                    .isInstanceOf(DuplicateSkuException.class)
                    .hasMessageContaining("SKU-001");

            verify(productRepository, never()).save(any());
            verify(eventPublisher, never()).publishProductCreated(any());
        }

        @Test
        @DisplayName("resolves category when categoryId is provided")
        void create_withCategory_resolvesCategory() {
            UUID categoryId = UUID.randomUUID();
            Category category = Category.builder()
                    .id(categoryId)
                    .name("Electronics")
                    .build();

            ProductRequest requestWithCategory = new ProductRequest(
                    "Phone", "Smartphone", new BigDecimal("1999.00"),
                    "SKU-PHONE-001", 5, categoryId);

            given(productRepository.existsBySku("SKU-PHONE-001")).willReturn(false);
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                p.onCreate();
                return p;
            });
            given(inventoryRepository.save(any(Inventory.class))).willAnswer(inv -> inv.getArgument(0));

            productService.create(requestWithCategory);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo(category);
        }
    }

    // ── findById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns response when product exists")
        void findById_found() {
            given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));

            ProductResponse response = productService.findById(productId);

            assertThat(response.id()).isEqualTo(productId);
            assertThat(response.sku()).isEqualTo("SKU-001");
        }

        @Test
        @DisplayName("throws ProductNotFoundException when product does not exist")
        void findById_notFound() {
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates product fields and publishes event")
        void update_success() {
            ProductRequest updateRequest = new ProductRequest(
                    "Updated Name", "New desc", new BigDecimal("149.90"),
                    "SKU-001", 20, null);

            given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));
            given(productRepository.save(any(Product.class))).willReturn(existingProduct);

            productService.update(productId, updateRequest);

            verify(productRepository).save(existingProduct);
            verify(eventPublisher).publishProductUpdated(any());
        }

        @Test
        @DisplayName("throws DuplicateSkuException when changing to an already-taken SKU")
        void update_skuConflict_throwsException() {
            ProductRequest updateRequest = new ProductRequest(
                    "Updated Name", "New desc", new BigDecimal("149.90"),
                    "SKU-TAKEN", 20, null);

            given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));
            given(productRepository.existsBySku("SKU-TAKEN")).willReturn(true);

            assertThatThrownBy(() -> productService.update(productId, updateRequest))
                    .isInstanceOf(DuplicateSkuException.class)
                    .hasMessageContaining("SKU-TAKEN");
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when new stock is below reserved quantity")
        void update_stockBelowReserved_throwsException() {
            inventory.setReservedQuantity(5);
            ProductRequest updateRequest = new ProductRequest(
                    "Test Product", "desc", new BigDecimal("99.90"),
                    "SKU-001", 3, null); // 3 < 5 reserved

            given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));

            assertThatThrownBy(() -> productService.update(productId, updateRequest))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("reserved");
        }
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("soft-deletes product and publishes event")
        void delete_success() {
            given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));
            given(productRepository.save(any(Product.class))).willReturn(existingProduct);

            productService.delete(productId);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getDeletedAt()).isNotNull();
            verify(eventPublisher).publishProductDeleted(any());
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when product has reserved stock")
        void delete_withReservedStock_throwsException() {
            inventory.setReservedQuantity(3);
            given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));

            assertThatThrownBy(() -> productService.delete(productId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("reserved");

            verify(productRepository, never()).save(any());
            verify(eventPublisher, never()).publishProductDeleted(any());
        }

        @Test
        @DisplayName("throws ProductNotFoundException when product does not exist")
        void delete_notFound() {
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.delete(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
