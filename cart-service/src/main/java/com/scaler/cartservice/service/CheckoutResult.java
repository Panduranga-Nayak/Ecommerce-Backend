package com.scaler.cartservice.service;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CheckoutResult {
    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
}