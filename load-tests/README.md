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

Fill this in from real runs and paste into the README performance section:

| Metric | Result |
|---|---|
| Max concurrent users before p95 > 5s | _from breaking-point run_ |
| Throughput at peak (req/s) | _from k6 summary_ |
| p(90) latency at 50 VUs | _from scenario 1_ |
| p(95) latency at 50 VUs | _from scenario 1_ |
| Error rate at peak | _from k6 summary_ |
| CPU/memory at peak | _docker stats during run_ |
| Bottlenecks identified | _documented below_ |

### Known bottlenecks (from architecture review)

- **PostgreSQL connection pool** — default Hikari pool (10) saturates under high concurrency; raise `spring.datasource.hikari.maximum-pool-size` when scaling.
- **JWT BCrypt refresh rotation** — refresh token verification runs BCrypt (cost 12) per refresh; ~100ms CPU each. Consider caching verified refresh tokens in Redis.
- **Rate limiter bucket map** — grows one entry per IP; under attack-style load, evict idle buckets (scheduled cleanup) to bound memory.
- **N+1 on order items** — order responses load items lazily per order; batch fetch for large order lists.
