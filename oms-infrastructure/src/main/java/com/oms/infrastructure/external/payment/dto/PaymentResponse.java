package com.oms.infrastructure.external.payment.dto;

public record PaymentResponse(
        String transactionId,
        String status,
        String message
) {
}
