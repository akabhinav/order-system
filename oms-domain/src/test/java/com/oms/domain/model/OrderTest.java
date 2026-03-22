package com.oms.domain.model;

import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.event.*;
import com.oms.domain.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    private static final Currency USD = Currency.getInstance("USD");

    private PlaceOrderCommand createPlaceOrderCommand() {
        return new PlaceOrderCommand(
                CustomerId.generate(),
                List.of(
                        new OrderItemCommand(ProductId.generate(), "SKU-001", 2,
                                new Money(new BigDecimal("25.00"), USD)),
                        new OrderItemCommand(ProductId.generate(), "SKU-002", 1,
                                new Money(new BigDecimal("15.00"), USD))
                ),
                new Address("123 Main St", "Springfield", "IL", "62701", "US"),
                UUID.randomUUID()
        );
    }

    @Test
    void place_createsPendingOrderWithCorrectFieldsAndEvent() {
        PlaceOrderCommand cmd = createPlaceOrderCommand();
        Order order = Order.place(cmd);

        assertThat(order.id()).isNotNull();
        assertThat(order.customerId()).isEqualTo(cmd.customerId());
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.items()).hasSize(2);
        assertThat(order.subtotal().amount()).isEqualByComparingTo(new BigDecimal("65.00"));
        assertThat(order.tax().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.shipping().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.total().amount()).isEqualByComparingTo(new BigDecimal("65.00"));
        assertThat(order.shippingAddress()).isEqualTo(cmd.shippingAddress());
        assertThat(order.idempotencyKey()).isEqualTo(cmd.idempotencyKey());
        assertThat(order.createdAt()).isNotNull();
        assertThat(order.updatedAt()).isNotNull();

        assertThat(order.pendingEvents()).hasSize(1);
        assertThat(order.pendingEvents().getFirst()).isInstanceOf(OrderPlacedEvent.class);
        OrderPlacedEvent event = (OrderPlacedEvent) order.pendingEvents().getFirst();
        assertThat(event.orderId()).isEqualTo(order.id());
        assertThat(event.customerId()).isEqualTo(cmd.customerId());
        assertThat(event.eventType()).isEqualTo("ORDER_PLACED");
        assertThat(event.schemaVersion()).isEqualTo(1);
    }

    @Test
    void confirm_fromPaymentProcessing_succeeds() {
        Order order = Order.place(createPlaceOrderCommand())
                .startPaymentProcessing()
                .confirm();

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.pendingEvents()).anyMatch(e -> e instanceof OrderConfirmedEvent);
    }

    @Test
    void confirm_fromPending_throwsInvalidOrderStateException() {
        Order order = Order.place(createPlaceOrderCommand());

        assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void cancel_fromPending_succeeds() {
        Order order = Order.place(createPlaceOrderCommand())
                .cancel("customer request");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.pendingEvents()).anyMatch(e -> e instanceof OrderCancelledEvent);
    }

    @Test
    void cancel_fromConfirmed_succeeds() {
        Order order = Order.place(createPlaceOrderCommand())
                .startPaymentProcessing()
                .confirm()
                .cancel("out of stock");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void ship_fromPacked_succeeds() {
        Order order = Order.place(createPlaceOrderCommand())
                .startPaymentProcessing()
                .confirm()
                .startPickup()
                .pack()
                .ship("TRACK-123");

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.pendingEvents()).anyMatch(e ->
                e instanceof OrderShippedEvent shipped
                        && shipped.trackingNumber().equals("TRACK-123"));
    }

    @Test
    void deliver_fromShipped_succeeds() {
        Order order = Order.place(createPlaceOrderCommand())
                .startPaymentProcessing()
                .confirm()
                .startPickup()
                .pack()
                .ship("TRACK-123")
                .deliver();

        assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.pendingEvents()).anyMatch(e -> e instanceof OrderDeliveredEvent);
    }

    @Test
    void drainPendingEvents_returnsEventsAndClears() {
        Order order = Order.place(createPlaceOrderCommand());
        assertThat(order.pendingEvents()).hasSize(1);

        List<DomainEvent> drained = order.pendingEvents();
        Order drained_order = order.drainPendingEvents();

        assertThat(drained).hasSize(1);
        assertThat(drained_order.pendingEvents()).isEmpty();
    }

    @Test
    void fullLifecycle_placeToDelivered() {
        Order order = Order.place(createPlaceOrderCommand());
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);

        order = order.startPaymentProcessing();
        assertThat(order.status()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);

        order = order.confirm();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);

        order = order.startPickup();
        assertThat(order.status()).isEqualTo(OrderStatus.PICKING);

        order = order.pack();
        assertThat(order.status()).isEqualTo(OrderStatus.PACKED);

        order = order.ship("TRACK-FULL-001");
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);

        order = order.deliver();
        assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);

        // Verify all events accumulated
        assertThat(order.pendingEvents())
                .hasSize(4) // OrderPlaced, OrderConfirmed, OrderShipped, OrderDelivered
                .extracting(DomainEvent::eventType)
                .containsExactly("ORDER_PLACED", "ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED");
    }

    @Test
    void cancel_fromDelivered_throwsInvalidOrderStateException() {
        Order order = Order.place(createPlaceOrderCommand())
                .startPaymentProcessing()
                .confirm()
                .startPickup()
                .pack()
                .ship("TRACK-123")
                .deliver();

        assertThatThrownBy(() -> order.cancel("too late"))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
