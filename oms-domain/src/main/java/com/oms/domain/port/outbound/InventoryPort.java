package com.oms.domain.port.outbound;

import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.model.OrderId;

import java.util.List;

public interface InventoryPort {

    record ReservationResult(boolean success, String reservationId) {}

    ReservationResult reserve(OrderId orderId, List<OrderItemCommand> items);

    void release(OrderId orderId);
}
