package com.oms.application.usecase;

import com.oms.domain.model.CustomerId;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;
import com.oms.domain.port.inbound.GetOrderUseCase;
import com.oms.domain.port.outbound.OrderRepository;

import java.util.List;
import java.util.Optional;

public class GetOrderUseCaseImpl implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
