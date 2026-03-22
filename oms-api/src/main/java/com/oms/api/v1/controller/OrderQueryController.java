package com.oms.api.v1.controller;

import com.oms.api.v1.dto.response.OrderSummaryResponse;
import com.oms.api.v1.dto.response.PagedResponse;
import com.oms.api.v1.mapper.OrderApiMapper;
import com.oms.domain.model.CustomerId;
import com.oms.domain.model.Order;
import com.oms.domain.port.inbound.GetOrderUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class OrderQueryController {

    private final GetOrderUseCase getOrderUseCase;

    public OrderQueryController(GetOrderUseCase getOrderUseCase) {
        this.getOrderUseCase = getOrderUseCase;
    }

    @GetMapping("/{customerId}/orders")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getCustomerOrders(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {

        List<Order> orders = getOrderUseCase.findByCustomerId(new CustomerId(customerId));

        List<OrderSummaryResponse> summaries = orders.stream()
                .limit(limit)
                .map(OrderApiMapper::toSummary)
                .toList();

        String nextCursor = summaries.size() >= limit && !orders.isEmpty()
                ? orders.getLast().id().value().toString()
                : null;

        PagedResponse<OrderSummaryResponse> response =
                new PagedResponse<>(summaries, nextCursor, limit);

        return ResponseEntity.ok(response);
    }
}
