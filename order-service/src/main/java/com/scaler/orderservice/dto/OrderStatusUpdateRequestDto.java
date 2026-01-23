package com.scaler.orderservice.dto;

import com.scaler.orderservice.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequestDto {
    @NotNull
    private OrderStatus status;

    private String description;
}