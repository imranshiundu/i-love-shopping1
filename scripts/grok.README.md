# Grok — End-to-End Test Harness

The `grok` is the canonical test harness for i-love-shopping. It exercises the
cart, checkout, payment (M-Pesa, Stripe), and order flows
against a live backend, then runs the full `description2.txt` mandatory test
battery.

## Philosophy: real, not faked

Every test in the harness drives the real API. There is no DB-seeding of
payment rows, no forged callback bodies, no mocked providers. When a
provider's sandbox credentials are configured in `.env`, the harness
performs a real STK push, waits for a real callback, and checks a real
email. When credentials are missing, the harness **skips** the test (not
fakes it). See `SETUP-PAYMENTS.md` for the full setup.

## Layout

| File | Purpose |
|---|---|
| `grok.sh` | Top-level runner. Orchestrates the other suites and prints a summary. |
| `grok-mpesa.sh` | Real M-Pesa STK push + callback polling. Also covers callback envelope, malformed-JSON safety, and amount-mismatch logic. |
| `grok-stripe.sh` | Real Stripe PaymentIntent creation + test-card confirmation. Webhook permitAll + signature-rejection. |
| `grok-description2.sh` | The `description2.txt` mandatory test battery — 32 checks, all live. |
| `grok-tunnel.sh` | ngrok helper. Starts/stops a tunnel to `:8080` so Daraja/Stripe can call back. |

## Usage

```bash
# Run everything
./scripts/grok.sh all

# Or one suite at a time
./scripts/grok.sh cart
./scripts/grok.sh checkout
./scripts/grok.sh mpesa
./scripts/grok.sh stripe
./scripts/grok.sh orders
./scripts/grok.sh description2

# Tunnel for live callbacks
./scripts/grok-tunnel.sh start
./scripts/grok.sh mpesa
./scripts/grok-tunnel.sh stop

# Machine-readable output
./scripts/grok.sh all --json > results.json

# No colour (for log files)
./scripts/grok.sh all --no-color | tee run.log
```

## What happens when keys ARE configured

```
M-Pesa suite
  ✓ Empty callback returns 200 envelope with ResultCode:0
  ✓ Malformed JSON returns 200 (no crash)
  ...
  ✓ Real M-Pesa OAuth token obtained from https://sandbox.safaricom.co.ke
  ✓ Real order created: ILS-1787959642-C4EC (total=255.68)
  ✓ Real STK push to Daraja: responseCode=0, checkoutRequestId=ws_CO_xxx
  ✓ Real callback received from Daraja: resultCode=0 after 9s
  ✓ Order auto-confirmed by real callback
  ✓ Confirmation email sent to guest after real callback
```

## What happens when keys are NOT configured

```
M-Pesa suite
  ✓ Empty callback returns 200 envelope with ResultCode:0
  ~ Real M-Pesa payment — set MPESA_CONSUMER_KEY/SECRET/SHORTCODE/PASSKEY in .env
  ~ Real order confirmation email — depends on M-Pesa callback
```

Tests **skip** rather than fake. The `~` symbol indicates a skip (not a pass, not a fail).

## Pre-flight

The grok expects:
- Backend at `http://localhost:8080`
- Frontend at `http://localhost:3000` (optional)
- MailHog at `http://localhost:8025`
- RabbitMQ at `http://localhost:15672`
- Postgres at `localhost:5433` (DB user `iloveshopping`, password `iloveshopping`)
- Seed users: `user@iloveshopping.com` / `User123!`, `admin@iloveshopping.com` / `Admin123!`

For live M-Pesa:
- `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_SHORTCODE`, `MPESA_PASSKEY` in `.env`
- ngrok running: `./scripts/grok-tunnel.sh start`
- `MPESA_CALLBACK_URL` and `MPESA_TIMEOUT_URL` set to the ngrok URL

For live Stripe:
- `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY` in `.env`
- Stripe CLI running: `stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook`
- `STRIPE_WEBHOOK_SECRET` from the Stripe CLI output

## What the M-Pesa suite tests when keys are configured

1. Gets a real OAuth token from `https://sandbox.safaricom.co.ke/oauth/v1/generate`
2. Creates a real order via `POST /api/v1/orders/checkout`
3. Fires a real STK push to Daraja
4. Polls `POST /api/v1/payments/mpesa/stk-query` for the real callback
5. Verifies the order transitioned PENDING → CONFIRMED in the DB
6. Verifies the confirmation email was sent via MailHog

## What the M-Pesa suite tests when keys are NOT configured

Still tests the callback handler:
- Empty body returns 200 envelope with `ResultCode: 0`
- Malformed JSON returns 200 (no crash)
- Missing Body node returns 200
- Unknown CheckoutRequestID returns 200
- Timeout endpoint returns 200 envelope
- User-cancelled callback returns 200
- Timeout with unknown id returns 200

These are tested directly against `POST /api/v1/orders/payments/mpesa/callback` with synthetic (but realistic) callback bodies. The M-Pesa service processes them end-to-end.

## Output

Results are written to `.grok-results/grok-YYYYMMDD-HHMMSS.log`. The
JSONL file `.grok-results/results.jsonl` contains one line per assertion:

```json
{"test":"Real M-Pesa OAuth token obtained","status":"PASS","detail":"matched: OK"}
{"test":"Real callback received from Daraja","status":"PASS","detail":"resultCode=0 after 9s"}
```

## Why a real-DB test harness?

Unit tests catch logic errors. The grok catches:
- Wiring mistakes (Spring beans not autowired, security config wrong)
- Transactional bugs (e.g., the `@Transactional readOnly` fix on `PaymentService`)
- Real M-Pesa callback envelope compliance
- DB-level constraints (orders_status_check, FK violations)
- Email delivery (MailHog)
- Message queue wiring (RabbitMQ)
- Cookie persistence
- Cart restore on cancel
- Stock atomicity

A passing grok with `MPESA_*` keys in `.env` and ngrok running is a strong
signal that the system actually works end-to-end with real providers.

