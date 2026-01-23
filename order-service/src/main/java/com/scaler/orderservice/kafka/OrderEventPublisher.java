package com.scaler.orderservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.EventEnvelope;
import com.scaler.ecommerce.common.events.OrderCreatedEvent;
import com.scaler.ecommerce.common.events.OrderStatusUpdatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String orderEventsTopic;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${kafka.topics.order-events}") String orderEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.orderEventsTopic = orderEventsTopic;
    }

    public void publishOrderCreated(String correlationId, OrderCreatedEvent payload) {
        publish("order.created", correlationId, payload);
    }

    public void publishStatusUpdated(String correlationId, OrderStatusUpdatedEvent payload) {
        publish("order.status.updated", correlationId, payload);
    }

    private void publish(String eventType, String correlationId, Object payload) {
        EventEnvelope<Object> envelope = new EventEnvelope<>();
        envelope.setId(UUID.randomUUID().toString());
        envelope.setType(eventType);
        envelope.setSource("order-service");
        envelope.setOccurredAt(Instant.now());
        envelope.setCorrelationId(correlationId);
        envelope.setPayload(payload);

        try {
            String message = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(orderEventsTopic, envelope.getId(), message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order event", e);
        }
    }
}