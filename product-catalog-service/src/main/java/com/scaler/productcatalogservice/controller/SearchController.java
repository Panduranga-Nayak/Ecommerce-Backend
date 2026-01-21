package com.scaler.productcatalogservice.controller;

import com.scaler.productcatalogservice.dto.ProductSearchResponseDto;
import com.scaler.productcatalogservice.mapper.ProductSearchMapper;
import com.scaler.productcatalogservice.service.ProductService;
import com.scaler.productcatalogservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
public class SearchController {
    private final ProductService productService;

    public SearchController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/search")
    public Page<ProductSearchResponseDto> search(@RequestParam String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> results = productService.searchProducts(q, pageable);
        List<ProductSearchResponseDto> items = results.getContent().stream()
                .map(ProductSearchMapper::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, results.getTotalElements());
    }
}