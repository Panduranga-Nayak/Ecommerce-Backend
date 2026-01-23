package com.scaler.orderservice.dto;

import com.scaler.orderservice.model.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class OrderStatusHistoryDto {
    private OrderStatus status;
    private String description;
    private Instant createdAt;
}