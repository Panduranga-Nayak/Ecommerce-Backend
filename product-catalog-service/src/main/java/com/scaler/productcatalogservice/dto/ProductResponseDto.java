package com.scaler.productcatalogservice.dto;

import com.scaler.productcatalogservice.model.enums.ProductStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductResponseDto {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer stockQuantity;
    private ProductStatus status;
    private CategoryResponseDto category;
    private List<ProductImageDto> images;
    private List<ProductSpecificationDto> specifications;
}