package com.scaler.paymentservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.OrderCreatedEvent;
import com.scaler.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public OrderEventListener(ObjectMapper objectMapper, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${kafka.topics.order-events}", groupId = "payment-service")
    public void handleOrderEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.path("type").asText();
            if (!"order.created".equals(type)) {
                return;
            }

            OrderCreatedEvent event = objectMapper.treeToValue(root.path("payload"), OrderCreatedEvent.class);
            paymentService.processOrderCreatedEvent(event);
        } catch (Exception e) {
            log.warn("Failed to handle order event", e);
        }
    }
}