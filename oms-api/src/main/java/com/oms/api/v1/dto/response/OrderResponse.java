package com.oms.api.v1.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
    UUID orderId,
    UUID customerId,
    String status,
    BigDecimal totalAmount,
    String currency,
    AddressResponse shippingAddress,
    List<OrderItemResponse> items,
    Instant createdAt,
    Map<String, String> links
) {}
