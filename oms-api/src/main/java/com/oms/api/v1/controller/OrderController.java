package com.oms.api.v1.controller;

import com.oms.api.v1.dto.request.CancelOrderRequest;
import com.oms.api.v1.dto.request.PlaceOrderRequest;
import com.oms.api.v1.dto.response.OrderResponse;
import com.oms.api.v1.mapper.OrderApiMapper;
import com.oms.domain.command.CancelOrderCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.exception.OrderNotFoundException;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderId;
import com.oms.domain.port.inbound.CancelOrderUseCase;
import com.oms.domain.port.inbound.GetOrderUseCase;
import com.oms.domain.port.inbound.PlaceOrderUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           GetOrderUseCase getOrderUseCase,
                           CancelOrderUseCase cancelOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    @PostMapping
    @RateLimiter(name = "place-order")
    public ResponseEntity<Void> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey) {

        PlaceOrderCommand command = OrderApiMapper.toCommand(request, idempotencyKey);
        OrderId orderId = placeOrderUseCase.execute(command);

        URI location = URI.create("/api/v1/orders/" + orderId.value());
        return ResponseEntity.accepted().location(location).build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        OrderId id = new OrderId(orderId);
        Order order = getOrderUseCase.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return ResponseEntity.ok(OrderApiMapper.toResponse(order));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        OrderId id = new OrderId(orderId);
        CancelOrderCommand command = new CancelOrderCommand(id, request.reason(), null);
        cancelOrderUseCase.execute(command);

        Order order = getOrderUseCase.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return ResponseEntity.ok(OrderApiMapper.toResponse(order));
    }
}
