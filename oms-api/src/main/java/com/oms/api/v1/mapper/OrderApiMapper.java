package com.oms.api.v1.mapper;

import com.oms.api.v1.dto.request.OrderItemRequest;
import com.oms.api.v1.dto.request.PlaceOrderRequest;
import com.oms.api.v1.dto.response.*;
import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.*;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class OrderApiMapper {

    private OrderApiMapper() {}

    public static PlaceOrderCommand toCommand(PlaceOrderRequest request, UUID idempotencyKey) {
        Address address = new Address(
                request.shippingAddress().street(),
                request.shippingAddress().city(),
                request.shippingAddress().state(),
                request.shippingAddress().postalCode(),
                request.shippingAddress().countryCode()
        );

        var items = request.items().stream()
                .map(OrderApiMapper::toItemCommand)
                .toList();

        return new PlaceOrderCommand(
                new CustomerId(request.customerId()),
                items,
                address,
                idempotencyKey
        );
    }

    private static OrderItemCommand toItemCommand(OrderItemRequest item) {
        Money unitPrice = new Money(item.unitPrice(), Currency.getInstance(item.currency()));
        return new OrderItemCommand(
                new ProductId(item.productId()),
                item.sku(),
                item.quantity(),
                unitPrice
        );
    }

    public static OrderResponse toResponse(Order order) {
        var itemResponses = order.items().stream()
                .map(OrderApiMapper::toItemResponse)
                .toList();

        AddressResponse addressResponse = toAddressResponse(order.shippingAddress());

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/orders/" + order.id().value());
        links.put("cancel", "/api/v1/orders/" + order.id().value() + "/cancel");

        return new OrderResponse(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                order.total().amount(),
                order.total().currency().getCurrencyCode(),
                addressResponse,
                itemResponses,
                order.createdAt(),
                links
        );
    }

    public static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.id().value(),
                order.status().name(),
                order.total().amount(),
                order.total().currency().getCurrencyCode(),
                order.createdAt()
        );
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.productId().value(),
                item.sku(),
                item.quantity(),
                item.unitPrice().amount(),
                item.totalPrice().amount()
        );
    }

    private static AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.street(),
                address.city(),
                address.state(),
                address.postalCode(),
                address.countryCode()
        );
    }
}
