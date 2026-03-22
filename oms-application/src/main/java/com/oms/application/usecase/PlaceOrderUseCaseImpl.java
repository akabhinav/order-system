package com.oms.application.usecase;

import com.oms.application.saga.SagaOrchestrator;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.event.DomainEvent;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;
import com.oms.domain.port.inbound.PlaceOrderUseCase;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.domain.port.outbound.IdempotencyStore;
import com.oms.domain.port.outbound.OrderRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final IdempotencyStore idempotencyStore;
    private final SagaOrchestrator sagaOrchestrator;

    public PlaceOrderUseCaseImpl(OrderRepository orderRepository,
                                 EventPublisher eventPublisher,
                                 IdempotencyStore idempotencyStore,
                                 SagaOrchestrator sagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyStore = idempotencyStore;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Override
    public OrderId execute(PlaceOrderCommand command) {
        Optional<String> cached = idempotencyStore.get(command.idempotencyKey());
        if (cached.isPresent()) {
            return OrderId.of(cached.get());
        }

        Order order = Order.place(command);
        List<DomainEvent> events = order.pendingEvents();

        orderRepository.save(order);
        eventPublisher.publish(events);
        sagaOrchestrator.start(order.id(), command);
        idempotencyStore.set(
                command.idempotencyKey(),
                order.id().value().toString(),
                Duration.ofHours(24)
        );

        return order.id();
    }
}
