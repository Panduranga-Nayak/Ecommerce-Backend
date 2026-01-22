package com.scaler.cartservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CheckoutResponseDto {
    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
}