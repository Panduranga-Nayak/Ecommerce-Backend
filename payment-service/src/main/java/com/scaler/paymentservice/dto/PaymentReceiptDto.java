package com.scaler.paymentservice.dto;

import com.scaler.paymentservice.model.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentReceiptDto {
    private Long paymentId;
    private Long orderId;
    private String receiptNumber;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
}