package com.oms.domain.event;

import com.oms.domain.model.OrderId;

import java.time.Instant;

public record OrderDeliveredEvent(OrderId orderId, Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_DELIVERED";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
