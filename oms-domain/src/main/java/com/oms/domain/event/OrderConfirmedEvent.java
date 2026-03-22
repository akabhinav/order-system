package com.oms.domain.event;

import com.oms.domain.model.OrderId;

import java.time.Instant;

public record OrderConfirmedEvent(OrderId orderId, Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_CONFIRMED";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
