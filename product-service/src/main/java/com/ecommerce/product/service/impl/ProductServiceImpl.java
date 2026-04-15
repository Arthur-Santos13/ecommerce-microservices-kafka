package com.ecommerce.product.service.impl;

import com.ecommerce.product.domain.Inventory;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.PageResponse;
import com.ecommerce.product.dto.ProductFilter;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.DuplicateSkuException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSpecification;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

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
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .sku(request.sku())
                .build();

        Product saved = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(saved)
                .quantityInStock(request.quantityInStock())
                .reservedQuantity(0)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);
        saved.setInventory(savedInventory);

        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
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

        if (product.getInventory() != null) {
            product.getInventory().setQuantityInStock(request.quantityInStock());
            inventoryRepository.save(product.getInventory());
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .sku(request.sku())
                .build();

        Product saved = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(saved)
                .quantityInStock(request.quantityInStock())
                .reservedQuantity(0)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);
        saved.setInventory(savedInventory);

        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
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

        if (product.getInventory() != null) {
            product.getInventory().setQuantityInStock(request.quantityInStock());
            inventoryRepository.save(product.getInventory());
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
