package com.oms.domain.event;

import com.oms.domain.model.OrderId;

import java.time.Instant;

public record OrderShippedEvent(OrderId orderId, String trackingNumber, Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_SHIPPED";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
