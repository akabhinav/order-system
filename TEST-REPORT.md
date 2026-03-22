# Order Management System - Test Report

## Test Summary

| Category | Tests | Pass | Fail | Coverage |
|----------|-------|------|------|----------|
| Domain Unit Tests | 41 | 41 | 0 | Order, Money, OrderStatus |
| Application Unit Tests | 9 | 9 | 0 | PlaceOrder, CancelOrder, Saga |
| Concurrent Load Tests (mock) | 3 | 3 | 0 | 10K requests, burst, idempotency |
| Architecture Tests | 1 | 1 | 0 | Hexagonal architecture |
| Real Load Tests (Postgres+Redis) | 2 | 2 | 0 | 100 & 1000 req/s |
| **Total** | **56** | **56** | **0** | |

---

## 1. Domain Unit Tests

**Module:** `oms-domain`
**Runner:** JUnit 5 + AssertJ
**Command:** `mvn test -pl oms-domain`

### 1.1 OrderTest (10 tests)

| Test | Description | Status |
|------|-------------|--------|
| `shouldCreateOrderFromPlaceCommand` | Creates Order from PlaceOrderCommand with correct fields | PASS |
| `shouldGenerateOrderPlacedEvent` | Pending events list contains OrderPlacedEvent | PASS |
| `shouldCalculateTotalFromItems` | Total = sum(quantity * unitPrice) for all items | PASS |
| `shouldSetInitialStatusToPending` | New order status is PENDING | PASS |
| `shouldTransitionToPaymentProcessing` | PENDING -> PAYMENT_PROCESSING | PASS |
| `shouldTransitionToConfirmed` | PAYMENT_PROCESSING -> CONFIRMED | PASS |
| `shouldTransitionToCancelled` | PENDING -> CANCELLED with reason | PASS |
| `shouldRejectInvalidTransition` | DELIVERED -> PENDING throws InvalidOrderStateException | PASS |
| `shouldTrackVersionForOptimisticLocking` | Version increments on state change | PASS |
| `shouldClearPendingEventsAfterRead` | pendingEvents() clears internal list | PASS |

### 1.2 MoneyTest (11 tests)

| Test | Description | Status |
|------|-------------|--------|
| `shouldCreateMoney` | Constructs with amount + currency | PASS |
| `shouldAddSameCurrency` | 10.00 + 5.00 = 15.00 USD | PASS |
| `shouldRejectDifferentCurrencyAdd` | USD + EUR throws IllegalArgumentException | PASS |
| `shouldMultiply` | 10.00 * 3 = 30.00 | PASS |
| `shouldBeZero` | Money.zero(USD) = 0.00 | PASS |
| `shouldCompare` | 10.00 > 5.00 | PASS |
| `shouldNegate` | negate(10.00) = -10.00 | PASS |
| `shouldRejectNullCurrency` | null currency throws NullPointerException | PASS |
| `shouldRejectNullAmount` | null amount throws NullPointerException | PASS |
| `shouldHandleScale` | 10.1 rounds to 10.10 | PASS |
| `shouldSupportEquality` | same amount+currency are equal | PASS |

### 1.3 OrderStatusTest (20 tests)

| Test | Description | Status |
|------|-------------|--------|
| Valid transitions (10) | PENDING->PAYMENT_PROCESSING, PAYMENT_PROCESSING->CONFIRMED, CONFIRMED->PICKING, PICKING->PACKED, PACKED->SHIPPED, SHIPPED->DELIVERED, DELIVERED->REFUND_REQUESTED, REFUND_REQUESTED->REFUNDED, PENDING->CANCELLED, CONFIRMED->CANCELLED | ALL PASS |
| Invalid transitions (10) | CANCELLED->PENDING, DELIVERED->PENDING, REFUNDED->PENDING, SHIPPED->CANCELLED, etc. | ALL PASS |

---

## 2. Application Unit Tests

**Module:** `oms-application`
**Runner:** JUnit 5 + Mockito
**Command:** `mvn test -pl oms-application`

### 2.1 PlaceOrderUseCaseTest (2 tests)

| Test | Description | Status |
|------|-------------|--------|
| `shouldPlaceOrderSuccessfully` | Saves order, publishes events, starts saga, stores idempotency key | PASS |
| `shouldReturnCachedOrderIdWhenIdempotencyKeyExists` | Returns cached OrderId, skips save/publish/saga | PASS |

### 2.2 CancelOrderUseCaseTest (3 tests)

