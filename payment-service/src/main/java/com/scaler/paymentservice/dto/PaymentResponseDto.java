package com.scaler.paymentservice.dto;

import com.scaler.paymentservice.model.enums.PaymentMethod;
import com.scaler.paymentservice.model.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private PaymentStatus status;
    private PaymentMethod method;
    private BigDecimal amount;
    private String currency;
    private String receiptNumber;
    private String paymentLink;
}