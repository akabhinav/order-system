package com.oms.infrastructure.external.inventory;

import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.model.OrderId;
import com.oms.domain.port.outbound.InventoryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class InventoryServiceClient implements InventoryPort {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceClient.class);

    private final RestClient restClient;

    public InventoryServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://inventory-service:8080")
                .build();
    }

    @Override
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveFallback")
    public ReservationResult reserve(OrderId orderId, List<OrderItemCommand> items) {
        log.info("Reserving inventory for order {} with {} items", orderId.value(), items.size());
        // Stub: in production this would call the actual inventory service
        log.info("Inventory reservation succeeded for order {} (stub)", orderId.value());
        return new ReservationResult(true, UUID.randomUUID().toString());
    }

    @Override
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "releaseFallback")
    public void release(OrderId orderId) {
        log.info("Releasing inventory for order {}", orderId.value());
        // Stub
        log.info("Inventory release succeeded for order {} (stub)", orderId.value());
    }

    @SuppressWarnings("unused")
    private ReservationResult reserveFallback(OrderId orderId, List<OrderItemCommand> items, Throwable t) {
        log.error("Circuit breaker fallback: inventory reservation failed for order {}", orderId.value(), t);
        throw new RuntimeException("Inventory service unavailable for order " + orderId.value(), t);
    }

    @SuppressWarnings("unused")
    private void releaseFallback(OrderId orderId, Throwable t) {
        log.error("Circuit breaker fallback: inventory release failed for order {}", orderId.value(), t);
        throw new RuntimeException("Inventory service unavailable for release on order " + orderId.value(), t);
    }
}
