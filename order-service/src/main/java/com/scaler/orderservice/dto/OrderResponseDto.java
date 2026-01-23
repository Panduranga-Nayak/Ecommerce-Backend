package com.scaler.orderservice.dto;

import com.scaler.orderservice.model.enums.OrderStatus;
import com.scaler.orderservice.model.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderResponseDto {
    private Long orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String trackingNumber;
    private AddressDto deliveryAddress;
    private List<OrderItemResponseDto> items;
}