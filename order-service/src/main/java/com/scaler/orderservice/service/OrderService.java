package com.scaler.orderservice.service;

import com.scaler.orderservice.dto.CreateOrderRequestDto;
import com.scaler.orderservice.dto.OrderStatusUpdateRequestDto;
import com.scaler.orderservice.model.Order;
import com.scaler.orderservice.model.OrderStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    Order createOrder(CreateOrderRequestDto request, Long userId, String idempotencyKey);

    Page<Order> listOrders(Long userId, Pageable pageable);

    Order getOrder(Long orderId, Long userId);

    List<OrderStatusHistory> getTracking(Long orderId, Long userId);

    Order updateStatus(Long orderId, OrderStatusUpdateRequestDto request);

    void handlePaymentCompleted(Long orderId, Long userId, String receiptNumber);

    void handlePaymentFailed(Long orderId, Long userId, String reason);
}