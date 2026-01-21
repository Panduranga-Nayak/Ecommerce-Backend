package com.scaler.productcatalogservice.controller;

import com.scaler.productcatalogservice.dto.CategoryRequestDto;
import com.scaler.productcatalogservice.dto.CategoryResponseDto;
import com.scaler.productcatalogservice.mapper.CategoryMapper;
import com.scaler.productcatalogservice.service.AuthorizationGuard;
import com.scaler.productcatalogservice.service.ProductService;
import com.scaler.productcatalogservice.model.Category;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final ProductService productService;

    public CategoryController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public CategoryResponseDto createCategory(@Valid @RequestBody CategoryRequestDto request) {
        AuthorizationGuard.requireRole("ADMIN");
        Category category = productService.createCategory(request);
        return CategoryMapper.toResponse(category);
    }

    @GetMapping
    public List<CategoryResponseDto> listCategories() {
        return productService.listCategories().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{categoryId}")
    public CategoryResponseDto getCategory(@PathVariable Long categoryId) {
        return CategoryMapper.toResponse(productService.getCategory(categoryId));
    }
}