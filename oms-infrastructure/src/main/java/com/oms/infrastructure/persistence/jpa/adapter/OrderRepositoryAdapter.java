package com.oms.infrastructure.persistence.jpa.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.*;
import com.oms.domain.port.outbound.OrderRepository;
import com.oms.infrastructure.persistence.jpa.entity.OrderItemJpaEntity;
import com.oms.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import com.oms.infrastructure.persistence.jpa.entity.OrderStatusEnum;
import com.oms.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(OrderRepositoryAdapter.class);

    private final OrderJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Order save(Order order) {
        try {
            OrderJpaEntity entity = toJpaEntity(order);
            OrderJpaEntity saved = jpaRepository.save(entity);
            return toDomainOrder(saved);
        } catch (OptimisticLockException e) {
            log.error("Optimistic lock conflict saving order {}", order.id(), e);
            throw new IllegalStateException(
                    "Order was modified by another transaction: " + order.id(), e);
        }
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value())
                .map(this::toDomainOrder);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId.value(), PageRequest.of(0, 50))
                .stream()
                .map(this::toDomainOrder)
                .toList();
    }

    private OrderJpaEntity toJpaEntity(Order order) {
        List<OrderItemJpaEntity> itemEntities = order.items().stream()
                .map(this::toJpaItemEntity)
                .toList();

        String addressJson = null;
        if (order.shippingAddress() != null) {
            try {
                addressJson = objectMapper.writeValueAsString(order.shippingAddress());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize shipping address", e);
            }
        }

        return new OrderJpaEntity(
                order.id().value(),
                order.version() == 0 ? null : order.version(),
                order.customerId().value(),
                OrderStatusEnum.valueOf(order.status().name()),
                order.total().currency().getCurrencyCode(),
                order.subtotal().amount(),
                order.tax().amount(),
                order.shipping().amount(),
                order.total().amount(),
                addressJson,
                order.idempotencyKey(),
                itemEntities,
                order.createdAt(),
                order.updatedAt()
        );
    }

    private OrderItemJpaEntity toJpaItemEntity(OrderItem item) {
        return new OrderItemJpaEntity(
                UUID.randomUUID(),
                item.productId().value(),
                item.sku(),
                item.quantity(),
                item.unitPrice().amount(),
                item.totalPrice().amount()
        );
    }

    private Order toDomainOrder(OrderJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrencyCode());

        List<OrderItem> items = entity.getItems().stream()
                .map(ie -> new OrderItem(
                        new ProductId(ie.getProductId()),
                        ie.getSku(),
                        ie.getQuantity(),
                        new Money(ie.getUnitPrice(), currency)
                ))
                .toList();

        Address address = null;
        if (entity.getShippingAddressJson() != null) {
            try {
                address = objectMapper.readValue(entity.getShippingAddressJson(), Address.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize shipping address", e);
            }
        }

        return Order.reconstitute(
                new OrderId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                OrderStatus.valueOf(entity.getStatus().name()),
                items,
                new Money(entity.getSubtotalAmount(), currency),
                new Money(entity.getTaxAmount(), currency),
                new Money(entity.getShippingAmount(), currency),
                new Money(entity.getTotalAmount(), currency),
                address,
                entity.getIdempotencyKey(),
                entity.getVersion() != null ? entity.getVersion() : 0L,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
