package com.scaler.productcatalogservice;

import com.scaler.productcatalogservice.controller.ProductController;
import com.scaler.productcatalogservice.dto.ProductResponseDto;
import com.scaler.productcatalogservice.service.ProductService;
import com.scaler.productcatalogservice.model.enums.ProductStatus;
import com.scaler.productcatalogservice.model.Category;
import com.scaler.productcatalogservice.model.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock
    private ProductService productService;

    @AfterEach
    void clearContext() {
        com.scaler.ecommerce.common.security.RequestContext.clear();
    }

    @Test
    void getProductReturnsMappedResponse() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Electronics");
        category.setDescription("Gadgets");

        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-1");
        product.setName("Phone");
        product.setDescription("Smartphone");
        product.setPrice(new BigDecimal("499.00"));
        product.setCurrency("USD");
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        when(productService.getProduct(1L)).thenReturn(product);

        ProductController controller = new ProductController(productService);
        ProductResponseDto response = controller.getProduct(1L);

        assertEquals(1L, response.getId());
        assertEquals("SKU-1", response.getSku());
        assertEquals("Electronics", response.getCategory().getName());
        assertEquals(ProductStatus.ACTIVE, response.getStatus());
    }
}