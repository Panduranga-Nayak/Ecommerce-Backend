package com.scaler.productcatalogservice.controller;

import com.scaler.productcatalogservice.dto.CreateProductRequestDto;
import com.scaler.productcatalogservice.dto.ProductResponseDto;
import com.scaler.productcatalogservice.dto.UpdateProductRequestDto;
import com.scaler.productcatalogservice.mapper.ProductMapper;
import com.scaler.productcatalogservice.service.AuthorizationGuard;
import com.scaler.productcatalogservice.service.ProductFilter;
import com.scaler.productcatalogservice.service.ProductService;
import com.scaler.productcatalogservice.model.enums.ProductStatus;
import com.scaler.productcatalogservice.model.Product;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponseDto createProduct(@Valid @RequestBody CreateProductRequestDto request) {
        AuthorizationGuard.requireRole("ADMIN");
        Product product = productService.createProduct(request);
        return ProductMapper.toResponse(product);
    }

    @PutMapping("/{productId}")
    public ProductResponseDto updateProduct(@PathVariable Long productId,
                                            @Valid @RequestBody UpdateProductRequestDto request) {
        AuthorizationGuard.requireRole("ADMIN");
        Product product = productService.updateProduct(productId, request);
        return ProductMapper.toResponse(product);
    }

    @DeleteMapping("/{productId}")
    public void deleteProduct(@PathVariable Long productId) {
        AuthorizationGuard.requireRole("ADMIN");
        productService.deleteProduct(productId);
    }

    @GetMapping("/{productId}")
    public ProductResponseDto getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ProductMapper.toResponse(product);
    }

    @GetMapping
    public Page<ProductResponseDto> listProducts(@RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) BigDecimal minPrice,
                                                 @RequestParam(required = false) BigDecimal maxPrice,
                                                 @RequestParam(required = false) ProductStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        ProductFilter filter = new ProductFilter();
        filter.setCategoryId(categoryId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setStatus(status);

        Page<Product> products = productService.listProducts(filter, pageable);
        List<ProductResponseDto> items = products.getContent().stream()
                .map(ProductMapper::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, products.getTotalElements());
    }

    private Pageable buildPageable(int page, int size, String sort) {
        Set<String> allowedFields = Set.of("createdAt", "price", "name");
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0] : "createdAt";
        String direction = parts.length > 1 ? parts[1] : "desc";

        if (!allowedFields.contains(field)) {
            field = "createdAt";
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, field));
    }
}