package com.oms.infrastructure.messaging.kafka.consumer;

import com.oms.infrastructure.messaging.kafka.schema.KafkaTopics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes messages from the dead letter queue topic for monitoring and alerting.
 */
@Component
@org.springframework.context.annotation.Profile("!loadtest")
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final Counter deadLetterCounter;

    public DeadLetterConsumer(MeterRegistry meterRegistry) {
        this.deadLetterCounter = Counter.builder("oms.dead_letter.received.total")
                .description("Total number of dead letter messages received")
                .tag("topic", KafkaTopics.ORDER_EVENTS_DLQ)
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS_DLQ,
            groupId = "oms-dead-letter-consumer",
            autoStartup = "${oms.kafka.dlq.auto-start:true}"
    )
    public void handleDeadLetter(ConsumerRecord<String, String> record) {
        log.error("Dead letter received: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value());

        deadLetterCounter.increment();
    }
}
