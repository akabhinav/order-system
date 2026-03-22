import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Quick smoke test: validates the system works under moderate load
// Run with: k6 run --env BASE_URL=http://localhost:8080 quick-test.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const orderDuration = new Trend('order_duration_ms', true);
const errorRate = new Rate('errors');

export const options = {
    stages: [
        { duration: '10s', target: 50 },   // ramp to 50 VUs
        { duration: '30s', target: 100 },   // ramp to 100 VUs
        { duration: '30s', target: 100 },   // sustain 100 VUs
        { duration: '10s', target: 0 },     // ramp down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<300', 'p(99)<500'],
        'errors': ['rate<0.05'],
    },
};

export default function () {
    const payload = JSON.stringify({
        customerId: uuidv4(),
        items: [{
            productId: uuidv4(),
            sku: 'LOAD-TEST-001',
            quantity: 1,
            unitPrice: '29.99',
            currency: 'USD',
        }],
        shippingAddress: {
            street: '123 Test St',
            city: 'Loadville',
            state: 'CA',
            postalCode: '90210',
            countryCode: 'US',
        },
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': uuidv4(),
            'X-Request-Id': uuidv4(),
        },
    };

    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
    orderDuration.add(Date.now() - start);

    const ok = check(res, {
        'status is 202': (r) => r.status === 202,
        'p95 < 300ms': (r) => r.timings.duration < 300,
    });

    errorRate.add(!ok);
}
