package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EventEnvelope<T> {
    private String id;
    private String type;
    private String source;
    private Instant occurredAt;
    private String correlationId;
    private T payload;
}
