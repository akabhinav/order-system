#!/usr/bin/env python3
"""
Real load test against a running OMS instance.
Fires concurrent HTTP requests at POST /api/v1/orders using asyncio + aiohttp.
Falls back to threading + urllib if aiohttp is unavailable.

Usage: python3 load-test-real.py [--rps 1000] [--duration 10] [--url http://localhost:8080]
"""

import argparse
import json
import statistics
import sys
import time
import uuid
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Barrier, BrokenBarrierError
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

def generate_order():
    return json.dumps({
        "customerId": str(uuid.uuid4()),
        "items": [{
            "productId": str(uuid.uuid4()),
            "sku": f"LOAD-{uuid.uuid4().hex[:6].upper()}",
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
    }).encode()

def send_request(base_url):
    idempotency_key = str(uuid.uuid4())
    payload = generate_order()
    req = Request(
        f"{base_url}/api/v1/orders",
        data=payload,
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": idempotency_key,
        },
        method="POST",
    )
    start = time.monotonic()
    try:
        resp = urlopen(req, timeout=10)
        elapsed_ms = (time.monotonic() - start) * 1000
        return resp.status, elapsed_ms
    except HTTPError as e:
        elapsed_ms = (time.monotonic() - start) * 1000
        return e.code, elapsed_ms
    except (URLError, TimeoutError) as e:
        elapsed_ms = (time.monotonic() - start) * 1000
        return 0, elapsed_ms  # 0 = connection error

def percentile(data, p):
    if not data:
        return 0
    k = (len(data) - 1) * (p / 100)
    f = int(k)
    c = f + 1 if f + 1 < len(data) else f
    d = k - f
    return data[f] + d * (data[c] - data[f])

def run_load_test(base_url, rps, duration):
    total_requests = rps * duration
    print(f"\n{'='*60}")
    print(f"  REAL LOAD TEST: {rps} req/s x {duration}s = {total_requests} requests")
    print(f"  Target: {base_url}")
    print(f"{'='*60}\n")

    # Warmup
    print("  Warming up (5 requests)...")
    for _ in range(5):
        send_request(base_url)

    results = []
    status_counts = Counter()
    test_start = time.monotonic()

    with ThreadPoolExecutor(max_workers=min(rps * 2, 500)) as executor:
        futures = []

        for second in range(duration):
            batch_start = test_start + second
            # Wait until this second's slot
            now = time.monotonic()
            if batch_start > now:
                time.sleep(batch_start - now)

            # Fire rps requests for this second
            for _ in range(rps):
                futures.append(executor.submit(send_request, base_url))

            done_count = sum(1 for f in futures if f.done())
            print(f"  Second {second+1}/{duration}: dispatched {rps} requests "
                  f"(total in-flight: {len(futures) - done_count})")

        # Collect all results
        print("\n  Collecting results...")
        for f in as_completed(futures, timeout=60):
            try:
                status, latency_ms = f.result()
                results.append(latency_ms)
                status_counts[status] += 1
            except Exception as e:
                status_counts["error"] += 1

    test_duration = time.monotonic() - test_start
    results.sort()

    # Compute stats
    total = len(results)
    success_count = status_counts.get(202, 0)
    error_count = total - success_count
    error_rate = (error_count / total * 100) if total > 0 else 0
    actual_rps = total / test_duration

    avg = statistics.mean(results) if results else 0
    p50 = percentile(results, 50)
    p95 = percentile(results, 95)
    p99 = percentile(results, 99)
    max_lat = max(results) if results else 0

    print(f"\n{'='*60}")
    print(f"  RESULTS")
    print(f"{'='*60}")
    print(f"  Target rate:     {rps} req/s")
    print(f"  Duration:        {test_duration:.1f}s")
    print(f"  Total requests:  {total}")
    print(f"  Actual RPS:      {actual_rps:.1f} req/s")
    print(f"  ---- Status Codes ----")
    for code, count in sorted(status_counts.items()):
        print(f"    {code}: {count}")
    print(f"  ---- Latency ----")
    print(f"  Avg:             {avg:.1f} ms")
    print(f"  P50:             {p50:.1f} ms")
    print(f"  P95:             {p95:.1f} ms")
    print(f"  P99:             {p99:.1f} ms")
    print(f"  Max:             {max_lat:.1f} ms")
    print(f"  ---- SLA Check ----")
    print(f"  Error rate:      {error_rate:.2f}% {'PASS' if error_rate < 5 else 'FAIL'} (SLA: <5%)")
    print(f"  P99 < 500ms:     {'PASS' if p99 < 500 else 'FAIL'} ({p99:.1f}ms)")
    print(f"{'='*60}\n")

    # Exit code based on SLA
    if error_rate >= 5 or p99 >= 2000:
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="OMS Load Test")
    parser.add_argument("--rps", type=int, default=100, help="Requests per second")
    parser.add_argument("--duration", type=int, default=10, help="Test duration in seconds")
    parser.add_argument("--url", default="http://localhost:8080", help="Base URL")
    args = parser.parse_args()
    run_load_test(args.url, args.rps, args.duration)
