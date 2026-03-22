package com.oms.domain.exception;

import com.oms.domain.model.OrderId;

public final class OrderNotFoundException extends DomainException {

    private final OrderId orderId;

    public OrderNotFoundException(OrderId orderId) {
        super("Order not found: " + orderId.value());
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
