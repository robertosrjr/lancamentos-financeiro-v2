package com.verity.controlefinanceiro.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String payload,
    String idempotencyKey,
    Instant createdAt,
    boolean published
) {
    public static OutboxEvent create(
        UUID aggregateId,
        String aggregateType,
        String eventType,
        String payload,
        String idempotencyKey
    ) {
        return new OutboxEvent(
            UUID.randomUUID(),
            aggregateType,
            aggregateId,
            eventType,
            payload,
            idempotencyKey,
            Instant.now(),
            false
        );
    }
}
