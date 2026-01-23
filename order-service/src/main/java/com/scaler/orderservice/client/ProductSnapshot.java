package com.scaler.orderservice.client;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductSnapshot {
    private Long id;
    private String name;
    private BigDecimal price;
    private String currency;
    private String status;
    private Integer stockQuantity;
}