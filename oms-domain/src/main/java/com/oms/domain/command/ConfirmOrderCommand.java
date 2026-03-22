package com.oms.domain.command;

import com.oms.domain.model.OrderId;

import java.util.UUID;

public record ConfirmOrderCommand(OrderId orderId, UUID idempotencyKey) {
}