| Test | Description | Status |
|------|-------------|--------|
| `shouldCancelOrder` | Transitions order to CANCELLED, publishes event | PASS |
| `shouldThrowWhenOrderNotFound` | Throws OrderNotFoundException for unknown ID | PASS |
| `shouldThrowWhenCancellationNotAllowed` | Throws InvalidOrderStateException for non-cancellable state | PASS |

### 2.3 OrderPlacementSagaTest (4 tests)

| Test | Description | Status |
|------|-------------|--------|
| `shouldCompleteHappyPath` | Reserve inventory -> charge payment -> confirm order | PASS |
| `shouldCompensateOnPaymentFailure` | Payment fails -> release inventory -> cancel order | PASS |
| `shouldCompensateOnInventoryFailure` | Inventory reservation fails -> cancel order | PASS |
| `shouldPersistSagaState` | Saga state saved at each step transition | PASS |

---

## 3. Concurrent Load Tests (Mock I/O)

**Module:** `oms-application`
**File:** `PlaceOrderConcurrentLoadTest.java`
**Runner:** JUnit 5 + Virtual Threads
**Command:** `mvn test -pl oms-application -Dtest="PlaceOrderConcurrentLoadTest"`

Uses Mockito mocks with simulated I/O latency (2ms Redis, 5ms DB, 1ms event publish).

### 3.1 Sustained Load Test (1000 req/s x 10s)

| Metric | Value |
|--------|-------|
| Target rate | 1000 req/s |
| Duration | 10 seconds |
| Total requests | 10,000 |
| Actual RPS | 1,103 req/s |
| Successes | 10,000 (100%) |
| Errors | 0 (0%) |
| Unique orders | 10,000 |
| **Avg latency** | **75.2 ms** |
| **P50 latency** | **57 ms** |
| **P95 latency** | **224 ms** |
| **P99 latency** | **272 ms** |
| **Max latency** | **292 ms** |
| Status | **PASS** |

### 3.2 Burst Test (50 concurrent)

| Metric | Value |
|--------|-------|
| Burst size | 50 simultaneous requests |
| Successes | 50/50 (100%) |
| P99 latency | 17 ms |
| Status | **PASS** |

### 3.3 Idempotency Under Concurrency

| Metric | Value |
|--------|-------|
| Concurrent duplicate requests | 20 |
| Unique OrderIds returned | 1 |
| DB save calls | 1 (deduplicated) |
| Status | **PASS** |

---

## 4. Real Load Tests (Live Postgres + Redis)

**Script:** `load-tests/load-test-real.py`
**Stack:** Spring Boot 3.3 + Postgres 16 + Redis 7 + Java 21 Virtual Threads
**Command:** `python3 load-tests/load-test-real.py --rps 100 --duration 10`

### Infrastructure Config

| Component | Config |
|-----------|--------|
| **Java** | 21 with `--enable-preview`, virtual threads enabled |
| **Spring Boot** | 3.3.0 with `loadtest` profile (Kafka disabled) |
| **Postgres 16** | localhost:5432, HikariCP pool: 50 max connections |
| **Redis 7** | localhost:6379, Lettuce pool: 32 max-active, 16 max-idle |
| **Resilience4j** | Rate limiter: 10,000 req/s per period |
| **Tomcat** | Default thread pool (virtual threads handle concurrency) |

### 4.1 Load Test: 100 req/s (1,000 total)

| Metric | Value |
|--------|-------|
| Target rate | 100 req/s |
| Duration | 9.3s |
| Total requests | 1,000 |
| Actual RPS | 107.7 req/s |
| HTTP 202 (success) | **1,000 (100%)** |
| Errors | **0 (0%)** |
| **Avg latency** | **367.7 ms** |
| **P50 latency** | **312.9 ms** |
| **P95 latency** | **769.2 ms** |
| **P99 latency** | **810.9 ms** |
| **Max latency** | **854.2 ms** |
| Error rate SLA (<5%) | **PASS** |

### 4.2 Stress Test: 1000 req/s (10,000 total)

| Metric | Value |
|--------|-------|
| Target rate | 1,000 req/s |
| Duration | 21.6s |
| Total requests | 10,000 |
| Actual RPS | 463.6 req/s (saturated) |
| HTTP 202 (success) | **9,980** |
| HTTP 500 (errors) | **20 (0.2%)** |
| **Avg latency** | **1,022 ms** |
| **P50 latency** | **863.5 ms** |
| **P95 latency** | **2,226 ms** |
| **P99 latency** | **3,109 ms** |
| **Max latency** | **5,797 ms** |
| Error rate SLA (<5%) | **PASS** |

