package com.oms.domain.exception;

import com.oms.domain.model.OrderStatus;

public final class InvalidOrderStateException extends DomainException {

    private final OrderStatus currentStatus;
    private final OrderStatus attemptedStatus;

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus attemptedStatus) {
        super("Cannot transition from " + currentStatus + " to " + attemptedStatus);
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }

    public OrderStatus currentStatus() {
        return currentStatus;
    }

    public OrderStatus attemptedStatus() {
        return attemptedStatus;
    }
}
