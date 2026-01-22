package com.scaler.cartservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CartResponseDto {
    private Long userId;
    private List<CartItemResponseDto> items;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private String currency;
}