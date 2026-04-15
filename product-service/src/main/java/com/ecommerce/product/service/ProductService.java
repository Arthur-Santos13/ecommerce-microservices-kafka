package com.ecommerce.product.service;

import com.ecommerce.product.dto.PageResponse;
import com.ecommerce.product.dto.ProductFilter;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    PageResponse<ProductResponse> findAll(ProductFilter filter, Pageable pageable);

    ProductResponse findById(UUID id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);
}
