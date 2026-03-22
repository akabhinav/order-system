package com.oms.domain.event;

import com.oms.domain.model.*;

import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
        OrderId orderId,
        CustomerId customerId,
        List<OrderItem> items,
        Money totalAmount,
        Address shippingAddress,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_PLACED";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
