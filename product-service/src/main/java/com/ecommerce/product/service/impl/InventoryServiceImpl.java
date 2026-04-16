package com.ecommerce.product.service.impl;

import com.ecommerce.product.domain.Inventory;
import com.ecommerce.product.dto.InventoryResponse;
import com.ecommerce.product.dto.StockAdjustmentRequest;
import com.ecommerce.product.exception.BusinessRuleViolationException;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse findByProductId(UUID productId) {
        return InventoryResponse.from(getOrThrow(productId));
    }

    @Override
    @Transactional
    public InventoryResponse restock(UUID productId, StockAdjustmentRequest request) {
        Inventory inventory = getOrThrow(productId);
        inventory.setQuantityInStock(inventory.getQuantityInStock() + request.quantity());
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional
    public InventoryResponse reserve(UUID productId, StockAdjustmentRequest request) {
        Inventory inventory = getOrThrow(productId);

        int available = inventory.getAvailableQuantity();
        if (request.quantity() > available) {
            throw new InsufficientStockException(productId, request.quantity(), available);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional
    public InventoryResponse release(UUID productId, StockAdjustmentRequest request) {
        Inventory inventory = getOrThrow(productId);

        if (request.quantity() > inventory.getReservedQuantity()) {
            throw new BusinessRuleViolationException(
                    "Cannot release " + request.quantity() + " units: only "
                            + inventory.getReservedQuantity() + " are reserved for product " + productId);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - request.quantity());
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    private Inventory getOrThrow(UUID productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
