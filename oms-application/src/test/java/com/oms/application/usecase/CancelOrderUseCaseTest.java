package com.oms.application.usecase;

import com.oms.domain.command.CancelOrderCommand;
import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.event.DomainEvent;
import com.oms.domain.exception.InvalidOrderStateException;
import com.oms.domain.exception.OrderNotFoundException;
import com.oms.domain.model.*;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.domain.port.outbound.IdempotencyStore;
import com.oms.domain.port.outbound.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotencyStore idempotencyStore;

    private CancelOrderUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelOrderUseCaseImpl(orderRepository, eventPublisher, idempotencyStore);
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        Order existingOrder = createPlacedOrder();
        CancelOrderCommand command = new CancelOrderCommand(
                existingOrder.id(), "Customer requested", UUID.randomUUID());

        when(idempotencyStore.get(command.idempotencyKey())).thenReturn(Optional.empty());
        when(orderRepository.findById(existingOrder.id())).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderId result = useCase.execute(command);

        assertThat(result).isEqualTo(existingOrder.id());
        verify(orderRepository).save(any(Order.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(eventsCaptor.capture());
        assertThat(eventsCaptor.getValue()).hasSize(1);
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        OrderId orderId = OrderId.generate();
        CancelOrderCommand command = new CancelOrderCommand(
                orderId, "Customer requested", UUID.randomUUID());

        when(idempotencyStore.get(command.idempotencyKey())).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldThrowWhenOrderInInvalidState() {
        Order placedOrder = createPlacedOrder();
        Order cancelledOrder = placedOrder.cancel("first cancellation");

        CancelOrderCommand command = new CancelOrderCommand(
                cancelledOrder.id(), "Second cancel attempt", UUID.randomUUID());

        when(idempotencyStore.get(command.idempotencyKey())).thenReturn(Optional.empty());
        when(orderRepository.findById(cancelledOrder.id())).thenReturn(Optional.of(cancelledOrder));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    private Order createPlacedOrder() {
        Currency usd = Currency.getInstance("USD");
        PlaceOrderCommand cmd = new PlaceOrderCommand(
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
        return Order.place(cmd);
    }
}
