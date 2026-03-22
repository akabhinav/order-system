package com.oms.domain.port.outbound;

import com.oms.domain.model.CustomerId;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomerId(CustomerId customerId);
}
