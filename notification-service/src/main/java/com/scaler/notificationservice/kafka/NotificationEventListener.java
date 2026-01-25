package com.scaler.notificationservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.OrderStatusUpdatedEvent;
import com.scaler.ecommerce.common.events.PasswordResetRequestedEvent;
import com.scaler.ecommerce.common.events.PaymentReceiptEvent;
import com.scaler.ecommerce.common.events.UserRegisteredEvent;
import com.scaler.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public NotificationEventListener(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = {"${kafka.topics.user-events}", "${kafka.topics.order-events}", "${kafka.topics.payment-events}"},
            groupId = "notification-service")
    public void handleEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");

            if ("user.registered".equals(type)) {
                UserRegisteredEvent event = objectMapper.treeToValue(payload, UserRegisteredEvent.class);
                notificationService.sendUserRegistered(event);
            } else if ("user.password_reset.requested".equals(type)) {
                PasswordResetRequestedEvent event = objectMapper.treeToValue(payload, PasswordResetRequestedEvent.class);
                notificationService.sendPasswordReset(event);
            } else if ("order.status.updated".equals(type)) {
                OrderStatusUpdatedEvent event = objectMapper.treeToValue(payload, OrderStatusUpdatedEvent.class);
                notificationService.sendOrderStatusUpdate(event);
            } else if ("payment.receipt".equals(type)) {
                PaymentReceiptEvent event = objectMapper.treeToValue(payload, PaymentReceiptEvent.class);
                notificationService.sendPaymentReceipt(event);
            }
        } catch (Exception e) {
            log.warn("Failed to handle event", e);
        }
    }
}