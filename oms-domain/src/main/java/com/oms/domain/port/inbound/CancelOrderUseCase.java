package com.oms.domain.port.inbound;

import com.oms.domain.command.CancelOrderCommand;
import com.oms.domain.model.OrderId;

public interface CancelOrderUseCase {

    OrderId execute(CancelOrderCommand command);
}
