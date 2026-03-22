package com.oms.application.saga;

import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.*;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.domain.port.outbound.InventoryPort;
import com.oms.domain.port.outbound.OrderRepository;
import com.oms.domain.port.outbound.PaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPlacementSagaTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private InventoryPort inventoryPort;

    @Mock
    private PaymentPort paymentPort;

    @Mock
    private SagaStateRepository sagaStateRepository;

    private OrderPlacementSaga saga;
    private OrderId orderId;
    private PlaceOrderCommand command;
    private Order placedOrder;

    @BeforeEach
    void setUp() {
        saga = new OrderPlacementSaga(
                orderRepository, eventPublisher, inventoryPort, paymentPort, sagaStateRepository);

        Currency usd = Currency.getInstance("USD");
        command = new PlaceOrderCommand(
                CustomerId.generate(),
                List.of(new OrderItemCommand(
                        ProductId.generate(),
                        "WIDGET-001",
                        2,
                        new Money(new BigDecimal("10.00"), usd)
                )),
                new Address("123 Main St", "Springfield", "IL", "62701", "US"),
                UUID.randomUUID()
        );
        placedOrder = Order.place(command);
        orderId = placedOrder.id();

        when(sagaStateRepository.save(any(SagaState.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldCompleteAllStepsSuccessfully() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(placedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        SagaState result = saga.execute(orderId, command);

        assertThat(result.status()).isEqualTo(SagaState.COMPLETED);

        verify(inventoryPort).reserve(eq(orderId), eq(command.items()));
        verify(paymentPort).charge(eq(orderId), eq(placedOrder.total()), eq(command.idempotencyKey()));
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(any());

        verify(inventoryPort, never()).release(any());
        verify(paymentPort, never()).refund(any(), any(), any());
    }

    @Test
    void shouldFailWithNoCompensationWhenStep1Fails() {
        doThrow(new RuntimeException("Inventory unavailable"))
                .when(inventoryPort).reserve(eq(orderId), eq(command.items()));

        SagaState result = saga.execute(orderId, command);

        assertThat(result.status()).isEqualTo(SagaState.COMPENSATED);

        verify(inventoryPort).reserve(eq(orderId), eq(command.items()));
        verify(paymentPort, never()).charge(any(), any(), any());
        verify(paymentPort, never()).refund(any(), any(), any());
        verify(inventoryPort, never()).release(any());
    }

    @Test
    void shouldCompensateStep1WhenStep2Fails() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(placedOrder));
        doThrow(new RuntimeException("Payment declined"))
                .when(paymentPort).charge(eq(orderId), any(Money.class), eq(command.idempotencyKey()));

        SagaState result = saga.execute(orderId, command);

        assertThat(result.status()).isEqualTo(SagaState.COMPENSATED);

        verify(inventoryPort).reserve(eq(orderId), eq(command.items()));
        verify(inventoryPort).release(orderId);
        verify(paymentPort, never()).refund(any(), any(), any());
    }

    @Test
    void shouldCompensateStep2AndStep1WhenStep3Fails() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(placedOrder))
                .thenReturn(Optional.of(placedOrder))
                .thenReturn(Optional.of(placedOrder));
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new RuntimeException("DB error on confirm"))
                .thenAnswer(inv -> inv.getArgument(0));

        SagaState result = saga.execute(orderId, command);

        assertThat(result.status()).isEqualTo(SagaState.COMPENSATED);

        verify(inventoryPort).reserve(eq(orderId), eq(command.items()));
        verify(paymentPort).charge(eq(orderId), eq(placedOrder.total()), eq(command.idempotencyKey()));

        verify(paymentPort).refund(eq(orderId), any(Money.class), eq(command.idempotencyKey()));
        verify(inventoryPort).release(orderId);
    }
}
