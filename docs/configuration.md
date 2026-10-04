# Configuration

## Prerequisites

The project needs **Docker** for the database, cache, queue and mail services. Optionally **Java 21 + Maven** and **Node.js 20+** if you want to run the API or frontend directly on your machine.

| Tool | Required for | Notes |
|------|--------------|-------|
| **Docker** + **Docker Compose** | Everything (option 1 & 2) | Runs PostgreSQL 16, Redis 7, RabbitMQ 3 and Mailhog |
| **Java 21** (JDK) | Running the API locally (option 2) | Required for `spring-boot:run` |
| **Maven 3.9+** | Running the API locally (option 2) | The included `./mvnw` wrapper works if Maven is not installed |
| **Node.js 20+** | Running the frontend locally (option 2) | Option 1 runs the frontend inside Docker instead |

> **No setup script?** If you follow the manual commands below instead of using `scripts/dev.sh`, you are responsible for installing the prerequisites yourself.

### External Services (Required for Full Functionality)
- **M-Pesa Daraja API** credentials (Consumer Key, Secret, Shortcode, Passkey)
- **Google reCAPTCHA** (Site Key, Secret Key)
- **Google OAuth2** (Client ID, Secret) - OAuth2 login stays disabled when unset (see below)
- **GitHub OAuth2** (Client ID, Secret) - OAuth2 login stays disabled when unset (see below)
- **SMTP Server** for emails (Mailhog for development, Gmail or Brevo for real mail — see below)

> Payment providers are optional. The app starts without any payment keys — configure one or more in your `.env` file as needed. See `docs/setup/PAYMENT-SETUP-GUIDE.md` for setup instructions.

### Bring Your Own Tokens: OAuth, Email & CAPTCHA

You must create your own third-party tokens and paste them into `.env` (backend) and the frontend env (`frontend/.env.production` in production). Nothing ships with working keys.

**Google login**
1. Go to https://console.cloud.google.com → APIs & Services → Credentials → Create Credentials → OAuth client ID (Web application).
2. Authorized redirect URI: `https://YOUR-API-HOST/api/v1/auth/oauth2/code/google` (local dev: `http://localhost:8080/api/v1/auth/oauth2/code/google`).
3. Paste into `.env`: `GOOGLE_CLIENT_ID=...` and `GOOGLE_CLIENT_SECRET=...`.
4. Show the button: set `NEXT_PUBLIC_GOOGLE_ENABLED=true` in the frontend env and redeploy the frontend.

**GitHub login**
1. Go to https://github.com/settings/developers → OAuth Apps → New OAuth App.
2. Authorization callback URL: `https://YOUR-API-HOST/api/v1/auth/oauth2/code/github`.
3. Paste into `.env`: `GITHUB_CLIENT_ID=...` and `GITHUB_CLIENT_SECRET=...`.
4. Show the button: set `NEXT_PUBLIC_GITHUB_ENABLED=true` in the frontend env and redeploy the frontend.

