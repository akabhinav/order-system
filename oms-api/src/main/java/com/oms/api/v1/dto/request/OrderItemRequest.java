package com.oms.api.v1.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
    @NotNull UUID productId,
    @NotBlank String sku,
    @Min(1) @Max(1000) int quantity,
    @NotNull @Positive BigDecimal unitPrice,
    @NotNull String currency
) {}
