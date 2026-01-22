package com.scaler.cartservice.controller;

import com.scaler.cartservice.dto.CartItemRequestDto;
import com.scaler.cartservice.dto.CartResponseDto;
import com.scaler.cartservice.dto.CheckoutRequestDto;
import com.scaler.cartservice.dto.CheckoutResponseDto;
import com.scaler.cartservice.dto.UpdateCartItemRequestDto;
import com.scaler.cartservice.mapper.CartMapper;
import com.scaler.cartservice.service.CartService;
import com.scaler.cartservice.service.CheckoutResult;
import com.scaler.cartservice.model.Cart;
import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/me")
    public CartResponseDto getCart() {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Cart cart = cartService.getCart(user.getUserId());
        return CartMapper.toResponse(cart);
    }

    @PostMapping("/items")
    public CartResponseDto addItem(@Valid @RequestBody CartItemRequestDto request) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Cart cart = cartService.addItem(user.getUserId(), request);
        return CartMapper.toResponse(cart);
    }

    @PutMapping("/items/{productId}")
    public CartResponseDto updateItem(@PathVariable Long productId,
                                      @Valid @RequestBody UpdateCartItemRequestDto request) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Cart cart = cartService.updateItem(user.getUserId(), productId, request.getQuantity());
        return CartMapper.toResponse(cart);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponseDto removeItem(@PathVariable Long productId) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Cart cart = cartService.removeItem(user.getUserId(), productId);
        return CartMapper.toResponse(cart);
    }

    @DeleteMapping
    public void clearCart() {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        cartService.clearCart(user.getUserId());
    }

    @PostMapping("/checkout")
    public CheckoutResponseDto checkout(@Valid @RequestBody CheckoutRequestDto request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : authorization;

        CheckoutResult result = cartService.checkout(user.getUserId(), request, idempotencyKey, token);
        CheckoutResponseDto response = new CheckoutResponseDto();
        response.setOrderId(result.getOrderId());
        response.setStatus(result.getStatus());
        response.setTotalAmount(result.getTotalAmount());
        response.setCurrency(result.getCurrency());
        return response;
    }
}