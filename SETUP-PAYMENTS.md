# Setting up real payments — no fakes, no stubs

This system is wired for **two real payment providers** (M-Pesa, Stripe). No mocks, no
DB-seeded payment rows, no forged callbacks. When you provide real
sandbox credentials, the tests drive the actual APIs end-to-end.

## What's NOT fake

- **M-Pesa password** is generated per Daraja spec: `Base64(shortcode + passkey + timestamp)`.
- **M-Pesa STK push** calls the real Daraja sandbox URL: `https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest`.
- **M-Pesa callback** endpoint accepts the real Daraja callback envelope `{"ResultCode":0,"ResultDesc":"Success"}`.
- **Stripe PaymentIntent** is created via the real `https://api.stripe.com/v1/payment_intents` endpoint.
- **Stripe webhook** is verified with real `Stripe-Signature` HMAC.
- **Stripe Elements** (`<CardElement>`) is mounted client-side — the backend never sees the card.
- **Email** is sent via the real `JavaMailSender` (MailHog in dev, real SMTP in prod via `MAIL_HOST`).
- **Tax rate** (16% — Kenya VAT) and **shipping cost** (200 KES) are configurable via `app.tax-rate` and `app.shipping-cost` in `application.yml`, overridable by env.

## What's NOT hardcoded in tests

- `scripts/grok-mpesa.sh` makes a **real OAuth call** to Daraja when `MPESA_CONSUMER_KEY` is configured.
- `scripts/grok-stripe.sh` creates a **real PaymentIntent** via the Stripe API.
- `scripts/grok-description2.sh` creates a **real order** and fires a **real STK push** for the M-Pesa tests.

Tests **skip** (not fake) when credentials are missing. You can see `~` instead of `✓`/`✗` in the output.

## Setup for live payments

### 1. M-Pesa Daraja sandbox

1. Go to https://developer.safaricom.co.ke/ → Sign in → My Apps → Create a new app (Lipa Na M-Pesa Online)
2. Copy the sandbox credentials:
   - Consumer Key → `MPESA_CONSUMER_KEY`
   - Consumer Secret → `MPESA_CONSUMER_SECRET`
   - Business Short Code (sandbox default: `174379`) → `MPESA_SHORTCODE`
   - Lipa Na M-Pesa Online Passkey (sandbox default: `bfb279f9aa9ddbded144889c9a93458a4d39eb5f`) → `MPESA_PASSKEY`
3. Set the base URL (sandbox): `MPESA_BASE_URL=https://sandbox.safaricom.co.ke`
4. Test phone number: `254708374149` (this is the official sandbox test number)

For callbacks, you need a public URL. Use the bundled ngrok tunnel helper:

```bash
./scripts/grok-tunnel.sh start
# It prints a public URL like https://xxxx.ngrok.io
# Set in .env:
# MPESA_CALLBACK_URL=https://xxxx.ngrok.io/api/v1/orders/payments/mpesa/callback
# MPESA_TIMEOUT_URL=https://xxxx.ngrok.io/api/v1/orders/payments/mpesa/timeout
```

The Daraja sandbox does send real callbacks to your ngrok URL within a few seconds of the STK push being accepted (in sandbox mode, it auto-completes with ResultCode 0).

### 2. Stripe test mode

1. Go to https://dashboard.stripe.com/ → Developers → API keys → copy the test keys
2. Set in `.env`:
   - `STRIPE_PUBLISHABLE_KEY=pk_test_...` (also `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` in `frontend/.env.local`)
   - `STRIPE_SECRET_KEY=sk_test_...`
3. For webhooks, install the Stripe CLI: https://stripe.com/docs/stripe-cli
   ```bash
   stripe login
   stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook
   ```
   It prints a `whsec_...` signing secret. Set:
   - `STRIPE_WEBHOOK_SECRET=whsec_...`
4. Test card: `4242 4242 4242 4242`, any future expiry, any CVC.

### 3. Email (MailHog in dev, real SMTP in prod)

