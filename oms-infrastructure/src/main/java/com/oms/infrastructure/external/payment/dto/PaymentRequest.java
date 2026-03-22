package com.oms.infrastructure.external.payment.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String orderId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
