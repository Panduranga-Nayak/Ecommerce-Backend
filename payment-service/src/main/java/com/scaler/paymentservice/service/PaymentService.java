package com.scaler.paymentservice.service;

import com.scaler.ecommerce.common.events.OrderCreatedEvent;
import com.scaler.paymentservice.dto.PaymentRequestDto;
import com.scaler.paymentservice.model.Payment;

public interface PaymentService {
    Payment createPayment(PaymentRequestDto request, Long userId, String idempotencyKey);

    Payment getPayment(Long paymentId, Long userId);

    Payment getPaymentByOrderId(Long orderId, Long userId);

    void processOrderCreatedEvent(OrderCreatedEvent event);

    void markPaymentCompleted(Long paymentId, String providerReference, String receiptNumber);

    void markPaymentFailed(Long paymentId, String failureReason);
}