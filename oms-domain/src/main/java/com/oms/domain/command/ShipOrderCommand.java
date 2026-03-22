package com.oms.domain.command;

import com.oms.domain.model.OrderId;

import java.util.UUID;

public record ShipOrderCommand(OrderId orderId, String trackingNumber, UUID idempotencyKey) {
}
