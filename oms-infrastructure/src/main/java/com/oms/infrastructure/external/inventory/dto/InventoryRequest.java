package com.oms.infrastructure.external.inventory.dto;

import java.util.List;

public record InventoryRequest(
        String orderId,
        List<InventoryLineItem> items
) {

    public record InventoryLineItem(
            String productId,
            int quantity
    ) {
    }
}
