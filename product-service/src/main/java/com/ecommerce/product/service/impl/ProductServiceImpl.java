package com.ecommerce.product.service.impl;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.domain.Inventory;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.PageResponse;
import com.ecommerce.product.dto.ProductFilter;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.event.ProductCreatedEvent;
import com.ecommerce.product.event.ProductDeletedEvent;
import com.ecommerce.product.event.ProductEventPublisher;
import com.ecommerce.product.event.ProductUpdatedEvent;
import com.ecommerce.product.exception.BusinessRuleViolationException;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.DuplicateSkuException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSpecification;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final ProductEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(
                productRepository
                        .findAll(ProductSpecification.fromFilter(filter), pageable)
                        .map(ProductResponse::from)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#result.id()")
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Category category = resolveCategory(request.categoryId());

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .sku(request.sku())
                .category(category)
                .build();

        Product saved = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(saved)
                .quantityInStock(request.quantityInStock())
                .reservedQuantity(0)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);
        saved.setInventory(savedInventory);

        eventPublisher.publishProductCreated(ProductCreatedEvent.from(saved));
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#id")
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        boolean skuChangedToExisting = !product.getSku().equals(request.sku())
                && productRepository.existsBySku(request.sku());

        if (skuChangedToExisting) {
            throw new DuplicateSkuException(request.sku());
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());
        product.setCategory(resolveCategory(request.categoryId()));

        if (product.getInventory() != null) {
            int reserved = product.getInventory().getReservedQuantity();
            if (request.quantityInStock() < reserved) {
                throw new BusinessRuleViolationException(
                        "Cannot set stock to " + request.quantityInStock()
                                + ": " + reserved + " units are currently reserved");
            }
            product.getInventory().setQuantityInStock(request.quantityInStock());
            inventoryRepository.save(product.getInventory());
        }

        Product saved = productRepository.save(product);
        eventPublisher.publishProductUpdated(ProductUpdatedEvent.from(saved));
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (product.getInventory() != null && product.getInventory().getReservedQuantity() > 0) {
            throw new BusinessRuleViolationException(
                    "Cannot delete product " + id + ": "
                            + product.getInventory().getReservedQuantity()
                            + " units are currently reserved");
        }

        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        eventPublisher.publishProductDeleted(ProductDeletedEvent.of(product.getId(), product.getSku()));
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#id")
    public ProductResponse restore(UUID id) {
        productRepository.findDeletedById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.restoreById(id);
        Product restored = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(restored);
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}