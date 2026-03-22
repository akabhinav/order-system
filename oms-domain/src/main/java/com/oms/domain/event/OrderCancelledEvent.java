package com.oms.domain.event;

import com.oms.domain.model.OrderId;

import java.time.Instant;

public record OrderCancelledEvent(OrderId orderId, String reason, Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_CANCELLED";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
