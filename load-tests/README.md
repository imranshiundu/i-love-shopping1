# Load Testing

Load tests use [k6](https://k6.io) and mimic real shopper behavior. Three scenarios cover the critical user journeys.

## Prerequisites

- The full stack running (`./start.sh`)
- k6 installed: `brew install k6` / `choco install k6` / [releases](https://github.com/grafana/k6/releases)

> **Rate limits:** the platform ships with real token-bucket limits (10 req/min auth, 100 req/min API per IP, burst 20).
> Scenarios 2 and 3 generate far more traffic than that from a single machine, so raise the limits for the load test run:
>
> ```bash
> RATE_LIMIT_AUTH_PER_MINUTE=100000 RATE_LIMIT_API_PER_MINUTE=100000 RATE_LIMIT_BURST_CAPACITY=5000 ./start.sh
> ```
>
> Restore the real limits afterwards (plain `./start.sh`).

## Scenarios

| # | Script | Journey | Executor |
|---|--------|---------|----------|
| 1 | `1-browsing.js` | Home → catalog → filters → product details → reviews | Ramping VUs 0→50 |
| 2 | `2-search-and-cart.js` | Search suggestions → search results → add to cart → cart view | Ramping arrival rate 5→15 iter/s |
| 3 | `3-checkout-flow.js` | Register/login → cart fill → checkout → order lookup | Ramping VUs 0→25 |

## Running

```bash
# From the repo root
BASE_URL=http://localhost:8080/api/v1 FRONTEND_URL=http://localhost:3000 k6 run load-tests/1-browsing.js
BASE_URL=http://localhost:8080/api/v1 k6 run load-tests/2-search-and-cart.js
BASE_URL=http://localhost:8080/api/v1 k6 run load-tests/3-checkout-flow.js
```

## Objectives (asserted as k6 thresholds)

- 90% of user requests processed within 2 seconds — `http_req_duration: p(90)<2000`
- Platform supports at least 50 concurrent users without degradation (scenario 1 ramps to 50 VUs)
- Throughput of at least 10 transactions per second at peak (scenario 2 arrival rate)
- 98% of transactions complete successfully during high traffic — `checks: rate>0.90-0.95`
- Error rate of less than 5% — `http_req_failed: rate<0.05`

All five are encoded as k6 `thresholds`, so a run exits non-zero when any objective is missed.

## Finding the breaking point

Run scenario 1 with increasing peak VUs until p(95) exceeds 5 seconds:

```bash
BASE_URL=... k6 run -e PEAK=100 load-tests/1-browsing.js   # edit stages target or use --vus
# or quick smoke at fixed concurrency:
k6 run --vus 100 --duration 60s load-tests/1-browsing.js
```

Record the VU level where p(95) first crosses 5s — that is the platform's practical concurrency ceiling on the test hardware.

## Report template

Fill this in from real runs and paste into the performance section of docs/testing.md:

| Metric | Result |
|---|---|
| Max concurrent users before p95 > 5s | _from breaking-point run_ |
| Throughput at peak (req/s) | _from k6 summary_ |
| p(90) latency at 50 VUs | _from scenario 1_ |
| p(95) latency at 50 VUs | _from scenario 1_ |
| Error rate at peak | _from k6 summary_ |
| CPU/memory at peak | _docker stats during run_ |
| Bottlenecks identified | _documented below_ |

### Measured results (dev stack, 4-vCPU host, k6 from Docker)

| Metric | Result |
|---|---|
| Max concurrent users before p95 > 5s | **~150 VUs** (100 → p95 3.11s, 125 → 4.27s, 150 → 5.03s: first crossing; 0% errors at every level) |
| Throughput at peak (req/s) | Browsing ~60 req/s; search+cart **46 req/s** (10.4 iter/s); checkout ~8 req/s |
| p(90) latency at 50 VUs | **404 ms** |
| p(95) latency at 50 VUs | **897 ms** |
| Error rate at peak | **0.00%** (scenario 2: 5,591/5,591 checks; scenario 3: 100% after the deadlock fix) |
| CPU/memory at peak | JVM 27–66% of one core / ~510 MB; Postgres 19% / 116 MB; Redis 1% / 2.3 MB |
| Checkout scenario at 25 VUs | 100% checkout success, p90≈7.3s (auth-path BCrypt dominates — see bottleneck 2) |

Run-prep notes: raise the rate limits as above, use the dev reCAPTCHA key (`RECAPTCHA_SECRET_KEY=dev-test-secret`) so auth
latency measures the app rather than a round-trip to Google, and restock `shoes`/`test-item-kes-1` before scenario 3
(it places real orders and a sold-out product correctly returns 409).

### Known bottlenecks (from architecture review + measured under load)

- **PostgreSQL connection pool** — default Hikari pool (10) saturates under high concurrency; raise `spring.datasource.hikari.maximum-pool-size` (this repo sets 20) and add `pgbouncer` when scaling horizontally.
- **BCrypt on the auth path** — login/register/refresh run BCrypt (cost 12) twice (password verify + refresh-token hash), ~1.1s CPU per hash on the test host; cache verified refresh tokens in Redis and hash refresh tokens with a fast keyed hash (they are 256-bit random values).
- **Rate limiter bucket map** — grows one entry per IP; under attack-style load, evict idle buckets (scheduled cleanup) to bound memory.
- **N+1 on order items** — order responses load items lazily per order; batch fetch for large order lists.
- **Hot stock rows** — concurrent checkouts buying the same product queue on that product's row lock (deadlocks were found under load and fixed by decrementing in deterministic product-id order); shard hot-product counters or move reservations to Redis for very hot SKUs.
