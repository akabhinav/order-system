package com.oms.infrastructure.messaging.kafka.schema;

/**
 * Central registry of Kafka topic names used across the order management system.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // constants class
    }

    public static final String ORDER_COMMANDS = "order.commands";
    public static final String ORDER_EVENTS = "order.events";
    public static final String ORDER_EVENTS_DLQ = "order.events.dlq";
    public static final String INVENTORY_COMMANDS = "inventory.commands";
    public static final String PAYMENT_COMMANDS = "payment.commands";
    public static final String NOTIFICATION_EVENTS = "notification.events";
    public static final String SAGA_REPLY = "saga.reply";
}
