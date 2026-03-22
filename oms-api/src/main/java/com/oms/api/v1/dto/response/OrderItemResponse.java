package com.oms.api.v1.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID productId,
    String sku,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}
