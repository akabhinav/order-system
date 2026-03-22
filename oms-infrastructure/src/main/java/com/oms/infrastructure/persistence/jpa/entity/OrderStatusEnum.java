package com.oms.infrastructure.persistence.jpa.entity;

/**
 * JPA-level enum mirroring the domain {@link com.oms.domain.model.OrderStatus} values.
 * Kept separate so infrastructure layer does not leak JPA annotations into the domain.
 */
public enum OrderStatusEnum {
    PENDING,
    PAYMENT_PROCESSING,
    CONFIRMED,
    PICKING,
    PACKED,
    SHIPPED,
    DELIVERED,
    REFUND_REQUESTED,
    REFUNDED,
    CANCELLED
}
