package com.oms.application.saga;

import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.event.DomainEvent;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;
import com.oms.domain.model.SagaId;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.domain.port.outbound.InventoryPort;
import com.oms.domain.port.outbound.OrderRepository;
import com.oms.domain.port.outbound.PaymentPort;

import java.util.ArrayList;
import java.util.List;

public class OrderPlacementSaga {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final InventoryPort inventoryPort;
    private final PaymentPort paymentPort;
    private final SagaStateRepository sagaStateRepository;

    public OrderPlacementSaga(OrderRepository orderRepository,
                              EventPublisher eventPublisher,
                              InventoryPort inventoryPort,
                              PaymentPort paymentPort,
                              SagaStateRepository sagaStateRepository) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryPort = inventoryPort;
        this.paymentPort = paymentPort;
        this.sagaStateRepository = sagaStateRepository;
    }

    public SagaState execute(OrderId orderId, PlaceOrderCommand command) {
        SagaId sagaId = SagaId.generate();
        SagaState state = SagaState.start(sagaId, orderId);
        sagaStateRepository.save(state);

        List<SagaStep<SagaState>> steps = buildSteps(orderId, command);
        int completedSteps = 0;

        for (int i = 0; i < steps.size(); i++) {
            SagaStep<SagaState> step = steps.get(i);
            state = state.withStep(step.name(), i);
            sagaStateRepository.save(state);

            try {
                state = step.execute().apply(state);
                completedSteps = i + 1;
            } catch (RuntimeException ex) {
                state = state.withStatus(SagaState.COMPENSATING);
                sagaStateRepository.save(state);

                state = compensate(steps, completedSteps - 1, state);
                return state;
            }
        }

        state = state.withStatus(SagaState.COMPLETED);
        sagaStateRepository.save(state);
        return state;
    }

    private SagaState compensate(List<SagaStep<SagaState>> steps, int fromIndex, SagaState state) {
        for (int i = fromIndex; i >= 0; i--) {
            SagaStep<SagaState> step = steps.get(i);
            try {
                state = step.compensate().apply(state);
            } catch (RuntimeException ex) {
                state = state.withStatus(SagaState.FAILED);
                sagaStateRepository.save(state);
                return state;
            }
        }
        state = state.withStatus(SagaState.COMPENSATED);
        sagaStateRepository.save(state);
        return state;
    }

    private List<SagaStep<SagaState>> buildSteps(OrderId orderId, PlaceOrderCommand command) {
        List<SagaStep<SagaState>> steps = new ArrayList<>();

        steps.add(new SagaStep<>(
                "ReserveInventory",
                s -> {
                    inventoryPort.reserve(orderId, command.items());
                    return s;
                },
                s -> {
                    inventoryPort.release(orderId);
                    return s;
                }
        ));

        steps.add(new SagaStep<>(
                "ProcessPayment",
                s -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new IllegalStateException("Order not found during saga: " + orderId));
                    paymentPort.charge(orderId, order.total(), command.idempotencyKey());
                    return s;
                },
                s -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new IllegalStateException("Order not found during saga compensation: " + orderId));
                    paymentPort.refund(orderId, order.total(), command.idempotencyKey());
                    return s;
                }
        ));

        steps.add(new SagaStep<>(
                "ConfirmOrder",
                s -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new IllegalStateException("Order not found during saga confirm: " + orderId));
                    Order confirmedOrder = order.startPaymentProcessing().confirm();
                    List<DomainEvent> events = confirmedOrder.pendingEvents();
                    orderRepository.save(confirmedOrder);
                    eventPublisher.publish(events);
                    return s;
                },
                s -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new IllegalStateException("Order not found during saga cancel: " + orderId));
                    Order cancelledOrder = order.cancel("saga_compensation");
                    List<DomainEvent> events = cancelledOrder.pendingEvents();
                    orderRepository.save(cancelledOrder);
                    eventPublisher.publish(events);
                    return s;
                }
        ));

        return steps;
    }
}
