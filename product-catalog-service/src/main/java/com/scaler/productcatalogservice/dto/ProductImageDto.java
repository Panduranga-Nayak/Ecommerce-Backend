package com.scaler.productcatalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageDto {
    @NotBlank
    private String url;

    @NotNull
    private Integer sortOrder;
}