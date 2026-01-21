package com.scaler.productcatalogservice.dto;

import com.scaler.productcatalogservice.model.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateProductRequestDto {
    @NotBlank
    @Size(max = 80)
    private String sku;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 4000)
    private String description;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @NotBlank
    @Size(max = 10)
    private String currency;

    @NotNull
    private Integer stockQuantity;

    @NotNull
    private Long categoryId;

    @NotNull
    private ProductStatus status;

    private List<ProductImageDto> images;

    private List<ProductSpecificationDto> specifications;
}