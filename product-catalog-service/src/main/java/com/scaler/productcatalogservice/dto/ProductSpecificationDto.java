package com.scaler.productcatalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSpecificationDto {
    @NotBlank
    private String key;

    @NotBlank
    private String value;
}