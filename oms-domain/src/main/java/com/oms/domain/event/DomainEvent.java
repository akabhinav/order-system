package com.oms.domain.event;

import com.oms.domain.model.OrderId;

import java.time.Instant;

public sealed interface DomainEvent
        permits OrderPlacedEvent, OrderConfirmedEvent, OrderShippedEvent,
                OrderCancelledEvent, OrderDeliveredEvent {

    OrderId orderId();

    Instant occurredAt();

    String eventType();

    int schemaVersion();
}
