package com.oms.infrastructure.messaging.kafka.producer;

import com.oms.infrastructure.persistence.jpa.entity.OutboxJpaEntity;
import com.oms.infrastructure.persistence.jpa.repository.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox table for unprocessed events and publishes them to Kafka.
 * Runs on a fixed delay to ensure at-least-once delivery.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxJpaRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 100)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxJpaEntity> pending = outboxRepository
                .findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxJpaEntity entry : pending) {
            try {
                kafkaTemplate.send(
                        entry.getTopic(),
                        entry.getPartitionKey(),
                        entry.getPayload()
                ).get(5, TimeUnit.SECONDS);

                entry.setProcessed(true);
                outboxRepository.save(entry);

                log.debug("Published outbox entry {} to topic {}", entry.getId(), entry.getTopic());
            } catch (Exception e) {
                log.error("Failed to publish outbox entry {} to Kafka, will retry next poll",
                        entry.getId(), e);
                // Skip this entry; it will be retried on the next poll cycle
            }
        }
    }
}
