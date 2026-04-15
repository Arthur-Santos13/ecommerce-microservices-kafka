package com.ecommerce.product.controller;

import com.ecommerce.product.dto.InventoryResponse;
import com.ecommerce.product.dto.StockAdjustmentRequest;
import com.ecommerce.product.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.findByProductId(productId));
    }

    @PostMapping("/restock")
    public ResponseEntity<InventoryResponse> restock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.restock(productId, request));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserve(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.reserve(productId, request));
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> release(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.release(productId, request));
    }
}
