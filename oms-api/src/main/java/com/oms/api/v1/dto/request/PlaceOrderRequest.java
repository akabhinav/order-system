package com.oms.api.v1.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
    @NotNull(message = "Customer ID is required")
    UUID customerId,

    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = 100, message = "Order cannot contain more than 100 items")
    List<@Valid OrderItemRequest> items,

    @NotNull(message = "Shipping address is required")
    @Valid
    AddressRequest shippingAddress
) {}
