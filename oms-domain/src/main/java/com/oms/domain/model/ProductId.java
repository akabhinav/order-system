package com.oms.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }
}
