package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentReceiptEvent {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String receiptNumber;
    private String email;
}
