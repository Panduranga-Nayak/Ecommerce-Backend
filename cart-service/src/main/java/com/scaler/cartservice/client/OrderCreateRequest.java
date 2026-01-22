package com.scaler.cartservice.client;

import com.scaler.cartservice.dto.AddressDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateRequest {
    private AddressDto deliveryAddress;
    private String paymentMethod;
    private List<OrderItemRequest> items;
}