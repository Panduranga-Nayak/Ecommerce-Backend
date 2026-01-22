package com.scaler.cartservice.client;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderCreateResponse {
    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
}