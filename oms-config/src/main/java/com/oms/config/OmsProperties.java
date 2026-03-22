package com.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oms")
public record OmsProperties(
    IdempotencyProperties idempotency,
    OutboxProperties outbox,
    SagaProperties saga
) {
    public record IdempotencyProperties(int ttlHours) {
        public IdempotencyProperties {
            if (ttlHours <= 0) ttlHours = 24;
        }
    }
    public record OutboxProperties(int pollIntervalMs, int batchSize) {
        public OutboxProperties {
            if (pollIntervalMs <= 0) pollIntervalMs = 100;
            if (batchSize <= 0) batchSize = 100;
        }
    }
    public record SagaProperties(int timeoutMinutes) {
        public SagaProperties {
            if (timeoutMinutes <= 0) timeoutMinutes = 15;
        }
    }
}
