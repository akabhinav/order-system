import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Custom metrics
const orderPlacedCounter = new Counter('orders_placed');
const orderErrorRate = new Rate('order_errors');
const orderDuration = new Trend('order_placement_duration', true);

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test stages - ramp up to 17K req/s target
export const options = {
    scenarios: {
        // Scenario 1: Smoke test (sanity check)
        smoke: {
            executor: 'constant-arrival-rate',
            rate: 10,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 20,
            maxVUs: 50,
            startTime: '0s',
            tags: { scenario: 'smoke' },
        },
        // Scenario 2: Ramp to 1K req/s
        load: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            stages: [
                { duration: '30s', target: 500 },
                { duration: '1m', target: 1000 },
                { duration: '1m', target: 1000 },
                { duration: '30s', target: 0 },
            ],
            preAllocatedVUs: 500,
            maxVUs: 2000,
            startTime: '30s',
            tags: { scenario: 'load' },
        },
        // Scenario 3: Stress test - push to 5K req/s
        stress: {
            executor: 'ramping-arrival-rate',
            startRate: 1000,
            timeUnit: '1s',
            stages: [
                { duration: '30s', target: 3000 },
                { duration: '1m', target: 5000 },
                { duration: '1m', target: 5000 },
                { duration: '30s', target: 0 },
            ],
            preAllocatedVUs: 2000,
            maxVUs: 10000,
            startTime: '3m30s',
            tags: { scenario: 'stress' },
        },
        // Scenario 4: Spike test - burst to 17K req/s
        spike: {
            executor: 'ramping-arrival-rate',
            startRate: 1000,
            timeUnit: '1s',
            stages: [
                { duration: '10s', target: 10000 },
                { duration: '30s', target: 17000 },
                { duration: '30s', target: 17000 },
                { duration: '10s', target: 1000 },
            ],
            preAllocatedVUs: 5000,
            maxVUs: 30000,
            startTime: '6m30s',
            tags: { scenario: 'spike' },
        },
        // Scenario 5: Read-heavy mix (70% reads, 30% writes)
        mixed: {
            executor: 'constant-arrival-rate',
            rate: 2000,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 1000,
            maxVUs: 5000,
            startTime: '8m',
            exec: 'mixedWorkload',
            tags: { scenario: 'mixed' },
        },
    },
    thresholds: {
        // SLA: p99 < 200ms for order placement
        'order_placement_duration{scenario:smoke}': ['p(99)<200'],
        'order_placement_duration{scenario:load}': ['p(99)<500'],
        'order_placement_duration{scenario:stress}': ['p(99)<1000'],

        // Error rate < 1% under normal load
        'order_errors{scenario:smoke}': ['rate<0.01'],
        'order_errors{scenario:load}': ['rate<0.01'],

        // Error rate < 5% under stress
        'order_errors{scenario:stress}': ['rate<0.05'],

        // HTTP request duration
        'http_req_duration{scenario:smoke}': ['p(95)<100', 'p(99)<200'],
        'http_req_duration{scenario:load}': ['p(95)<200', 'p(99)<500'],
    },
};

// Generate a random order payload
function generateOrder() {
    const numItems = Math.floor(Math.random() * 3) + 1;
    const items = [];
    for (let i = 0; i < numItems; i++) {
        items.push({
            productId: uuidv4(),
            sku: `SKU-${randomString(6).toUpperCase()}`,
            quantity: Math.floor(Math.random() * 5) + 1,
            unitPrice: (Math.random() * 100 + 1).toFixed(2),
            currency: 'USD',
        });
    }

    return {
        customerId: uuidv4(),
        items: items,
        shippingAddress: {
            street: `${Math.floor(Math.random() * 9999) + 1} Main St`,
            city: 'Springfield',
            state: 'IL',
            postalCode: '62701',
            countryCode: 'US',
        },
    };
}

// Default function: place an order
export default function () {
    const idempotencyKey = uuidv4();
    const payload = JSON.stringify(generateOrder());

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': idempotencyKey,
            'X-Request-Id': uuidv4(),
        },
        tags: { name: 'PlaceOrder' },
    };

    const startTime = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
    const duration = Date.now() - startTime;

    orderDuration.add(duration);

    const success = check(res, {
        'status is 202': (r) => r.status === 202,
        'has Location header': (r) => r.headers['Location'] !== undefined,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    if (success) {
        orderPlacedCounter.add(1);
        orderErrorRate.add(0);
    } else {
        orderErrorRate.add(1);
    }
}

// Mixed workload: reads + writes
export function mixedWorkload() {
    if (Math.random() < 0.3) {
        // 30% writes
        const idempotencyKey = uuidv4();
        const payload = JSON.stringify(generateOrder());
        const params = {
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': idempotencyKey,
            },
            tags: { name: 'PlaceOrder' },
        };
        const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
        check(res, { 'write status 2xx': (r) => r.status >= 200 && r.status < 300 });
    } else {
        // 70% reads - get a random order (will likely 404, but tests read path)
        const orderId = uuidv4();
        const params = { tags: { name: 'GetOrder' } };
        const res = http.get(`${BASE_URL}/api/v1/orders/${orderId}`, params);
        check(res, {
            'read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        });
    }
}

// Idempotency test: send same key twice
export function idempotencyTest() {
    const idempotencyKey = uuidv4();
    const payload = JSON.stringify(generateOrder());
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': idempotencyKey,
        },
    };

    const res1 = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
    const res2 = http.post(`${BASE_URL}/api/v1/orders`, payload, params);

    check(res1, { 'first request 202': (r) => r.status === 202 });
    check(res2, {
        'duplicate request succeeds': (r) => r.status === 200 || r.status === 202,
    });
}

export function handleSummary(data) {
    const summary = {
        timestamp: new Date().toISOString(),
        scenarios: {},
    };

    // Extract key metrics per scenario
    for (const [key, metric] of Object.entries(data.metrics)) {
        if (key.includes('http_req_duration') || key.includes('order_placement_duration')) {
            summary.scenarios[key] = {
                avg: metric.values.avg,
                p95: metric.values['p(95)'],
                p99: metric.values['p(99)'],
                max: metric.values.max,
            };
        }
        if (key === 'orders_placed') {
            summary.total_orders = metric.values.count;
        }
        if (key === 'order_errors') {
            summary.error_rate = metric.values.rate;
        }
    }

    return {
        'stdout': JSON.stringify(summary, null, 2) + '\n',
        'load-tests/results/summary.json': JSON.stringify(summary, null, 2),
    };
}
