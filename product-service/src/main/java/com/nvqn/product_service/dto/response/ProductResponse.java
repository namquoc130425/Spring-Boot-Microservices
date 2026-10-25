package com.nvqn.product_service.dto.response;

import com.nvqn.product_service.constans.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        ProductStatus status
) {
}
