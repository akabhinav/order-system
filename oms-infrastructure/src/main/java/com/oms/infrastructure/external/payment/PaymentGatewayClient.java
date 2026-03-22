package com.oms.infrastructure.external.payment;

import com.oms.domain.model.Money;
import com.oms.domain.model.OrderId;
import com.oms.domain.port.outbound.PaymentPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PaymentGatewayClient implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);

    private final RestClient restClient;

    public PaymentGatewayClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://payment-gateway:8080")
                .build();
    }

    @Override
    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "chargeFallback")
    public PaymentResult charge(OrderId orderId, Money amount, UUID idempotencyKey) {
        log.info("Charging payment for order {}: {} {}", orderId.value(), amount.amount(), amount.currency());
        // Stub: in production this would call the actual payment gateway
        log.info("Payment charge succeeded for order {} (stub)", orderId.value());
        return new PaymentResult(UUID.randomUUID().toString(), true);
    }

    @Override
    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "refundFallback")
    public PaymentResult refund(OrderId orderId, Money amount, UUID idempotencyKey) {
        log.info("Refunding payment for order {}: {} {}", orderId.value(), amount.amount(), amount.currency());
        // Stub
        log.info("Payment refund succeeded for order {} (stub)", orderId.value());
        return new PaymentResult(UUID.randomUUID().toString(), true);
    }

    @SuppressWarnings("unused")
    private PaymentResult chargeFallback(OrderId orderId, Money amount, UUID idempotencyKey, Throwable t) {
        log.error("Circuit breaker fallback: payment charge failed for order {}", orderId.value(), t);
        throw new RuntimeException("Payment service unavailable for order " + orderId.value(), t);
    }

    @SuppressWarnings("unused")
    private PaymentResult refundFallback(OrderId orderId, Money amount, UUID idempotencyKey, Throwable t) {
        log.error("Circuit breaker fallback: payment refund failed for order {}", orderId.value(), t);
        throw new RuntimeException("Payment service unavailable for refund on order " + orderId.value(), t);
    }
}
