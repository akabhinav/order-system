package com.oms.domain.port.outbound;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {

    Optional<String> get(UUID key);

    void set(UUID key, String result, Duration ttl);
}