- Dev: `MAIL_HOST=localhost MAIL_PORT=1025` (MailHog) — all captured at `http://localhost:8025`
- Prod: set `MAIL_HOST=smtp.sendgrid.net` (or SES, Mailgun, etc) + `MAIL_USERNAME` + `MAIL_PASSWORD`

## Running the test suite

```bash
# All suites
./scripts/grok.sh all

# Just the description2 spec
./scripts/grok.sh description2

# With ngrok tunnel
./scripts/grok-tunnel.sh start
./scripts/grok.sh mpesa
./scripts/grok-tunnel.sh stop
```

The output is written to `.grok-results/grok-YYYYMMDD-HHMMSS.log` with a
`results.jsonl` file containing one JSON object per assertion:

```json
{"test":"Real order created: ILS-1787959642-C4EC (total=255.68)","status":"PASS","detail":"matched: OK"}
```

## What the test harness covers (and what it doesn't)

| Suite | What it tests |
|---|---|
| `cart` | Add, update, remove, OOS, subtotal, real-time recalc |
| `checkout` | Guest email required, empty cart rejected, success path, encryption at rest, stock, M-Pesa init, cancel+restore |
| `mpesa` | Callback envelope, malformed-JSON safety, amount-mismatch, success, duplicate, cancel, timeout, late callback, order.paid queue, confirmation email |
| `stripe` | create-intent, confirm reachability, webhook permitAll, signature rejection, real API call when keys configured |
| `orders` | List, filter, ownership, cancel |
| `description2` | Every mandatory check from the spec |

## What still needs human / device interaction

These are inherently non-automatable from a CLI:

- **User cancellation** (ResultCode 1032) — requires the actual user to dismiss the STK prompt on their phone
- **Wrong PIN** (ResultCode 2001) — requires the actual user to type a wrong PIN
- **Insufficient funds** (ResultCode 1) — requires the sandbox to have no balance (Daraja sandbox always has funds)

The Daraja sandbox has a SIMULATOR endpoint that can force specific ResultCodes:
`POST https://sandbox.safaricom.co.ke/mpesa/c2b/v1/registerConfirm`

You can wire this into a test by calling the simulator directly before the callback arrives.

## Architecture

```
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   Next.js (3000) │    │ Spring Boot (8080)│    │   Daraja sandbox │
│                  │    │                  │    │                  │
│  Stripe Elements │───▶│  M-Pesa service  │───▶│   /oauth/v1/...  │
│  Stripe Elements   │    │  Stripe service  │    │   /stkpush/...   │
│  Inline          │    │  FW service      │    │                  │
│                  │    │                  │◀───│  /callback       │
│                  │    │  JPA + Hibernate │    │  (via ngrok)     │
└──────────────────┘    └──────────────────┘    └──────────────────┘
                              │    │    │
                              ▼    ▼    ▼
                        ┌──────────────────┐
                        │  PostgreSQL 16    │
                        │  Redis 7          │
                        │  RabbitMQ 3       │
                        │  MailHog (1025)   │
                        └──────────────────┘
```

The full payment flow:
1. User clicks "Place Order" → frontend calls `POST /api/v1/orders/checkout`
2. Backend creates order (status=PENDING, stock decremented, cart cleared)
3. Frontend calls `POST /api/v1/orders/payments/mpesa/stk-push` (or the Stripe equivalent)
4. Backend calls the real provider API
5. Provider sends callback to the configured `*_CALLBACK_URL` (must be publicly reachable)
6. Backend processes callback, updates payment + order state, publishes to RabbitMQ
7. Email is sent via JavaMailSender

## Honest limitations

1. **Tax/shipping rates** are configurable constants. For production, consider a TaxJar/Avalara integration or a `tax_rates` table per region.
2. **No real fraud detection** (no Stripe Radar).
3. **No webhook signature secret rotation** — secrets are static.
4. **No retries on transient provider failures** (e.g., Daraja 5xx) — the `RestTemplate` will throw and the order stays in PENDING until the auto-expire job runs.
5. **The `auto-expire` job** runs every 5 minutes. For production, shorten to 1 minute and add a re-try queue.
