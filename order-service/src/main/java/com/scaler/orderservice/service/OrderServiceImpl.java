package com.scaler.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.OrderCreatedEvent;
import com.scaler.ecommerce.common.events.OrderItemEvent;
import com.scaler.ecommerce.common.events.OrderStatusUpdatedEvent;
import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.ecommerce.common.utils.HashUtils;
import com.scaler.orderservice.dto.CreateOrderRequestDto;
import com.scaler.orderservice.dto.OrderItemRequestDto;
import com.scaler.orderservice.dto.OrderStatusUpdateRequestDto;
import com.scaler.orderservice.exception.AccessDeniedException;
import com.scaler.orderservice.exception.IdempotencyConflictException;
import com.scaler.orderservice.exception.InvalidOrderException;
import com.scaler.orderservice.exception.OrderNotFoundException;
import com.scaler.orderservice.exception.ProductUnavailableException;
import com.scaler.orderservice.model.enums.OrderStatus;
import com.scaler.orderservice.model.DeliveryAddress;
import com.scaler.orderservice.model.IdempotencyKey;
import com.scaler.orderservice.model.Order;
import com.scaler.orderservice.model.OrderItem;
import com.scaler.orderservice.model.OrderStatusHistory;
import com.scaler.orderservice.client.ProductCatalogClient;
import com.scaler.orderservice.client.ProductSnapshot;
import com.scaler.orderservice.kafka.OrderEventPublisher;
import com.scaler.orderservice.repo.IdempotencyKeyRepository;
import com.scaler.orderservice.repo.OrderRepository;
import com.scaler.orderservice.repo.OrderStatusHistoryRepository;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ProductCatalogClient productCatalogClient;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderStatusHistoryRepository orderStatusHistoryRepository,
                            IdempotencyKeyRepository idempotencyKeyRepository,
                            ProductCatalogClient productCatalogClient,
                            OrderEventPublisher orderEventPublisher,
                            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.productCatalogClient = productCatalogClient;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequestDto request, Long userId, String idempotencyKey) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain items");
        }

        String requestHash = hashRequest(request, userId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey existing = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);
            if (existing != null) {
                if (!existing.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key already used with different payload");
                }
                if (existing.getOrderId() != null) {
                    return orderRepository.findById(existing.getOrderId())
                            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
                }
            } else {
                IdempotencyKey record = new IdempotencyKey();
                record.setKey(idempotencyKey);
                record.setUserId(userId);
                record.setRequestHash(requestHash);
                record.setCreatedAt(Instant.now());
                idempotencyKeyRepository.save(record);
            }
        }

        Order order = new Order();
        order.setUserId(userId);
        AuthenticatedUser currentUser = RequestContext.getCurrentUser();
        if (currentUser != null) {
            order.setCustomerEmail(currentUser.getEmail());
        }
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryAddress(toDeliveryAddress(request));

        List<OrderItem> items = new java.util.ArrayList<>();
        String currency = null;
        for (OrderItemRequestDto itemRequest : request.getItems()) {
            ProductSnapshot product = productCatalogClient.getProduct(itemRequest.getProductId());
            if (product == null || product.getId() == null) {
                throw new ProductUnavailableException("Product not found");
            }
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new ProductUnavailableException("Product inactive");
            }
            if (product.getStockQuantity() != null && product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new ProductUnavailableException("Insufficient stock");
            }

            if (currency == null) {
                currency = product.getCurrency();
            } else if (product.getCurrency() != null && !currency.equalsIgnoreCase(product.getCurrency())) {
                throw new InvalidOrderException("Mixed currency orders are not supported");
            }

            OrderItem item = buildOrderItem(order, itemRequest, product);
            items.add(item);
        }
        order.getItems().addAll(items);

        BigDecimal total = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        order.setCurrency(currency != null ? currency : "USD");

        Order saved = orderRepository.save(order);
        if (saved.getTrackingNumber() == null) {
            saved.setTrackingNumber("TRK-" + saved.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));
            saved = orderRepository.save(saved);
        }

        saveStatusHistory(saved, saved.getStatus(), "Order created");

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey record = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);
            if (record != null) {
                record.setOrderId(saved.getId());
                idempotencyKeyRepository.save(record);
            }
        }

        publishOrderCreated(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> listOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!AuthorizationGuard.isAdmin() && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Order does not belong to user");
        }

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getTracking(Long orderId, Long userId) {
        Order order = getOrder(orderId, userId);
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
    }

    @Override
    @Transactional
    public Order updateStatus(Long orderId, OrderStatusUpdateRequestDto request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.setStatus(request.getStatus());
        Order saved = orderRepository.save(order);
        saveStatusHistory(saved, request.getStatus(), request.getDescription());

        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent();
        event.setOrderId(saved.getId());
        event.setUserId(saved.getUserId());
        event.setEmail(saved.getCustomerEmail());
        event.setStatus(saved.getStatus().name());
        event.setDescription(request.getDescription());
        orderEventPublisher.publishStatusUpdated(getCorrelationId(), event);

        return saved;
    }

    @Override
    @Transactional
    public void handlePaymentCompleted(Long orderId, Long userId, String receiptNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Order does not belong to user");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        saveStatusHistory(saved, OrderStatus.CONFIRMED, "Payment confirmed: " + receiptNumber);

        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent();
        event.setOrderId(saved.getId());
        event.setUserId(saved.getUserId());
        event.setEmail(saved.getCustomerEmail());
        event.setStatus(saved.getStatus().name());
        event.setDescription("Order confirmed");
        orderEventPublisher.publishStatusUpdated(getCorrelationId(), event);
    }

    @Override
    @Transactional
    public void handlePaymentFailed(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Order does not belong to user");
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        Order saved = orderRepository.save(order);
        saveStatusHistory(saved, OrderStatus.PAYMENT_FAILED, reason);

        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent();
        event.setOrderId(saved.getId());
        event.setUserId(saved.getUserId());
        event.setEmail(saved.getCustomerEmail());
        event.setStatus(saved.getStatus().name());
        event.setDescription("Payment failed");
        orderEventPublisher.publishStatusUpdated(getCorrelationId(), event);
    }

    private DeliveryAddress toDeliveryAddress(CreateOrderRequestDto request) {
        DeliveryAddress address = new DeliveryAddress();
        address.setLine1(request.getDeliveryAddress().getLine1());
        address.setLine2(request.getDeliveryAddress().getLine2());
        address.setCity(request.getDeliveryAddress().getCity());
        address.setState(request.getDeliveryAddress().getState());
        address.setPostalCode(request.getDeliveryAddress().getPostalCode());
        address.setCountry(request.getDeliveryAddress().getCountry());
        return address;
    }

    private OrderItem buildOrderItem(Order order, OrderItemRequestDto itemRequest, ProductSnapshot product) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(itemRequest.getQuantity());
        item.setUnitPrice(product.getPrice());
        item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        return item;
    }

    private void saveStatusHistory(Order order, OrderStatus status, String description) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setDescription(description);
        history.setCreatedAt(Instant.now());
        orderStatusHistoryRepository.save(history);
    }

    private void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setEmail(order.getCustomerEmail());
        event.setCurrency(order.getCurrency());
        event.setTotalAmount(order.getTotalAmount());
        event.setPaymentMethod(order.getPaymentMethod().name());
        event.setItems(order.getItems().stream().map(item -> {
            OrderItemEvent orderItemEvent = new OrderItemEvent();
            orderItemEvent.setProductId(item.getProductId());
            orderItemEvent.setProductName(item.getProductName());
            orderItemEvent.setQuantity(item.getQuantity());
            orderItemEvent.setUnitPrice(item.getUnitPrice());
            orderItemEvent.setLineTotal(item.getLineTotal());
            return orderItemEvent;
        }).collect(Collectors.toList()));

        orderEventPublisher.publishOrderCreated(getCorrelationId(), event);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private String hashRequest(CreateOrderRequestDto request, Long userId) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(request) + ":" + userId);
        } catch (JsonProcessingException e) {
            throw new InvalidOrderException("Unable to process request");
        }
    }
}