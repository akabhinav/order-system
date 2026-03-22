package com.oms.domain.exception;

import java.util.UUID;

public final class DuplicateOrderException extends DomainException {

    private final UUID idempotencyKey;

    public DuplicateOrderException(UUID idempotencyKey) {
        super("Duplicate order with idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public UUID idempotencyKey() {
        return idempotencyKey;
    }
}
