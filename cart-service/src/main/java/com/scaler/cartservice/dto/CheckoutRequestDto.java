package com.scaler.cartservice.dto;

import com.scaler.cartservice.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequestDto {
    @NotNull
    @Valid
    private AddressDto deliveryAddress;

    @NotNull
    private PaymentMethod paymentMethod;
}