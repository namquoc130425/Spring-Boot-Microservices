package com.nvqn.product_service.service;

import com.nvqn.product_service.dto.request.ProductRequest;
import com.nvqn.product_service.dto.response.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductRequest request);

    List<ProductResponse> findAll();

    ProductResponse findById(UUID id);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);
}
