package com.scaler.cartservice.service;

import com.scaler.cartservice.dto.CartItemRequestDto;
import com.scaler.cartservice.dto.CheckoutRequestDto;
import com.scaler.cartservice.exception.CartNotFoundException;
import com.scaler.cartservice.exception.InvalidCartItemException;
import com.scaler.cartservice.exception.OrderCreationException;
import com.scaler.cartservice.exception.ProductUnavailableException;
import com.scaler.cartservice.model.Cart;
import com.scaler.cartservice.model.CartItem;
import com.scaler.cartservice.cache.CartCacheRepository;
import com.scaler.cartservice.client.OrderClient;
import com.scaler.cartservice.client.OrderCreateRequest;
import com.scaler.cartservice.client.OrderCreateResponse;
import com.scaler.cartservice.client.OrderItemRequest;
import com.scaler.cartservice.client.ProductCatalogClient;
import com.scaler.cartservice.client.ProductSnapshot;
import com.scaler.cartservice.kafka.CartEventPublisher;
import com.scaler.cartservice.repo.CartRepository;
import com.scaler.ecommerce.common.events.CartUpdatedEvent;
import com.scaler.ecommerce.common.events.OrderItemEvent;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartCacheRepository cartCacheRepository;
    private final ProductCatalogClient productCatalogClient;
    private final OrderClient orderClient;
    private final CartEventPublisher cartEventPublisher;

    public CartServiceImpl(CartRepository cartRepository,
                           CartCacheRepository cartCacheRepository,
                           ProductCatalogClient productCatalogClient,
                           OrderClient orderClient,
                           CartEventPublisher cartEventPublisher) {
        this.cartRepository = cartRepository;
        this.cartCacheRepository = cartCacheRepository;
        this.productCatalogClient = productCatalogClient;
        this.orderClient = orderClient;
        this.cartEventPublisher = cartEventPublisher;
    }

    @Override
    public Cart getCart(Long userId) {
        Cart cached = cartCacheRepository.getCart(userId);
        if (cached != null) {
            return cached;
        }

        Cart cart = cartRepository.findById(userId).orElseGet(() -> {
            Cart emptyCart = new Cart();
            emptyCart.setUserId(userId);
            emptyCart.setTotalItems(0);
            emptyCart.setTotalAmount(BigDecimal.ZERO);
            emptyCart.setCurrency(null);
            emptyCart.setUpdatedAt(Instant.now());
            return emptyCart;
        });

        cartCacheRepository.putCart(userId, cart);
        return cart;
    }

    @Override
    public Cart addItem(Long userId, CartItemRequestDto request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidCartItemException("Quantity must be positive");
        }

        ProductSnapshot product = productCatalogClient.getProduct(request.getProductId());
        if (product == null || product.getId() == null) {
            throw new ProductUnavailableException("Product not found");
        }
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new ProductUnavailableException("Product is not active");
        }
        if (product.getStockQuantity() != null && product.getStockQuantity() < request.getQuantity()) {
            throw new ProductUnavailableException("Insufficient stock");
        }

        Cart cart = cartRepository.findById(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return newCart;
        });

        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(0);
            item.setUnitPrice(product.getPrice());
            item.setCurrency(product.getCurrency());
            cart.getItems().add(item);
        }

        int newQuantity = item.getQuantity() + request.getQuantity();
        item.setQuantity(newQuantity);
        item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));

        if (cart.getCurrency() == null) {
            cart.setCurrency(product.getCurrency());
        }
        if (!cart.getCurrency().equalsIgnoreCase(product.getCurrency())) {
            throw new InvalidCartItemException("Currency mismatch in cart");
        }

        recalculate(cart);
        Cart saved = cartRepository.save(cart);
        cartCacheRepository.putCart(userId, saved);
        publishCartUpdated(saved);
        return saved;
    }

    @Override
    public Cart updateItem(Long userId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new InvalidCartItemException("Item not found"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
            item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        recalculate(cart);
        Cart saved = cartRepository.save(cart);
        cartCacheRepository.putCart(userId, saved);
        publishCartUpdated(saved);
        return saved;
    }

    @Override
    public Cart removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        recalculate(cart);
        Cart saved = cartRepository.save(cart);
        cartCacheRepository.putCart(userId, saved);
        publishCartUpdated(saved);
        return saved;
    }

    @Override
    public void clearCart(Long userId) {
        cartRepository.deleteById(userId);
        cartCacheRepository.evict(userId);
    }

    @Override
    public CheckoutResult checkout(Long userId, CheckoutRequestDto request, String idempotencyKey, String accessToken) {
        Cart cart = cartRepository.findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new InvalidCartItemException("Cart is empty");
        }

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setDeliveryAddress(request.getDeliveryAddress());
        orderRequest.setPaymentMethod(request.getPaymentMethod().name());
        orderRequest.setItems(cart.getItems().stream().map(item -> {
            OrderItemRequest orderItem = new OrderItemRequest();
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            return orderItem;
        }).collect(Collectors.toList()));

        OrderCreateResponse response = orderClient.createOrder(orderRequest, idempotencyKey, accessToken);
        if (response == null || response.getOrderId() == null) {
            throw new OrderCreationException("Order creation failed");
        }

        clearCart(userId);

        CheckoutResult result = new CheckoutResult();
        result.setOrderId(response.getOrderId());
        result.setStatus(response.getStatus());
        result.setTotalAmount(response.getTotalAmount());
        result.setCurrency(response.getCurrency());
        return result;
    }

    private void recalculate(Cart cart) {
        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            totalItems += item.getQuantity();
            totalAmount = totalAmount.add(item.getLineTotal());
        }
        cart.setTotalItems(totalItems);
        cart.setTotalAmount(totalAmount);
        if (totalItems == 0) {
            cart.setCurrency(null);
        }
        cart.setUpdatedAt(Instant.now());
    }

    private void publishCartUpdated(Cart cart) {
        CartUpdatedEvent event = new CartUpdatedEvent();
        event.setUserId(cart.getUserId());
        event.setTotalItems(cart.getTotalItems());
        event.setTotalAmount(cart.getTotalAmount());
        event.setItems(cart.getItems().stream().map(item -> {
            OrderItemEvent orderItem = new OrderItemEvent();
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setLineTotal(item.getLineTotal());
            return orderItem;
        }).collect(Collectors.toList()));

        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        cartEventPublisher.publishCartUpdated(correlationId, event);
    }
}