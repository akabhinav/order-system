package com.oms.infrastructure.saga;

import com.oms.application.saga.OrderPlacementSaga;
import com.oms.application.saga.SagaOrchestrator;
import com.oms.application.saga.SagaState;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dispatches saga execution to a virtual thread so the HTTP request
 * returns immediately with 202 Accepted.
 *
 * With Java 21 virtual threads, each saga runs on its own lightweight thread.
 * Blocking I/O inside the saga (DB, HTTP, Redis) is cheap on virtual threads.
 */
@Component
public class AsyncSagaOrchestrator implements SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AsyncSagaOrchestrator.class);

    private final OrderPlacementSaga orderPlacementSaga;
    private final ExecutorService executor;

    public AsyncSagaOrchestrator(OrderPlacementSaga orderPlacementSaga) {
        this.orderPlacementSaga = orderPlacementSaga;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void start(OrderId orderId, PlaceOrderCommand command) {
        executor.submit(() -> {
            try {
                log.info("Starting saga for order {}", orderId.value());
                SagaState result = orderPlacementSaga.execute(orderId, command);
                log.info("Saga completed for order {} with status {}", orderId.value(), result.status());
            } catch (Exception e) {
                log.error("Saga failed for order {}", orderId.value(), e);
            }
        });
    }
}
