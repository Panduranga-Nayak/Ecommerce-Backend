package com.scaler.cartservice;

import com.scaler.cartservice.controller.CartController;
import com.scaler.cartservice.dto.CartItemRequestDto;
import com.scaler.cartservice.dto.CartResponseDto;
import com.scaler.cartservice.service.CartService;
import com.scaler.cartservice.model.Cart;
import com.scaler.cartservice.model.CartItem;
import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {
    @Mock
    private CartService cartService;

    private CartController controller;

    @BeforeEach
    void setup() {
        controller = new CartController(cartService);
    }

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void addItemReturnsCart() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserId(1L);
        user.setEmail("buyer@example.com");
        user.setRoles(java.util.Set.of("CUSTOMER"));
        RequestContext.setCurrentUser(user);

        CartItemRequestDto request = new CartItemRequestDto();
        request.setProductId(100L);
        request.setQuantity(2);

        CartItem item = new CartItem();
        item.setProductId(100L);
        item.setProductName("Phone");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("499.00"));
        item.setLineTotal(new BigDecimal("998.00"));
        item.setCurrency("USD");

        Cart cart = new Cart();
        cart.setUserId(1L);
        cart.setItems(List.of(item));
        cart.setTotalItems(2);
        cart.setTotalAmount(new BigDecimal("998.00"));
        cart.setCurrency("USD");

        when(cartService.addItem(1L, request)).thenReturn(cart);

        CartResponseDto response = controller.addItem(request);

        assertEquals(1L, response.getUserId());
        assertEquals(2, response.getTotalItems());
        assertEquals(100L, response.getItems().get(0).getProductId());
        verify(cartService).addItem(1L, request);
    }
}