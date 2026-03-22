package com.oms.application.usecase;

import com.oms.application.saga.SagaOrchestrator;
import com.oms.domain.command.OrderItemCommand;
import com.oms.domain.command.PlaceOrderCommand;
import com.oms.domain.model.*;
import com.oms.domain.port.outbound.EventPublisher;
import com.oms.domain.port.outbound.IdempotencyStore;
import com.oms.domain.port.outbound.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Concurrent load test for PlaceOrderUseCase.
 * Fires 10 req/s for 10 seconds (100 total) against the use case
 * with simulated I/O latency in the mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class PlaceOrderConcurrentLoadTest {

    private static final int REQUESTS_PER_SECOND = 50;
    private static final int DURATION_SECONDS = 10;
    private static final int TOTAL_REQUESTS = REQUESTS_PER_SECOND * DURATION_SECONDS;

    @Mock private OrderRepository orderRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private IdempotencyStore idempotencyStore;
    @Mock private SagaOrchestrator sagaOrchestrator;

    private PlaceOrderUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PlaceOrderUseCaseImpl(
                orderRepository, eventPublisher, idempotencyStore, sagaOrchestrator);

        // Simulate realistic I/O latency
        lenient().when(idempotencyStore.get(any())).thenAnswer(inv -> {
            Thread.sleep(2); // 2ms Redis lookup
            return Optional.empty();
        });
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Thread.sleep(5); // 5ms DB write
            return inv.getArgument(0);
        });
        lenient().doAnswer(inv -> {
            Thread.sleep(1); // 1ms event publish
            return null;
        }).when(eventPublisher).publish(any());
        lenient().doAnswer(inv -> {
            Thread.sleep(1); // 1ms saga dispatch (async, should return fast)
            return null;
        }).when(sagaOrchestrator).start(any(), any());
        lenient().doAnswer(inv -> {
            Thread.sleep(2); // 2ms Redis write
            return null;
        }).when(idempotencyStore).set(any(), any(), any(Duration.class));
    }

    @Test
    void shouldSustain10RequestsPerSecondFor10Seconds() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        Set<UUID> seenOrderIds = ConcurrentHashMap.newKeySet();

        // Warm up
        for (int i = 0; i < 5; i++) {
            useCase.execute(createCommand());
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        ExecutorService workerPool = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

        long testStart = System.nanoTime();

        for (int second = 0; second < DURATION_SECONDS; second++) {
            final int currentSecond = second;
            scheduler.schedule(() -> {
                for (int r = 0; r < REQUESTS_PER_SECOND; r++) {
                    workerPool.submit(() -> {
                        try {
                            PlaceOrderCommand cmd = createCommand();
                            long start = System.nanoTime();
                            OrderId orderId = useCase.execute(cmd);
                            long durationMs = (System.nanoTime() - start) / 1_000_000;

                            latencies.add(durationMs);
                            seenOrderIds.add(orderId.value());
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Request error: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }, currentSecond, TimeUnit.SECONDS);
        }

        boolean completed = latch.await(DURATION_SECONDS + 30, TimeUnit.SECONDS);
        long testDurationMs = (System.nanoTime() - testStart) / 1_000_000;
        scheduler.shutdown();
        workerPool.shutdown();

        assertThat(completed).as("All requests should complete within timeout").isTrue();

        // Compute latency stats
        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        long p50 = percentile(sorted, 50);
        long p95 = percentile(sorted, 95);
        long p99 = percentile(sorted, 99);
        long max = sorted.length > 0 ? sorted[sorted.length - 1] : 0;
        double avg = LongStream.of(sorted).average().orElse(0);
        int total = successCount.get() + errorCount.get();
        double errorRate = total > 0 ? (double) errorCount.get() / total * 100.0 : 0;
        double actualRps = (double) total / testDurationMs * 1000.0;

        // Print results
        System.out.println();
        System.out.println("=".repeat(50));
        System.out.println("  LOAD TEST RESULTS: PlaceOrderUseCase");
        System.out.println("=".repeat(50));
        System.out.printf("  Target rate:     %d req/s%n", REQUESTS_PER_SECOND);
        System.out.printf("  Duration:        %d seconds%n", DURATION_SECONDS);
        System.out.printf("  Total requests:  %d%n", total);
        System.out.printf("  Actual RPS:      %.1f req/s%n", actualRps);
        System.out.printf("  Successes:       %d%n", successCount.get());
        System.out.printf("  Errors:          %d (%.2f%%)%n", errorCount.get(), errorRate);
        System.out.printf("  Unique orders:   %d%n", seenOrderIds.size());
        System.out.println("  ---- Latency ----");
        System.out.printf("  Avg:             %.1f ms%n", avg);
        System.out.printf("  P50:             %d ms%n", p50);
        System.out.printf("  P95:             %d ms%n", p95);
        System.out.printf("  P99:             %d ms%n", p99);
        System.out.printf("  Max:             %d ms%n", max);
        System.out.println("=".repeat(50));
        System.out.println();

        // SLA assertions
        assertThat(errorRate)
                .as("Error rate should be below 1%%")
                .isLessThan(1.0);

        assertThat(p99)
                .as("P99 latency should be below 200ms (with simulated I/O ~11ms per request)")
                .isLessThan(200);

        assertThat(successCount.get())
                .as("All %d requests should succeed", TOTAL_REQUESTS)
                .isEqualTo(TOTAL_REQUESTS);

        assertThat(seenOrderIds)
                .as("Each request should produce a unique order")
                .hasSize(TOTAL_REQUESTS);

        // Verify thread safety: all orders were persisted
        verify(orderRepository, times(TOTAL_REQUESTS + 5 /* warmup */)).save(any(Order.class));
    }

    @Test
    void shouldHandleConcurrentBurstOf50Requests() throws Exception {
        int burstSize = 50;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(burstSize);
        AtomicInteger successes = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < burstSize; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    PlaceOrderCommand cmd = createCommand();
                    long start = System.nanoTime();
                    useCase.execute(cmd);
                    long durationMs = (System.nanoTime() - start) / 1_000_000;
                    latencies.add(durationMs);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Burst error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // fire all at once
        boolean done = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        long p99 = percentile(sorted, 99);

        System.out.println();
        System.out.println("=".repeat(50));
        System.out.println("  BURST TEST: 50 concurrent requests");
        System.out.println("=".repeat(50));
        System.out.printf("  Successes:   %d / %d%n", successes.get(), burstSize);
        System.out.printf("  P99 latency: %d ms%n", p99);
        System.out.println("=".repeat(50));
        System.out.println();

        assertThat(done).isTrue();
        assertThat(successes.get()).isEqualTo(burstSize);
        assertThat(p99).as("P99 under burst should be < 500ms").isLessThan(500);
    }

    @Test
    void shouldDeduplicateUnderConcurrentIdempotencyKeyCollisions() throws Exception {
        UUID sharedKey = UUID.randomUUID();

        // First call: cache miss -> creates order
        lenient().when(idempotencyStore.get(sharedKey)).thenReturn(Optional.empty());

        PlaceOrderCommand cmd = createCommandWithKey(sharedKey);
        OrderId firstResult = useCase.execute(cmd);

        // Subsequent calls: cache hit -> returns cached order
        lenient().when(idempotencyStore.get(sharedKey))
                .thenReturn(Optional.of(firstResult.value().toString()));

        int duplicateCount = 20;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(duplicateCount);
        Set<UUID> results = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < duplicateCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    OrderId id = useCase.execute(createCommandWithKey(sharedKey));
                    results.add(id.value());
                } catch (Exception e) {
                    System.err.println("Idempotency error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println();
        System.out.println("=".repeat(50));
        System.out.println("  IDEMPOTENCY TEST: 20 concurrent duplicates");
        System.out.println("=".repeat(50));
        System.out.printf("  Unique order IDs returned: %d (expected: 1)%n", results.size());
        System.out.println("=".repeat(50));
        System.out.println();

        assertThat(results)
                .as("All duplicate requests should return the same OrderId")
                .hasSize(1)
                .containsExactly(firstResult.value());

        // save() should only be called once (+ warmup from the sustained test may interfere,
        // but in this test context it's isolated)
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    private PlaceOrderCommand createCommand() {
        return createCommandWithKey(UUID.randomUUID());
    }

    private PlaceOrderCommand createCommandWithKey(UUID idempotencyKey) {
        Currency usd = Currency.getInstance("USD");
        return new PlaceOrderCommand(
                CustomerId.generate(),
                List.of(new OrderItemCommand(
                        ProductId.generate(),
                        "LOAD-TEST-001",
                        1,
                        new Money(new BigDecimal("29.99"), usd)
                )),
                new Address("123 Load Test Ave", "Testville", "CA", "90210", "US"),
                idempotencyKey
        );
    }

    private static long percentile(long[] sortedData, int percentile) {
        if (sortedData.length == 0) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedData.length) - 1;
        return sortedData[Math.max(0, Math.min(index, sortedData.length - 1))];
    }
}
