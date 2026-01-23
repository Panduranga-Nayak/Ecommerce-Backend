package com.scaler.orderservice.dto;

import com.scaler.orderservice.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequestDto {
    @NotNull
    @Valid
    private AddressDto deliveryAddress;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    @Valid
    private List<OrderItemRequestDto> items;
}