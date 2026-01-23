package com.scaler.orderservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.PaymentCompletedEvent;
import com.scaler.ecommerce.common.events.PaymentFailedEvent;
import com.scaler.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentEventListener(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "order-service")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");

            if ("payment.completed".equals(type)) {
                PaymentCompletedEvent event = objectMapper.treeToValue(payload, PaymentCompletedEvent.class);
                orderService.handlePaymentCompleted(event.getOrderId(), event.getUserId(), event.getReceiptNumber());
            } else if ("payment.failed".equals(type)) {
                PaymentFailedEvent event = objectMapper.treeToValue(payload, PaymentFailedEvent.class);
                orderService.handlePaymentFailed(event.getOrderId(), event.getUserId(), event.getFailureReason());
            }
        } catch (Exception e) {
            log.warn("Failed to handle payment event", e);
        }
    }
}