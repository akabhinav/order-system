package com.oms.domain.model;

import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.event.*;
import com.oms.domain.exception.InvalidOrderStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final OrderStatus status;
    private final List<OrderItem> items;
    private final Money subtotal;
    private final Money tax;
    private final Money shipping;
    private final Money total;
    private final Address shippingAddress;
    private final UUID idempotencyKey;
    private final List<DomainEvent> pendingEvents;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Order(OrderId id,
                  CustomerId customerId,
                  OrderStatus status,
                  List<OrderItem> items,
                  Money subtotal,
                  Money tax,
                  Money shipping,
                  Money total,
                  Address shippingAddress,
                  UUID idempotencyKey,
                  List<DomainEvent> pendingEvents,
                  long version,
                  Instant createdAt,
                  Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.subtotal = subtotal;
        this.tax = tax;
        this.shipping = shipping;
        this.total = total;
        this.shippingAddress = shippingAddress;
        this.idempotencyKey = idempotencyKey;
        this.pendingEvents = Collections.unmodifiableList(new ArrayList<>(pendingEvents));
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order place(PlaceOrderCommand cmd) {
        OrderId orderId = OrderId.generate();
        Instant now = Instant.now();
        Currency currency = cmd.items().getFirst().unitPrice().currency();

        List<OrderItem> orderItems = cmd.items().stream()
                .map(ic -> new OrderItem(ic.productId(), ic.sku(), ic.quantity(), ic.unitPrice()))
                .toList();

        Money subtotal = orderItems.stream()
                .map(OrderItem::totalPrice)
                .reduce(Money.zero(currency), Money::add);

        Money tax = Money.zero(currency);
        Money shipping = Money.zero(currency);
        Money total = subtotal;

        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId, cmd.customerId(), orderItems, total, cmd.shippingAddress(), now);

        return new Order(orderId, cmd.customerId(), OrderStatus.PENDING,
                orderItems, subtotal, tax, shipping, total,
                cmd.shippingAddress(), cmd.idempotencyKey(),
                List.of(event), 0L, now, now);
    }

    public Order confirm() {
        return transitionTo(OrderStatus.CONFIRMED,
                new OrderConfirmedEvent(id, Instant.now()));
    }

    public Order cancel(String reason) {
        return transitionTo(OrderStatus.CANCELLED,
                new OrderCancelledEvent(id, reason, Instant.now()));
    }

    public Order ship(String trackingNumber) {
        return transitionTo(OrderStatus.SHIPPED,
                new OrderShippedEvent(id, trackingNumber, Instant.now()));
    }

    public Order deliver() {
        return transitionTo(OrderStatus.DELIVERED,
                new OrderDeliveredEvent(id, Instant.now()));
    }

    public Order startPaymentProcessing() {
        return transitionTo(OrderStatus.PAYMENT_PROCESSING, null);
    }

    public Order startPickup() {
        return transitionTo(OrderStatus.PICKING, null);
    }

    public Order pack() {
        return transitionTo(OrderStatus.PACKED, null);
    }

    public Order requestRefund() {
        return transitionTo(OrderStatus.REFUND_REQUESTED, null);
    }

    public Order refund() {
        return transitionTo(OrderStatus.REFUNDED, null);
    }

    public static Order reconstitute(OrderId id, CustomerId customerId, OrderStatus status,
                                      List<OrderItem> items, Money subtotal, Money tax,
                                      Money shipping, Money total, Address shippingAddress,
                                      java.util.UUID idempotencyKey, long version,
                                      Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, status, items, subtotal, tax, shipping, total,
                shippingAddress, idempotencyKey, List.of(), version, createdAt, updatedAt);
    }

    public Order drainPendingEvents() {
        return new Order(id, customerId, status, items, subtotal, tax, shipping, total,
                shippingAddress, idempotencyKey, List.of(), version, createdAt, updatedAt);
    }

    private Order transitionTo(OrderStatus target, DomainEvent event) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStateException(status, target);
        }
        List<DomainEvent> newEvents = new ArrayList<>(pendingEvents);
        if (event != null) {
            newEvents.add(event);
        }
        return new Order(id, customerId, target, items, subtotal, tax, shipping, total,
                shippingAddress, idempotencyKey, newEvents, version, createdAt, Instant.now());
    }

    // Record-style accessors
    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public List<OrderItem> items() { return items; }
    public Money subtotal() { return subtotal; }
    public Money tax() { return tax; }
    public Money shipping() { return shipping; }
    public Money total() { return total; }
    public Address shippingAddress() { return shippingAddress; }
    public UUID idempotencyKey() { return idempotencyKey; }
    public List<DomainEvent> pendingEvents() { return pendingEvents; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
