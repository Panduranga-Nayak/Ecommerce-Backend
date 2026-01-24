package com.scaler.paymentservice.dto;

import com.scaler.paymentservice.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequestDto {
    @NotNull
    private Long orderId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;

    @NotNull
    private String currency;

    @NotNull
    private PaymentMethod method;

    @Size(max = 100)
    private String paymentInstrumentToken;
}