package com.oms.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for the Order Management System.
 * Registers common tags and custom business metrics.
 */
@Configuration
public class MetricsConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags("service", "order-service");
    }

    @Bean
    public MeterBinder orderMetrics() {
        return registry -> {
            Counter.builder("oms.orders.placed.total")
                    .description("Total number of orders placed")
                    .register(registry);

            Counter.builder("oms.orders.confirmed.total")
                    .description("Total number of orders confirmed")
                    .register(registry);

            Counter.builder("oms.orders.cancelled.total")
                    .description("Total number of orders cancelled")
                    .register(registry);

            Counter.builder("oms.orders.failed.total")
                    .description("Total number of order failures")
                    .register(registry);

            Timer.builder("oms.orders.processing.duration")
                    .description("Time taken to process an order")
                    .register(registry);

            Timer.builder("oms.payment.charge.duration")
                    .description("Time taken to charge payment")
                    .register(registry);

            Timer.builder("oms.inventory.reserve.duration")
                    .description("Time taken to reserve inventory")
                    .register(registry);
        };
    }
}
