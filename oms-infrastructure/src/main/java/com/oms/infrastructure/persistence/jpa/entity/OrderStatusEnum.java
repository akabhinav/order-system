package com.oms.infrastructure.persistence.jpa.entity;

/**
 * JPA-level enum mirroring the domain {@link com.oms.domain.model.OrderStatus} values.
 * Kept separate so infrastructure layer does not leak JPA annotations into the domain.
 */
public enum OrderStatusEnum {
    PLACED,
    CONFIRMED,
    CANCELLED,
    SHIPPED,
    DELIVERED,
    REFUNDED
}
