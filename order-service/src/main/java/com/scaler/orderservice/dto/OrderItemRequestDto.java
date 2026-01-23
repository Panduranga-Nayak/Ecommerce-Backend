package com.scaler.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequestDto {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;
}