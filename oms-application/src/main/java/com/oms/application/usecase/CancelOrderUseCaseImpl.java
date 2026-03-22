package com.oms.application.usecase;

import com.oms.domain.command.CancelOrderCommand;
import com.oms.domain.event.DomainEvent;
import com.oms.domain.exception.OrderNotFoundException;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;
import com.oms.domain.port.inbound.CancelOrderUseCase;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.domain.port.outbound.IdempotencyStore;
import com.oms.domain.port.outbound.OrderRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class CancelOrderUseCaseImpl implements CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final IdempotencyStore idempotencyStore;

    public CancelOrderUseCaseImpl(OrderRepository orderRepository,
                                  EventPublisher eventPublisher,
                                  IdempotencyStore idempotencyStore) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public OrderId execute(CancelOrderCommand command) {
        Optional<String> cached = idempotencyStore.get(command.idempotencyKey());
        if (cached.isPresent()) {
            return OrderId.of(cached.get());
        }

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        Order cancelledOrder = order.cancel(command.reason());
        List<DomainEvent> events = cancelledOrder.pendingEvents();

        orderRepository.save(cancelledOrder);
        eventPublisher.publish(events);
        idempotencyStore.set(
                command.idempotencyKey(),
                cancelledOrder.id().value().toString(),
                Duration.ofHours(24)
        );

        return cancelledOrder.id();
    }
}
