package com.oms.application.service;

import com.oms.domain.port.outbound.OrderRepository;

public class OrderDomainService {

    private final OrderRepository orderRepository;

    public OrderDomainService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
