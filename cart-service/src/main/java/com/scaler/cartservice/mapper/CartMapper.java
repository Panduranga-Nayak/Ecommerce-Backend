package com.scaler.cartservice.mapper;

import com.scaler.cartservice.dto.CartItemResponseDto;
import com.scaler.cartservice.dto.CartResponseDto;
import com.scaler.cartservice.model.Cart;
import com.scaler.cartservice.model.CartItem;

import java.util.List;
import java.util.stream.Collectors;

public class CartMapper {
    public static CartResponseDto toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        CartResponseDto dto = new CartResponseDto();
        dto.setUserId(cart.getUserId());
        dto.setItems(cart.getItems().stream().map(CartMapper::toItemResponse).collect(Collectors.toList()));
        dto.setTotalItems(cart.getTotalItems());
        dto.setTotalAmount(cart.getTotalAmount());
        dto.setCurrency(cart.getCurrency());
        return dto;
    }

    public static CartItemResponseDto toItemResponse(CartItem item) {
        CartItemResponseDto dto = new CartItemResponseDto();
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineTotal(item.getLineTotal());
        dto.setCurrency(item.getCurrency());
        return dto;
    }

    public static List<CartItemResponseDto> toItemResponses(List<CartItem> items) {
        return items.stream().map(CartMapper::toItemResponse).collect(Collectors.toList());
    }
}