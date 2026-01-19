package com.scaler.userservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class KafkaEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String userEventsTopic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${kafka.topics.user-events}") String userEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.userEventsTopic = userEventsTopic;
    }

    public void publish(String eventType, String correlationId, Object payload) {
        EventEnvelope<Object> envelope = new EventEnvelope<>();
        envelope.setId(UUID.randomUUID().toString());
        envelope.setType(eventType);
        envelope.setSource("user-service");
        envelope.setOccurredAt(Instant.now());
        envelope.setCorrelationId(correlationId);
        envelope.setPayload(payload);

        try {
            String message = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(userEventsTopic, envelope.getId(), message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}