package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private String email;
    private String currency;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private List<OrderItemEvent> items;
}
