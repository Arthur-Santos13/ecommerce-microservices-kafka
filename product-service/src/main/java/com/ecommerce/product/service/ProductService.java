package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    List<ProductResponse> findAll();

    ProductResponse findById(UUID id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);
}
