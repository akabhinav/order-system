package com.oms.domain.port.outbound;

import com.oms.domain.model.Money;
import com.oms.domain.model.OrderId;

import java.util.UUID;

public interface PaymentPort {

    record PaymentResult(String transactionId, boolean success) {}

    PaymentResult charge(OrderId orderId, Money amount, UUID idempotencyKey);

    PaymentResult refund(OrderId orderId, Money amount, UUID idempotencyKey);
}
