package com.scaler.productcatalogservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProductSearchResponseDto {
    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private BigDecimal price;
    private String currency;
    private List<String> imageUrls;
    private Map<String, String> specifications;
}