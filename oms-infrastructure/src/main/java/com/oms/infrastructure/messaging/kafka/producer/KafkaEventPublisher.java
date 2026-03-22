package com.oms.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.domain.event.DomainEvent;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.infrastructure.messaging.kafka.schema.KafkaTopics;
import com.oms.infrastructure.messaging.kafka.schema.OrderEventEnvelope;
import com.oms.infrastructure.persistence.jpa.entity.OutboxJpaEntity;
import com.oms.infrastructure.persistence.jpa.repository.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(OutboxJpaRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            try {
                String orderId = event.orderId().value().toString();
                String eventType = event.eventType();

                OrderEventEnvelope envelope = new OrderEventEnvelope(
                        UUID.randomUUID().toString(),
                        eventType,
                        event.schemaVersion(),
                        orderId,
                        event.occurredAt(),
                        objectMapper.writeValueAsString(event)
                );

                String payload = objectMapper.writeValueAsString(envelope);

                OutboxJpaEntity outboxEntry = OutboxJpaEntity.create(
                        KafkaTopics.ORDER_EVENTS,
                        orderId,
                        payload
                );

                outboxRepository.save(outboxEntry);
                log.debug("Persisted outbox entry for event {} on order {}", eventType, orderId);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize domain event: {}", event, e);
                throw new IllegalStateException("Failed to serialize domain event", e);
            }
        }
    }
}
