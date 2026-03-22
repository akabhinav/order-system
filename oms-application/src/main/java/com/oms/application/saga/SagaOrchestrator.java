package com.oms.application.saga;

import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.OrderId;

public interface SagaOrchestrator {
    void start(OrderId orderId, PlaceOrderCommand command);
}
