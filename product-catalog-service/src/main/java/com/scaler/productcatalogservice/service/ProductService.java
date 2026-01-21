package com.scaler.productcatalogservice.service;

import com.scaler.productcatalogservice.dto.CategoryRequestDto;
import com.scaler.productcatalogservice.dto.CreateProductRequestDto;
import com.scaler.productcatalogservice.dto.UpdateProductRequestDto;
import com.scaler.productcatalogservice.model.Category;
import com.scaler.productcatalogservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Product createProduct(CreateProductRequestDto request);

    Product updateProduct(Long productId, UpdateProductRequestDto request);

    void deleteProduct(Long productId);

    Product getProduct(Long productId);

    Page<Product> listProducts(ProductFilter filter, Pageable pageable);

    Page<Product> searchProducts(String query, Pageable pageable);

    Category createCategory(CategoryRequestDto request);

    List<Category> listCategories();

    Category getCategory(Long categoryId);
}