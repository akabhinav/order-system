package com.oms.domain.port.inbound;

import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.exception.DomainException;
import com.oms.domain.model.OrderId;

public interface PlaceOrderUseCase {

    OrderId execute(PlaceOrderCommand command) throws DomainException;
}
