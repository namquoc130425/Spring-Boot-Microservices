package com.nvqn.product_service.dto.request;

import com.nvqn.product_service.constans.ProductStatus;

import java.math.BigDecimal;

public record ProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        ProductStatus status
) {
}
