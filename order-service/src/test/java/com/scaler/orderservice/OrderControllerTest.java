package com.scaler.orderservice;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.orderservice.controller.OrderController;
import com.scaler.orderservice.dto.AddressDto;
import com.scaler.orderservice.dto.CreateOrderRequestDto;
import com.scaler.orderservice.dto.OrderItemRequestDto;
import com.scaler.orderservice.dto.OrderResponseDto;
import com.scaler.orderservice.service.OrderService;
import com.scaler.orderservice.model.enums.OrderStatus;
import com.scaler.orderservice.model.enums.PaymentMethod;
import com.scaler.orderservice.model.DeliveryAddress;
import com.scaler.orderservice.model.Order;
import com.scaler.orderservice.model.OrderItem;
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
class OrderControllerTest {
    @Mock
    private OrderService orderService;

    private OrderController controller;

    @BeforeEach
    void setup() {
        controller = new OrderController(orderService);
    }

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void createOrderReturnsResponse() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserId(1L);
        user.setEmail("buyer@example.com");
        user.setRoles(java.util.Set.of("CUSTOMER"));
        RequestContext.setCurrentUser(user);

        AddressDto addressDto = new AddressDto();
        addressDto.setLine1("123 Main St");
        addressDto.setCity("Seattle");
        addressDto.setState("WA");
        addressDto.setPostalCode("98101");
        addressDto.setCountry("USA");

        OrderItemRequestDto item = new OrderItemRequestDto();
        item.setProductId(100L);
        item.setQuantity(1);

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setDeliveryAddress(addressDto);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setItems(List.of(item));

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(100L);
        orderItem.setProductName("Phone");
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(new BigDecimal("499.00"));
        orderItem.setLineTotal(new BigDecimal("499.00"));

        DeliveryAddress address = new DeliveryAddress();
        address.setLine1("123 Main St");
        address.setCity("Seattle");
        address.setState("WA");
        address.setPostalCode("98101");
        address.setCountry("USA");

        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("499.00"));
        order.setCurrency("USD");
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setTrackingNumber("TRK-10");
        order.setDeliveryAddress(address);
        order.setItems(List.of(orderItem));

        when(orderService.createOrder(request, 1L, "order-key")).thenReturn(order);

        OrderResponseDto response = controller.createOrder(request, "order-key");

        assertEquals(10L, response.getOrderId());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals("USD", response.getCurrency());
        verify(orderService).createOrder(request, 1L, "order-key");
    }
}