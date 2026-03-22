package com.oms.domain.model;

import java.util.Objects;

public record OrderItem(ProductId productId, String sku, int quantity, Money unitPrice, Money totalPrice) {

    public OrderItem(ProductId productId, String sku, int quantity, Money unitPrice) {
        this(productId, sku, quantity, unitPrice, unitPrice.multiply(quantity));
    }

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(totalPrice, "totalPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        totalPrice = unitPrice.multiply(quantity);
    }
}
