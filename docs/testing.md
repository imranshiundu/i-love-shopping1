# Testing & Performance

## Performance Analysis

Load testing uses k6 with three scenarios that mimic real shopper behavior (browsing catalogs, searching + cart, checkout + account flow). Full instructions, scripts and the report template live in [`load-tests/`](load-tests/README.md).

**How to reproduce:**

```bash
RATE_LIMIT_AUTH_PER_MINUTE=100000 RATE_LIMIT_API_PER_MINUTE=100000 RATE_LIMIT_BURST_CAPACITY=5000 ./start.sh
BASE_URL=http://localhost:8080/api/v1 FRONTEND_URL=http://localhost:3000 k6 run load-tests/1-browsing.js
BASE_URL=http://localhost:8080/api/v1 k6 run load-tests/2-search-and-cart.js
BASE_URL=http://localhost:8080/api/v1 k6 run load-tests/3-checkout-flow.js
# breaking point: raise the peak until p(95) crosses 5s
BASE_URL=http://localhost:8080/api/v1 FRONTEND_URL=http://localhost:3000 PEAK=150 k6 run load-tests/1-browsing.js
```

> Restock the products the checkout scenario buys (`shoes`, `test-item-kes-1`) before high-volume
> runs of scenario 3 — it places real orders and can exhaust stock, which (correctly) fails checkouts
> with 409 once a product sells out.

**Objectives (asserted as k6 thresholds — a run fails when any is missed):**

| Objective | Threshold |
|---|---|
| 90% of requests within 2s | `http_req_duration: p(90)<2000` |
| 50 concurrent users supported | Scenario 1 ramps to 50 VUs |
| 10+ transactions/sec at peak | Scenario 2 arrival rate 15 iter/s |
| 98% transaction success at high traffic | `checks: rate>0.90` |
| Error rate < 5% | `http_req_failed: rate<0.05` |

**Results (measured on the dev stack: 4-vCPU host, API + Postgres 16 + Redis 7 + RabbitMQ + Next.js dev server, k6 from Docker with `--network host`):**

| Metric | Result |
|---|---|
| Max concurrent users before p95 > 5s | **~150 VUs** (100 VUs: p95=3.11s; 125 VUs: p95=4.27s; 150 VUs: p95=5.03s — first crossing) |
| Throughput at peak (req/s) | Browsing: ~60 req/s (11.8 iter/s); search+cart: **46 req/s** (10.4 iter/s); checkout: ~8 req/s |
| p(90) / p(95) at 50 VUs | **404 ms / 897 ms** (scenario 1, 100% checks) |
| Error rate at peak | **0.00%** across all scenarios at their peaks (scenario 2: 5,591/5,591 checks pass; scenario 3: 100% after deadlock fix) |
| CPU / memory at peak | JVM ~27–66% of one core, ~510 MB RSS; Postgres ~19% CPU, 116 MB; Redis ~1%, 2.3 MB (`docker stats` during runs) |
| Checkout scenario (25 VUs) | 100% checkouts succeed, 0 failed requests; p90≈7.3s dominated by the auth path (see bottleneck 2) |

**Known bottlenecks and proposed solutions:**

1. **PostgreSQL connection pool** — Hikari's default 10 connections saturate first under high concurrency. Fix: raise `spring.datasource.hikari.maximum-pool-size` (2× cores per instance; this repo already sets 20) and add `pgbouncer` in front when scaling horizontally.
2. **BCrypt on the auth hot path** — login/register/refresh run BCrypt (cost 12) twice (password verify + refresh-token hash): ~1.1s CPU per operation on the test hardware, so checkout-flow p90 degrades to ~7s at 25 VUs. Fix: cache recently-verified refresh tokens in Redis with a short TTL, and hash refresh tokens with a fast keyed hash (they are 256-bit random values and do not need password-grade KDFs).
3. **Rate-limiter bucket map** — one map entry per client IP grows unbounded under attack-style load. Fix: scheduled cleanup of idle buckets (already bounded in practice by nginx `limit_req` in production).
4. **Lazy order-items loading** — order list endpoints load items per order (N+1 on large lists). Fix: batch fetch with `@EntityGraph` for admin listing.
5. **Stock row lock contention** — concurrent checkouts buying the same product queue on the product row lock (they now lock in a deterministic order, which removed the deadlocks found under load testing). Fix: shard hot-product stock counters or move reservations to Redis.

## Testing

### Run All Tests

```bash
cd backend
./mvnw test
```

### Run Specific Test Classes

```bash
# Unit tests
./mvnw test -Dtest=JwtServiceTest
./mvnw test -Dtest=ProductTest
./mvnw test -Dtest=AuthValidationTest
./mvnw test -Dtest=SecurityTest

# With coverage
./mvnw test jacoco:report
```

### Test Reports

- **Surefire Reports**: `backend/target/surefire-reports/`
- **JaCoCo Coverage**: `backend/target/site/jacoco/index.html`

### Running Tests on Different Operating Systems

#### Linux/macOS

```bash
cd backend

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=JwtServiceTest

# Run with coverage report
./mvnw test jacoco:report

# View coverage report
xdg-open target/site/jacoco/index.html  # Linux
open target/site/jacoco/index.html      # macOS
```

#### Windows (PowerShell)

```powershell
cd backend

# Run all tests
.\mvnw.cmd test

# Run specific test class
.\mvnw.cmd test -Dtest=JwtServiceTest

# Run with coverage report
.\mvnw.cmd test jacoco:report

# View coverage report
start target\site\jacoco\index.html
```

#### Windows (Command Prompt)

```cmd
cd backend

REM Run all tests
mvnw.cmd test

REM Run specific test class
mvnw.cmd test -Dtest=JwtServiceTest

REM Run with coverage report
mvnw.cmd test jacoco:report

REM View coverage report
start target\site\jacoco\index.html
```

### Test Reports

- **Surefire Reports**: `backend/target/surefire-reports/`
- **JaCoCo Coverage**: `backend/target/site/jacoco/index.html`

### Test Categories

| Test Class | Category | Description |
|------------|----------|-------------|
| `JwtServiceTest` | Unit | JWT token generation, validation, expiry |
| `AuthValidationTest` | Unit | Input validation for auth DTOs |
| `AuthControllerTest` | API integration | Auth endpoint routing, validation, service delegation |
| `OrderControllerTest` | API integration | Unpaid-order delete delegation |
| `DataEncryptionServiceTest` | Unit | AES-GCM round-trip, idempotency, legacy plaintext compat |
| `CartFunctionalityTest` | Unit | Cart totals, snapshots, quantity recalculation |
| `CheckoutFlowTest` / `CheckoutValidationTest` | Unit | Order totals, shipping threshold, cancel guards, stock errors |
| `ProductTest` | Unit | Product entity business logic |
| `SecurityTest` | Unit | SQL injection, XSS, path traversal detection |
| `HealthCheckTest` | Unit | Health check response structure |
