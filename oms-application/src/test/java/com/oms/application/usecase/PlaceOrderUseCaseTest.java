package com.oms.application.usecase;

import com.oms.application.saga.SagaOrchestrator;
import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.event.DomainEvent;
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
import java.time.Duration;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotencyStore idempotencyStore;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    private PlaceOrderUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PlaceOrderUseCaseImpl(
                orderRepository, eventPublisher, idempotencyStore, sagaOrchestrator);
    }

    @Test
    void shouldPlaceOrderSuccessfully() {
        PlaceOrderCommand command = createCommand();
        when(idempotencyStore.get(command.idempotencyKey())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderId result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.value()).isNotNull();

        verify(idempotencyStore).get(command.idempotencyKey());
        verify(orderRepository).save(any(Order.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(eventsCaptor.capture());
        assertThat(eventsCaptor.getValue()).hasSize(1);

        verify(sagaOrchestrator).start(eq(result), eq(command));
        verify(idempotencyStore).set(
                eq(command.idempotencyKey()),
                eq(result.value().toString()),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void shouldReturnCachedOrderIdWhenIdempotencyKeyExists() {
        UUID existingOrderId = UUID.randomUUID();
        PlaceOrderCommand command = createCommand();
        when(idempotencyStore.get(command.idempotencyKey()))
                .thenReturn(Optional.of(existingOrderId.toString()));

        OrderId result = useCase.execute(command);

        assertThat(result).isEqualTo(OrderId.of(existingOrderId.toString()));

        verify(idempotencyStore).get(command.idempotencyKey());
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(sagaOrchestrator);
        verify(idempotencyStore, never()).set(any(), any(), any());
    }

    private PlaceOrderCommand createCommand() {
        Currency usd = Currency.getInstance("USD");
        return new PlaceOrderCommand(
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
    }
}
