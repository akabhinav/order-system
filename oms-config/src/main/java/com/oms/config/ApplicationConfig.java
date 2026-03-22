package com.oms.config;

import com.oms.application.saga.OrderPlacementSaga;
import com.oms.application.saga.OrderPlacementSagaOrchestrator;
import com.oms.application.saga.SagaOrchestrator;
import com.oms.application.saga.SagaStateRepository;
import com.oms.application.usecase.CancelOrderUseCaseImpl;
import com.oms.application.usecase.GetOrderUseCaseImpl;
import com.oms.application.usecase.PlaceOrderUseCaseImpl;
import com.oms.domain.port.inbound.CancelOrderUseCase;
import com.oms.domain.port.inbound.GetOrderUseCase;
import com.oms.domain.port.inbound.PlaceOrderUseCase;
import com.oms.domain.port.outbound.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public PlaceOrderUseCase placeOrderUseCase(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            IdempotencyStore idempotencyStore,
            SagaOrchestrator sagaOrchestrator) {
        return new PlaceOrderUseCaseImpl(orderRepository, eventPublisher, idempotencyStore, sagaOrchestrator);
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderUseCaseImpl(orderRepository);
    }

    @Bean
    public CancelOrderUseCase cancelOrderUseCase(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            IdempotencyStore idempotencyStore) {
        return new CancelOrderUseCaseImpl(orderRepository, eventPublisher, idempotencyStore);
    }

    @Bean
    public OrderPlacementSaga orderPlacementSaga(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            InventoryPort inventoryPort,
            PaymentPort paymentPort,
            SagaStateRepository sagaStateRepository) {
        return new OrderPlacementSaga(orderRepository, eventPublisher, inventoryPort, paymentPort, sagaStateRepository);
    }

    @Bean
    public SagaOrchestrator sagaOrchestrator(OrderPlacementSaga orderPlacementSaga) {
        return new OrderPlacementSagaOrchestrator(orderPlacementSaga);
    }
}
