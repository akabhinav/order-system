package com.oms.domain.exception;

public abstract sealed class DomainException extends RuntimeException
        permits InvalidOrderStateException, InsufficientStockException,
                OrderNotFoundException, DuplicateOrderException {

    protected DomainException(String message) {
        super(message);
    }
}
