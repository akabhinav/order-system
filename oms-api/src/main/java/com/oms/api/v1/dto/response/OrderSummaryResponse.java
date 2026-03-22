package com.oms.api.v1.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
    UUID orderId,
    String status,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt
) {}
