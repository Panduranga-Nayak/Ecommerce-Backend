package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentFailedEvent {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String method;
    private String currency;
    private BigDecimal amount;
    private String failureReason;
}
