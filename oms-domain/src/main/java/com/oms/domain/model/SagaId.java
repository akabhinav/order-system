package com.oms.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SagaId(UUID value) {

    public SagaId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static SagaId generate() {
        return new SagaId(UUID.randomUUID());
    }

    public static SagaId of(String value) {
        return new SagaId(UUID.fromString(value));
    }
}