**Email — Gmail app password (simplest, step by step)**
1. Sign in to the Google account that will send the mail.
2. Turn on 2-Step Verification: Google Account → Security → 2-Step Verification → follow the prompts (required — app passwords don't exist without it).
3. Create the app password: Security → search "App passwords" → name it (e.g. `i-love-shopping`) → Google shows a **16-letter code** like `abcd efgh ijkl mnop`. Copy it immediately (it won't be shown again).
4. Paste into `.env` — **delete the spaces** from the code and map it like this:
   | Gmail value | `.env` variable |
   |---|---|
   | `smtp.gmail.com` | `MAIL_HOST` |
   | `587` | `MAIL_PORT` |
   | your Gmail address | `MAIL_USERNAME` **and** `MAIL_FROM` (must be the same account) |
   | 16-letter code, no spaces (`abcdefghijklmnop`) | `MAIL_PASSWORD` |
   | on | `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true` |
5. Restart the backend and register a test account — the verification email should land in the Gmail inbox (check Spam first). If it fails, the app logs the SMTP error on startup/first send; the usual cause is spaces left in the code or 2-Step Verification being off.

**Email — Brevo (or any SMTP service)**
1. Sign up at https://www.brevo.com → get an SMTP key (any provider works: Brevo, SendGrid, SES, Mailgun...).
2. Paste into `.env`: `MAIL_HOST` (e.g. `smtp-relay.brevo.com`), `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD` (the SMTP key), `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`, `MAIL_FROM` (a verified sender on that service).

**Email — local dev only (MailHog, nothing is sent)**
`MAIL_HOST=localhost`, `MAIL_PORT=1025`, `MAIL_SMTP_AUTH=false`, `MAIL_SMTP_STARTTLS=false` — read everything at `http://localhost:8025`.

**reCAPTCHA v3 (registration bot protection)**
1. https://www.google.com/recaptcha/admin → register the site (v3) → get Site Key + Secret Key.
2. Backend `.env`: `RECAPTCHA_SECRET_KEY=<secret>`; frontend env: `NEXT_PUBLIC_RECAPTCHA_SITE_KEY=<site key>` and `NEXT_PUBLIC_RECAPTCHA_ENABLED=true`.
3. Leave the dev defaults (`dev-test-secret` + disabled flag) and verification is bypassed for local work.

### Payment test keys (sandbox)

Both money rails work out of the box in test mode — no real money moves.

**M-Pesa Daraja sandbox (pre-filled — Africa only)**
- M-Pesa works **only in supported African markets** (Kenya, Tanzania, DRC, Mozambique, Lesotho, Ghana, Ethiopia). Outside Africa, test payments with a Stripe card below instead.
- `.env` already contains working sandbox credentials (`MPESA_*`) — nothing to paste.
- Test phone: `254708374149` (Safaricom's official sandbox number). Any STK push to it is accepted with `ResponseCode: 0`.
- In sandbox, Safaricom auto-completes the prompt with `ResultCode: 0` within seconds **if it can reach your callback URL**. Locally that means running a tunnel (ngrok/cloudflared) and setting `MPESA_CALLBACK_URL`/`MPESA_TIMEOUT_URL` to it; without a tunnel you can still drive the full flow by POSTing a Daraja-shaped callback to `/orders/payments/mpesa/callback` yourself.
- Unanswered prompts expire after `MPESA_STK_TIMEOUT_SECONDS` (default 120) and the order gets a payable invoice email automatically.
- Production: get your own keys at https://developer.safaricom.co.ke, set `MPESA_ENVIRONMENT=production` + `MPESA_BASE_URL=https://api.safaricom.co.ke`, and point the callback URLs at your public API.

**Stripe test mode (works worldwide)**
1. https://dashboard.stripe.com/test/apikeys → copy the test keys into `.env` (`STRIPE_SECRET_KEY=sk_test_...`) and the frontend env (`NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_...`). Anyone, anywhere can test card payments with these.
2. Test card: `4242 4242 4242 4242`, any future expiry, any CVC.
3. Orders under `STRIPE_MIN_AMOUNT` (default KES 100) are rejected with a message pointing at M-Pesa — Stripe itself refuses sub-≈$0.50 charges.
4. Webhooks (optional, for real-time status): install the Stripe CLI, run `stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook`, and set the printed `whsec_...` as `STRIPE_WEBHOOK_SECRET`. Unsigned webhook calls are rejected with 400 (never 500, so Stripe won't pointlessly retry them).

**End-to-end test payment (either rail)**
1. Add a product to the cart → checkout → place the order (status `PENDING`, invoice email arrives).
2. M-Pesa: STK push to `254708374149` → approve (or wait for the sandbox auto-callback) → order flips to `CONFIRMED`.
3. Card: create a PaymentIntent → confirm with `4242...` → `CONFIRMED`.
4. Watch `/account/orders`, the confirmation email, and the receipt at `/account/orders/NUMBER/receipt`.

> Deep-dive (no mocks, real API calls, failure matrix): [`SETUP-PAYMENTS.md`](SETUP-PAYMENTS.md). Automated harness: `./scripts/grok.sh all`.

### Going live with a real bank card

**How the modes differ (important):** unlike M-Pesa Daraja — where even the sandbox auto-completes prompts against a fake balance — **Stripe test mode moves no money at all**: charges and refunds are simulated ledger entries visible only in your test dashboard. Flip to live and the *same code* moves real money, which you then reverse with a real refund. So: test freely in test mode, and after any live test, refund immediately (below).

Switching is one variable — the storefront follows automatically (it loads whichever publishable key matches the backend mode, and badges the checkout `Test mode` vs `Live — real money`):

1. **Activate your Stripe account** at https://dashboard.stripe.com (business details + payout bank account; required before live keys work).
2. **Copy the LIVE keys**: Developers → API keys → `pk_live_...` and `sk_live_...` (never commit them, never paste them in chat or email).
3. **Paste them once** as the live set (keep the test set untouched):
   `STRIPE_LIVE_SECRET_KEY=sk_live_...`, `STRIPE_LIVE_PUBLISHABLE_KEY=pk_live_...`, `STRIPE_LIVE_WEBHOOK_SECRET=whsec_...` — in backend `.env`, and on the production server's `.env`. No frontend change needed.
4. **Flip the switch**: `STRIPE_ENVIRONMENT=live` (back to `test` when done) + restart the API. That's it.
5. **Live webhook**: Stripe Dashboard → Developers → Webhooks → Add endpoint `https://YOUR-API-HOST/api/v1/payments/stripe/webhook`, subscribe to `payment_intent.succeeded` + `payment_intent.payment_failed` → that's where the live `whsec_...` comes from.
6. **Safe first live test**: buy a cheap item (e.g. KES 150) with your real card → confirm the order flips to CONFIRMED and the charge appears in the **live** dashboard → refund it from the admin Orders page (REFUNDED button calls the real Stripe refund API and reverses the charge). A small processing fee may be non-refundable — that's Stripe's cut, not ours.
7. Card data never touches our servers in either mode (Stripe Elements tokenizes in the browser), so PCI scope doesn't change.

**M-Pesa production (real money on STK approval):** create a production app at https://developer.safaricom.co.ke, set `MPESA_ENVIRONMENT=production`, `MPESA_BASE_URL=https://api.safaricom.co.ke`, your production shortcode/passkey/keys, and publicly reachable callback URLs. M-Pesa reversals happen in the M-Pesa org portal — the system has no auto-reversal endpoint for mobile money.

## Configuration

### Application Profiles

| Profile | Description |
|---------|-------------|
| `development` | Default local profile. Connects to PostgreSQL on `localhost:5432` and Redis on `localhost:6379` |
| `test` | Testcontainers-based integration tests |
| `docker` | Used inside Docker Compose. Connects to the `postgres` and `redis` services over the compose network |

> When the API runs locally against the development containers, use `DATABASE_URL` pointing at port **5433** and `REDIS_PORT=6380` (see [Running Manually](#running-manually)).

### Key Backend Properties

```yaml
# Database
spring.datasource.url: jdbc:postgresql://localhost:5432/iloveshopping
spring.datasource.username: iloveshopping
spring.datasource.password: ${DATABASE_PASSWORD}

# JWT
security.jwt.access-secret: ${JWT_ACCESS_SECRET}
security.jwt.refresh-secret: ${JWT_REFRESH_SECRET}
security.jwt.access-expiry-minutes: 15
security.jwt.refresh-expiry-days: 7

# RabbitMQ (order events)
spring.rabbitmq.host: ${RABBITMQ_HOST:localhost}
spring.rabbitmq.port: ${RABBITMQ_PORT:5672}
spring.rabbitmq.username: ${RABBITMQ_USERNAME}
spring.rabbitmq.password: ${RABBITMQ_PASSWORD}

# Mail (set MAIL_SMTP_AUTH=false for Mailhog)
spring.mail.host: ${MAIL_HOST}
spring.mail.port: ${MAIL_PORT}
spring.mail.properties.mail.smtp.auth: ${MAIL_SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable: ${MAIL_SMTP_STARTTLS:true}

# Encryption at rest (addresses, payment records, email/name/guest email).
# REQUIRED — there is no fallback: the application refuses to start without
# a key rather than encrypting with a shared hardcoded one.
# Generate one: openssl rand -base64 32
app.data-encryption-key: ${DATA_ENCRYPTION_KEY}

# M-Pesa (set your keys in .env)
mpesa.environment: ${MPESA_ENVIRONMENT:sandbox}
mpesa.consumer-key: ${MPESA_CONSUMER_KEY:}
mpesa.consumer-key: ${MPESA_CONSUMER_KEY}
mpesa.consumer-secret: ${MPESA_CONSUMER_SECRET}
mpesa.shortcode: ${MPESA_SHORTCODE}
mpesa.passkey: ${MPESA_PASSKEY}
mpesa.callback-url: ${MPESA_CALLBACK_URL}

# Rate Limiting
security.rate-limit.auth-requests-per-minute: 10
security.rate-limit.api-requests-per-minute: 100
# X-Forwarded-For / X-Real-IP are only trusted when the API actually sits
# behind our nginx proxy (docker-compose.prod.yml sets this to true);
# direct deployments keep it false — otherwise the client IP is spoofable
# by rotating the forwarded header, defeating the buckets.
security.rate-limit.trust-forwarded-ips: ${RATE_LIMIT_TRUST_FORWARDED:false}

# Commerce rules
app.default-currency: KES
app.tax-rate: 0.16
app.free-shipping-threshold: 5000
```

### Frontend Environment Variables (`frontend/.env.local`)

Every value in the frontend is configurable - no hardcoded URLs, prices or identity strings. All variables use the `NEXT_PUBLIC_` prefix:

| Variable | Default (dev) | Purpose |
|----------|---------------|---------|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api/v1` | Backend API base URL |
| `NEXT_PUBLIC_APP_NAME` | `i-love-shopping` | Brand name shown in header/footer/metadata |
| `NEXT_PUBLIC_APP_DESCRIPTION` | Kenyan market blurb | Metadata description |
| `NEXT_PUBLIC_APP_URL` | `http://localhost:3000` | Public site URL |
| `NEXT_PUBLIC_SUPPORT_EMAIL` | `support@iloveshopping.com` | Contact email in footer |
| `NEXT_PUBLIC_COMPANY_LOCATION` | `Nairobi, Kenya` | Address line in footer |
| `NEXT_PUBLIC_DEFAULT_COUNTRY` | `KE` | Pre-selected country on address forms |
| `NEXT_PUBLIC_CURRENCY` / `NEXT_PUBLIC_LOCALE` | `KES` / `en-KE` | Currency formatting |
| `NEXT_PUBLIC_FREE_SHIPPING_THRESHOLD` | `5000` | Free shipping above this subtotal |
| `NEXT_PUBLIC_SHIPPING_COST` | `200` | Flat shipping fee below threshold |
| `NEXT_PUBLIC_TAX_RATE` | `0.16` | VAT rate applied at cart/checkout |
| `NEXT_PUBLIC_MIN_PASSWORD_LENGTH` | `8` | Registration/password validation |
| `NEXT_PUBLIC_ALLOWED_IMAGE_HOSTS` | `picsum.photos,images.unsplash.com,raw.githubusercontent.com` | Next.js image allowlist (comma-separated) |
| `NEXT_PUBLIC_FEATURED_PRODUCTS_COUNT` | `8` | Products on the home page |
| `NEXT_PUBLIC_PRODUCTS_PAGE_SIZE` | `12` | Products per listing page |
| `NEXT_PUBLIC_ORDERS_PAGE_SIZE` | `10` | Orders per account page |
| `NEXT_PUBLIC_ADMIN_PAGE_SIZE` | `20` | Rows per admin table |
| `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` / `_ENABLED` | dev bypass | reCAPTCHA integration |
| `NEXT_PUBLIC_GOOGLE_ENABLED` / `NEXT_PUBLIC_GITHUB_ENABLED` | `false` | Show OAuth buttons (needs matching backend secrets) |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | (empty) | Stripe publishable key (test or live) |
| `NEXT_PUBLIC_CURRENCY_RATES` | `USD=0.0077,EUR=0.0071,...` | Display-currency conversion rates from KES |

### Multi-Currency Display

The storefront shows prices in **KES by default**. Visitors can switch
currency anytime from the globe icon in the header; the choice persists in
their browser and every price on the site converts instantly.

| Currency | Code | Default rate (from KES) |
|----------|------|-------------------------|
| Kenyan Shilling | `KES` | 1 (base) |
| US Dollar | `USD` | 0.0077 |
| Euro | `EUR` | 0.0071 |
| Pound Sterling | `GBP` | 0.0061 |
| Tanzanian Shilling | `TZS` | 19.8 |
| Ugandan Shilling | `UGX` | 28.3 |
| South African Rand | `ZAR` | 0.14 |

- Rates are display-only estimates set through
  `NEXT_PUBLIC_CURRENCY_RATES=CODE=RATE,CODE=RATE` - plug in live FX rates here.
- **Payments always settle in KES** (M-Pesa and the card gateway
  are Kenyan-shilling rails). Checkout states this clearly whenever a
  non-KES currency is selected, so there is no surprise at the PIN prompt.

> Keep these values in sync with backend `app.tax-rate`, `app.free-shipping-threshold` and `app.default-currency` so cart math matches server-side totals.
