package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CartUpdatedEvent {
    private Long userId;
    private int totalItems;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
}
