package com.scaler.orderservice.mapper;

import com.scaler.orderservice.dto.AddressDto;
import com.scaler.orderservice.dto.OrderItemResponseDto;
import com.scaler.orderservice.dto.OrderResponseDto;
import com.scaler.orderservice.dto.OrderStatusHistoryDto;
import com.scaler.orderservice.model.DeliveryAddress;
import com.scaler.orderservice.model.Order;
import com.scaler.orderservice.model.OrderItem;
import com.scaler.orderservice.model.OrderStatusHistory;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {
    public static OrderResponseDto toResponse(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setOrderId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setDeliveryAddress(toAddressDto(order.getDeliveryAddress()));
        dto.setItems(order.getItems().stream().map(OrderMapper::toItemDto).collect(Collectors.toList()));
        return dto;
    }

    public static OrderItemResponseDto toItemDto(OrderItem item) {
        OrderItemResponseDto dto = new OrderItemResponseDto();
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    public static AddressDto toAddressDto(DeliveryAddress address) {
        if (address == null) {
            return null;
        }

        AddressDto dto = new AddressDto();
        dto.setLine1(address.getLine1());
        dto.setLine2(address.getLine2());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        return dto;
    }

    public static DeliveryAddress toDeliveryAddress(AddressDto dto) {
        DeliveryAddress address = new DeliveryAddress();
        address.setLine1(dto.getLine1());
        address.setLine2(dto.getLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        return address;
    }

    public static List<OrderStatusHistoryDto> toHistoryDtos(List<OrderStatusHistory> history) {
        return history.stream().map(entry -> {
            OrderStatusHistoryDto dto = new OrderStatusHistoryDto();
            dto.setStatus(entry.getStatus());
            dto.setDescription(entry.getDescription());
            dto.setCreatedAt(entry.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}