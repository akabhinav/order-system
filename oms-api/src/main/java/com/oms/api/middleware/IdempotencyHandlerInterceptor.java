package com.oms.api.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;

public class IdempotencyHandlerInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String REDIS_KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyHandlerInterceptor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!isMutatingMethod(request.getMethod())) {
            return true;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"title\":\"Missing Required Header\",\"detail\":\"Idempotency-Key header is required for mutating requests\"}");
            return false;
        }

        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        String cachedResponse = redisTemplate.opsForValue().get(redisKey);

        if (cachedResponse != null) {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(cachedResponse);
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        if (!isMutatingMethod(request.getMethod())) {
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        // Note: This is a simplified version. A full implementation would need
        // response body capturing via a ContentCachingResponseWrapper to store
        // the actual response body in Redis.
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        int status = response.getStatus();
        if (status >= 200 && status < 300) {
            redisTemplate.opsForValue().set(redisKey,
                    "{\"status\":" + status + "}", TTL);
        }
    }

    private boolean isMutatingMethod(String method) {
        return HttpMethod.POST.name().equalsIgnoreCase(method)
                || HttpMethod.PATCH.name().equalsIgnoreCase(method);
    }
}