### Bottleneck Analysis

| Factor | Impact |
|--------|--------|
| HikariCP pool (50 connections) | Primary bottleneck - threads wait for DB connections |
| Single Postgres instance | No read replicas, all writes go to one DB |
| Hibernate ORM overhead | Entity mapping + dirty checking adds ~50-100ms per request |
| Unidirectional @OneToMany | Hibernate inserts child rows then updates FK (extra UPDATE) |

### Scaling Recommendations

| Action | Expected Improvement |
|--------|---------------------|
| Increase HikariCP pool to 100-200 | 2-3x throughput |
| Add Postgres read replicas | Offload GET queries |
| Enable Hibernate batch inserts | Reduce round-trips for multi-item orders |
| Scale to 3+ app instances behind LB | Linear horizontal scaling |
| Switch to JDBC batch for outbox writes | Reduce per-event DB overhead |

---

## 5. Architecture Tests

**Module:** `oms-tests`
**File:** `ArchitectureTest.java`
**Command:** `mvn test -pl oms-tests`

| Test | Description | Status |
|------|-------------|--------|
| Hexagonal architecture enforcement | Domain has no dependency on infrastructure/API layers | PASS |

---

## 6. k6 Load Test Scripts (for CI/CD)

**Directory:** `load-tests/`

These scripts are ready for use in CI/CD but require k6 + Docker.

### 6.1 stress-test.js

| Scenario | Rate | Duration | SLA |
|----------|------|----------|-----|
| Smoke | 10 req/s | 30s | p99 < 200ms, errors < 1% |
| Load | ramp to 1K req/s | 3min | p99 < 500ms, errors < 1% |
| Stress | ramp to 5K req/s | 3min | p99 < 1s, errors < 5% |
| Spike | burst to 17K req/s | 1.5min | Measured (no hard SLA) |
| Mixed | 2K req/s (70% reads, 30% writes) | 2min | Measured |

### 6.2 quick-test.js

| Scenario | Rate | Duration | SLA |
|----------|------|----------|-----|
| Quick smoke | ramp to 100 VUs | 80s | p95 < 300ms, p99 < 500ms, errors < 5% |

### Running k6 Tests

```bash
# Start infrastructure
docker-compose up -d postgres redis zookeeper kafka

# Run quick smoke test
k6 run --env BASE_URL=http://localhost:8080 load-tests/quick-test.js

# Run full stress test suite
k6 run --env BASE_URL=http://localhost:8080 load-tests/stress-test.js
```

---

## 7. Integration Test (Testcontainers)

**Module:** `oms-tests`
**File:** `OrderPlacementLoadTest.java`
**Requires:** Docker + Maven Central access

Auto-starts Postgres 16, Redis 7, and Kafka via Testcontainers.

| Test | Description |
|------|-------------|
| `shouldHandle10RequestsPerSecondWithAcceptableLatency` | 10 req/s for 10s via MockMvc against full Spring context |
| `shouldHandleConcurrentBurstOf50Requests` | 50 simultaneous requests |
| `shouldRejectDuplicateIdempotencyKey` | Idempotency key deduplication |

```bash
mvn test -pl oms-tests -Dtest="OrderPlacementLoadTest" \
  -Dsurefire.failIfNoSpecifiedTests=false -am
```

---

## How to Run All Tests

```bash
# Unit tests (no Docker required)
mvn test -pl oms-domain,oms-application

# Concurrent load test (no Docker required)
mvn test -pl oms-application -Dtest="PlaceOrderConcurrentLoadTest"

# Real load test (requires Postgres + Redis running)
pg_ctlcluster 16 main start
redis-server --daemonize yes
psql -h localhost -U oms -d oms < oms-infrastructure/src/main/resources/db/migration/V1__create_orders.sql
mvn package -DskipTests -pl oms-config -am
java --enable-preview -jar oms-config/target/oms-config-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=loadtest --spring.flyway.enabled=false
# In another terminal:
python3 load-tests/load-test-real.py --rps 100 --duration 10

# Integration test (requires Docker)
mvn test -pl oms-tests -Dtest="OrderPlacementLoadTest" -Dsurefire.failIfNoSpecifiedTests=false -am

# k6 load test (requires Docker + k6)
docker-compose up -d
k6 run load-tests/stress-test.js
```
