package com.scaler.cartservice.service;

import com.scaler.cartservice.dto.CartItemRequestDto;
import com.scaler.cartservice.dto.CheckoutRequestDto;
import com.scaler.cartservice.model.Cart;

public interface CartService {
    Cart getCart(Long userId);

    Cart addItem(Long userId, CartItemRequestDto request);

    Cart updateItem(Long userId, Long productId, Integer quantity);

    Cart removeItem(Long userId, Long productId);

    void clearCart(Long userId);

    CheckoutResult checkout(Long userId, CheckoutRequestDto request, String idempotencyKey, String accessToken);
}