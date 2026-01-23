package com.scaler.orderservice.model.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}