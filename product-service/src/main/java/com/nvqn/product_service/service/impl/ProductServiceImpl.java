package com.nvqn.product_service.service.impl;

import com.nvqn.product_service.dto.request.ProductRequest;
import com.nvqn.product_service.dto.response.ProductResponse;
import com.nvqn.product_service.entity.Product;
import com.nvqn.product_service.mapper.ProductMapper;
import com.nvqn.product_service.repository.ProductRepository;
import com.nvqn.product_service.service.ProductService;

import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.Objects;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductServiceImpl implements ProductService {
    ProductRepository productRepository;
    ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        validate(request);
        Product product = productMapper.toEntity(request);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return productMapper.toResponse(getProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        validate(request);
        Product product = getProduct(id);
        productMapper.updateEntity(request, product);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    private Product getProduct(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: " + id
                ));
    }

    private static void validate(ProductRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be greater than or equal to 0");
        }
        if (request.quantity() == null || request.quantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than or equal to 0");
        }
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must not be null");
        }
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
