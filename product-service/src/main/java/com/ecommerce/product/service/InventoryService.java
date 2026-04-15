package com.ecommerce.product.service;

import com.ecommerce.product.dto.InventoryResponse;
import com.ecommerce.product.dto.StockAdjustmentRequest;

import java.util.UUID;

public interface InventoryService {

    InventoryResponse findByProductId(UUID productId);

    /** Adds units to physical stock (receiving from supplier). */
    InventoryResponse restock(UUID productId, StockAdjustmentRequest request);

    /** Reserves units for a pending order (reduces available quantity). */
    InventoryResponse reserve(UUID productId, StockAdjustmentRequest request);

    /** Releases previously reserved units (order cancelled or rejected). */
    InventoryResponse release(UUID productId, StockAdjustmentRequest request);
}
