package com.oms.infrastructure.messaging.kafka.schema;

import java.time.Instant;

/**
 * Standard envelope for order events published to Kafka.
 * Includes metadata for schema evolution and tracing.
 */
public record OrderEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String orderId,
        Instant occurredAt,
        String payload
) {
}
