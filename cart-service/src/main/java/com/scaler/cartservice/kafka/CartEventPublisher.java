package com.scaler.cartservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.CartUpdatedEvent;
import com.scaler.ecommerce.common.events.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CartEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String cartEventsTopic;

    public CartEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              @Value("${kafka.topics.cart-events}") String cartEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.cartEventsTopic = cartEventsTopic;
    }

    public void publishCartUpdated(String correlationId, CartUpdatedEvent payload) {
        EventEnvelope<CartUpdatedEvent> envelope = new EventEnvelope<>();
        envelope.setId(UUID.randomUUID().toString());
        envelope.setType("cart.updated");
        envelope.setSource("cart-service");
        envelope.setOccurredAt(Instant.now());
        envelope.setCorrelationId(correlationId);
        envelope.setPayload(payload);

        try {
            String message = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(cartEventsTopic, envelope.getId(), message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cart event", e);
        }
    }
}