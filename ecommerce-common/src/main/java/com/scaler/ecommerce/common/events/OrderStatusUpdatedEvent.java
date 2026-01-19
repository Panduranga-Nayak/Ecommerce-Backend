package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdatedEvent {
    private Long orderId;
    private Long userId;
    private String email;
    private String status;
    private String description;
}
