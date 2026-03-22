package com.oms.domain.command;

import com.oms.domain.model.Address;
import com.oms.domain.model.CustomerId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlaceOrderCommand(
        CustomerId customerId,
        List<OrderItemCommand> items,
        Address shippingAddress,
        UUID idempotencyKey
) {

    public PlaceOrderCommand {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(shippingAddress, "shippingAddress must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        items = List.copyOf(items);
    }
}
