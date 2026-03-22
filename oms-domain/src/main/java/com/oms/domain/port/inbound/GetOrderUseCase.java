package com.oms.domain.port.inbound;

import com.oms.domain.model.CustomerId;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;

import java.util.List;
import java.util.Optional;

public interface GetOrderUseCase {

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomerId(CustomerId customerId);
}
