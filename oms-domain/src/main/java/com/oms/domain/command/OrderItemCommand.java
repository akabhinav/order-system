package com.oms.domain.command;

import com.oms.domain.model.Money;
import com.oms.domain.model.ProductId;

import java.util.Objects;

public record OrderItemCommand(ProductId productId, String sku, int quantity, Money unitPrice) {

    public OrderItemCommand {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
}
