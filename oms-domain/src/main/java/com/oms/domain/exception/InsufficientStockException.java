package com.oms.domain.exception;

import com.oms.domain.model.ProductId;

public final class InsufficientStockException extends DomainException {

    private final ProductId productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(ProductId productId, int requested, int available) {
        super("Insufficient stock for product " + productId.value()
                + ": requested " + requested + ", available " + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public ProductId productId() {
        return productId;
    }

    public int requested() {
        return requested;
    }

    public int available() {
        return available;
    }
}
