package com.oms.infrastructure.cache;

import com.oms.domain.port.outbound.IdempotencyStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<String> get(UUID key) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + key.toString());
        return Optional.ofNullable(value);
    }

    @Override
    public void set(UUID key, String result, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + key.toString(), result, ttl);
    }
}
