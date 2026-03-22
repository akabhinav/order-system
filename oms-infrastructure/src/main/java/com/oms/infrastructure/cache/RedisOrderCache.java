package com.oms.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed cache for order response objects.
 * Caches serialized order responses to reduce database load on read-heavy paths.
 */
@Component
public class RedisOrderCache {

    private static final Logger log = LoggerFactory.getLogger(RedisOrderCache.class);
    private static final String KEY_PREFIX = "order:cache:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisOrderCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(UUID orderId, Class<T> type) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + orderId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached order {}, evicting entry", orderId, e);
            evict(orderId);
            return Optional.empty();
        }
    }

    public <T> void set(UUID orderId, T value) {
        set(orderId, value, DEFAULT_TTL);
    }

    public <T> void set(UUID orderId, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(KEY_PREFIX + orderId, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize order {} for caching", orderId, e);
        }
    }

    public void evict(UUID orderId) {
        redisTemplate.delete(KEY_PREFIX + orderId);
    }
}
