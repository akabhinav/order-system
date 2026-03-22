package com.oms.application.saga;

import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.OrderId;

public class OrderPlacementSagaOrchestrator implements SagaOrchestrator {

    private final OrderPlacementSaga orderPlacementSaga;

    public OrderPlacementSagaOrchestrator(OrderPlacementSaga orderPlacementSaga) {
        this.orderPlacementSaga = orderPlacementSaga;
    }

    @Override
    public void start(OrderId orderId, PlaceOrderCommand command) {
        orderPlacementSaga.execute(orderId, command);
    }
}
