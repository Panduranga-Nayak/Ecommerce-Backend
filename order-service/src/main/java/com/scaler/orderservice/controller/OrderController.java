package com.scaler.orderservice.controller;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.orderservice.dto.CreateOrderRequestDto;
import com.scaler.orderservice.dto.OrderResponseDto;
import com.scaler.orderservice.dto.OrderStatusHistoryDto;
import com.scaler.orderservice.dto.OrderStatusUpdateRequestDto;
import com.scaler.orderservice.mapper.OrderMapper;
import com.scaler.orderservice.service.AuthorizationGuard;
import com.scaler.orderservice.service.OrderService;
import com.scaler.orderservice.model.Order;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponseDto createOrder(@Valid @RequestBody CreateOrderRequestDto request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Order order = orderService.createOrder(request, user.getUserId(), idempotencyKey);
        return OrderMapper.toResponse(order);
    }

    @GetMapping
    public Page<OrderResponseDto> listOrders(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Pageable pageable = buildPageable(page, size, sort);
        Page<Order> orders = orderService.listOrders(user.getUserId(), pageable);
        List<OrderResponseDto> items = orders.getContent().stream()
                .map(OrderMapper::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, orders.getTotalElements());
    }

    @GetMapping("/{orderId}")
    public OrderResponseDto getOrder(@PathVariable Long orderId) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Order order = orderService.getOrder(orderId, user.getUserId());
        return OrderMapper.toResponse(order);
    }

    @GetMapping("/{orderId}/tracking")
    public List<OrderStatusHistoryDto> trackOrder(@PathVariable Long orderId) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        return OrderMapper.toHistoryDtos(orderService.getTracking(orderId, user.getUserId()));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponseDto updateStatus(@PathVariable Long orderId,
                                         @Valid @RequestBody OrderStatusUpdateRequestDto request) {
        AuthorizationGuard.requireAdmin();
        Order order = orderService.updateStatus(orderId, request);
        return OrderMapper.toResponse(order);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        Set<String> allowedFields = Set.of("createdAt", "status");
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0] : "createdAt";
        String direction = parts.length > 1 ? parts[1] : "desc";

        if (!allowedFields.contains(field)) {
            field = "createdAt";
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, field));
    }
}