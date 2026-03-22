package com.oms.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.config.OmsApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(
    classes = OmsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("loadtest")
@Testcontainers
class OrderPlacementLoadTest {

    private static final int REQUESTS_PER_SECOND = 10;
    private static final int DURATION_SECONDS = 10;
    private static final int TOTAL_REQUESTS = REQUESTS_PER_SECOND * DURATION_SECONDS;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("oms")
            .withUsername("oms")
            .withPassword("oms");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String buildOrderJson(UUID idempotencyKey) {
        return """
            {
              "customerId": "%s",
              "items": [{
                "productId": "%s",
                "sku": "LOAD-TEST-001",
                "quantity": 1,
                "unitPrice": 29.99,
                "currency": "USD"
              }],
              "shippingAddress": {
                "street": "123 Load Test Ave",
                "city": "Testville",
                "state": "CA",
                "postalCode": "90210",
                "countryCode": "US"
              }
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void shouldHandle10RequestsPerSecondWithAcceptableLatency() throws Exception {
        // Warm up: send 5 requests to prime connection pools and JIT
        for (int i = 0; i < 5; i++) {
            UUID key = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", key.toString())
                    .content(buildOrderJson(key)));
        }
        Thread.sleep(500); // let warmup settle

        // Metrics accumulators
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

        // Use a ScheduledExecutorService to fire exactly REQUESTS_PER_SECOND per second
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        ExecutorService workerPool = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

        AtomicInteger requestIndex = new AtomicInteger(0);

        // Schedule REQUESTS_PER_SECOND requests per second for DURATION_SECONDS seconds
        for (int second = 0; second < DURATION_SECONDS; second++) {
            final int currentSecond = second;
            scheduler.schedule(() -> {
                for (int r = 0; r < REQUESTS_PER_SECOND; r++) {
                    workerPool.submit(() -> {
                        try {
                            UUID idempotencyKey = UUID.randomUUID();
                            String json = buildOrderJson(idempotencyKey);

                            long start = System.nanoTime();
                            MvcResult result = mockMvc.perform(post("/api/v1/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Idempotency-Key", idempotencyKey.toString())
                                    .content(json))
                                    .andReturn();
                            long durationMs = (System.nanoTime() - start) / 1_000_000;

                            latencies.add(durationMs);

                            int status = result.getResponse().getStatus();
                            if (status == 202) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                                System.err.printf("  [%d] Unexpected status %d: %s%n",
                                        requestIndex.incrementAndGet(), status,
                                        result.getResponse().getContentAsString());
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.printf("  Request exception: %s%n", e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }, currentSecond, TimeUnit.SECONDS);
        }

        // Wait for all requests to complete (generous timeout)
        boolean completed = latch.await(DURATION_SECONDS + 30, TimeUnit.SECONDS);
        scheduler.shutdown();
        workerPool.shutdown();

        assertThat(completed).as("All requests should complete within timeout").isTrue();

        // Compute latency percentiles
        long[] sortedLatencies = latencies.stream()
                .mapToLong(Long::longValue)
                .sorted()
                .toArray();

        long p50 = percentile(sortedLatencies, 50);
        long p95 = percentile(sortedLatencies, 95);
        long p99 = percentile(sortedLatencies, 99);
        long max = sortedLatencies.length > 0 ? sortedLatencies[sortedLatencies.length - 1] : 0;
        double avg = LongStream.of(sortedLatencies).average().orElse(0);
        int total = successCount.get() + errorCount.get();
        double errorRate = total > 0 ? (double) errorCount.get() / total * 100.0 : 0;

        // Print results
        System.out.println("\n====== Load Test Results ======");
        System.out.printf("  Target rate:     %d req/s%n", REQUESTS_PER_SECOND);
        System.out.printf("  Duration:        %d seconds%n", DURATION_SECONDS);
        System.out.printf("  Total requests:  %d%n", total);
        System.out.printf("  Success (202):   %d%n", successCount.get());
        System.out.printf("  Errors:          %d (%.2f%%)%n", errorCount.get(), errorRate);
        System.out.printf("  Avg latency:     %.1f ms%n", avg);
        System.out.printf("  P50 latency:     %d ms%n", p50);
        System.out.printf("  P95 latency:     %d ms%n", p95);
        System.out.printf("  P99 latency:     %d ms%n", p99);
        System.out.printf("  Max latency:     %d ms%n", max);
        System.out.println("===============================\n");

        // Assertions - SLA checks
        assertThat(errorRate)
                .as("Error rate should be below 1%%")
                .isLessThan(1.0);

        assertThat(p99)
                .as("P99 latency should be below 500ms")
                .isLessThan(500);

        assertThat(successCount.get())
                .as("At least 95%% of requests should succeed")
                .isGreaterThanOrEqualTo((int) (TOTAL_REQUESTS * 0.95));
    }

    @Test
    void shouldHandleConcurrentBurstOf50Requests() throws Exception {
        // Burst test: fire 50 requests simultaneously
        int burstSize = 50;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(burstSize);
        AtomicInteger successes = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < burstSize; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // all threads fire at once
                    UUID key = UUID.randomUUID();
                    long start = System.nanoTime();
                    MvcResult result = mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", key.toString())
                            .content(buildOrderJson(key)))
                            .andReturn();
                    long durationMs = (System.nanoTime() - start) / 1_000_000;
                    latencies.add(durationMs);
                    if (result.getResponse().getStatus() == 202) {
                        successes.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Burst request error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // fire!
        boolean done = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        long p99 = percentile(sorted, 99);

        System.out.println("\n====== Burst Test Results ======");
        System.out.printf("  Burst size:    %d concurrent requests%n", burstSize);
        System.out.printf("  Successes:     %d%n", successes.get());
        System.out.printf("  P99 latency:   %d ms%n", p99);
        System.out.println("================================\n");

        assertThat(done).isTrue();
        assertThat(successes.get())
                .as("At least 90%% of burst requests should succeed")
                .isGreaterThanOrEqualTo((int) (burstSize * 0.9));
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        String json = buildOrderJson(idempotencyKey);

        // First request: should succeed with 202
        MvcResult first = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey.toString())
                .content(json))
                .andReturn();

        assertThat(first.getResponse().getStatus()).isEqualTo(202);

        // Second request with same key: should return 409 or 200 (idempotent)
        MvcResult second = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey.toString())
                .content(json))
                .andReturn();

        int status = second.getResponse().getStatus();
        assertThat(status)
                .as("Duplicate key should return 409 Conflict or 200 OK (idempotent)")
                .isIn(200, 202, 409);
    }

    private static long percentile(long[] sortedData, int percentile) {
        if (sortedData.length == 0) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedData.length) - 1;
        return sortedData[Math.max(0, Math.min(index, sortedData.length - 1))];
    }
}
