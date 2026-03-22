package com.oms.domain.model;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    CONFIRMED,
    PICKING,
    PACKED,
    SHIPPED,
    DELIVERED,
    REFUND_REQUESTED,
    REFUNDED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(PAYMENT_PROCESSING, CANCELLED),
            PAYMENT_PROCESSING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PICKING, CANCELLED),
            PICKING, Set.of(PACKED, CANCELLED),
            PACKED, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(REFUND_REQUESTED),
            REFUND_REQUESTED, Set.of(REFUNDED),
            REFUNDED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
