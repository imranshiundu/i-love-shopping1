# Payment Setup Guide for Testers

This guide covers how to set up and test all payment methods in the i-Love-Shopping platform. It is written for testers in **Estonia** (who use bank cards) and **Kenya** (who use M-Pesa).

## Payment Methods Overview

| Method | Who uses it | How it works | Test mode |
|--------|------------|--------------|-----------|
| **M-Pesa** | Kenya (Safaricom) | STK push to phone, enter PIN | Daraja sandbox |
| **Stripe Card** | Estonia, worldwide | Enter card details in checkout | Stripe test mode |
| **Flutterwave Card** | Africa-wide | Redirect to Flutterwave checkout | Flutterwave test mode |

---

## Setup for Estonia Testers (Bank Cards)

### Option A: Stripe (Recommended)

Stripe is the simplest way to test card payments. No redirect — card details are entered directly in checkout.

#### Step 1: Get Stripe Test Keys

1. Go to https://dashboard.stripe.com/register and create a free account
2. Once logged in, go to https://dashboard.stripe.com/test/apikeys
3. Copy your **Test** keys (they start with `sk_test_` and `pk_test_`)
4. You'll also need a **Webhook Secret** — go to https://dashboard.stripe.com/test/webhooks, click "Add endpoint", set URL to `http://YOUR_NGROK_URL/api/v1/payments/stripe/webhook`, select `payment_intent.succeeded` and `payment_intent.payment_failed` events

#### Step 2: Set Environment Variables

```bash
export STRIPE_SECRET_KEY=sk_test_your_key_here
export STRIPE_PUBLISHABLE_KEY=pk_test_your_key_here
export STRIPE_WEBHOOK_SECRET=whsec_your_secret_here
```

Or add to `scripts/dev.sh` / `start-backend2.sh`.

#### Step 3: Test with Stripe Test Cards

| Card Number | Result | Use case |
|-------------|--------|----------|
| `4242 4242 4242 4242` | Success | Happy path |
| `4000 0000 0000 0002` | Declined | Card declined |
| `4000 0025 0000 3155` | Requires 3D Secure | Authentication required |
| `4000 0000 0000 9995` | Insufficient funds | Not enough money |

- **Expiry**: Any future date (e.g., `12/28`)
- **CVV**: Any 3 digits (e.g., `123`)
- **Name**: Any name

#### Step 4: Test the Flow

1. Add products to cart
2. Go to checkout
3. Select **Card — Stripe**
4. Enter test card number `4242 4242 4242 4242`
5. Enter any future expiry and any CVV
6. Click "Pay"
7. Payment should succeed immediately (no 3DS for this card)
8. Redirect to success page

---

### Option B: Flutterwave

Flutterwave redirects to their hosted checkout page. Supports cards, mobile money, and bank transfers across Africa.

#### Step 1: Get Flutterwave Test Keys

1. Go to https://app.flutterwave.com/register and create a free account
2. Go to Settings → API Keys (make sure you're in **Test Mode**)
3. Copy your **Test** keys:
   - **Public Key** (starts with `FLWPUBK_TEST-...`)
   - **Secret Key** (starts with `FLWSECK_TEST-...`)
   - **Encryption Key** (from the same page)

#### Step 2: Set Environment Variables

```bash
export FLUTTERWAVE_SECRET_KEY=FLWSECK_TEST-your_key_here
export FLUTTERWAVE_PUBLIC_KEY=FLWPUBK_TEST-your_key_here
export FLUTTERWAVE_ENCRYPTION_KEY=your_encryption_key_here
```

#### Step 3: Test with Flutterwave Test Cards

| Card Number | Result |
|-------------|--------|
| `4187427415564246` | Success (Visa) |
| `4000000000000002` | Success (Mastercard) |
| `4000000000000069` | Declined |
| `4000000000000127` | Insufficient funds |

- **Expiry**: Any future date
- **CVV**: Any 3 digits

#### Step 4: Test the Flow

1. Add products to cart
2. Go to checkout
3. Select **Card — Flutterwave**
4. Click "Pay" — you'll be redirected to Flutterwave's checkout page
5. Enter test card details on the Flutterwave page
6. Complete payment
7. Redirected back to success page

---

## Setup for Kenya Testers (M-Pesa)

### Using Daraja Sandbox

The sandbox uses Safaricom's test environment. No real money moves.

#### Step 1: Credentials (Pre-configured)

The sandbox credentials are already configured in the application:
- Consumer Key: `o6tPhH5EoSsGmtLZAG4yMT7b2GQp9e87X3h4T6dk0DuZh2aU`
- Consumer Secret: `LgJ5V6tShu5SCA9nhcyAA8qrkuq3nHUsbQdmCDRinBIPmjGEY2hVmxoqi28XSdPE`
- Shortcode: `174379`
- Passkey: `bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919`

#### Step 2: Get a Daraja App (Optional, for production)

If you need your own credentials:
1. Go to https://developer.safaricom.co.ke
2. Create an account and create a new app
3. Copy the Consumer Key, Consumer Secret, and Passkey

#### Step 3: Set Up Callback URL (for real callbacks)

Safaricom sends payment callbacks to your server. For local development, use **ngrok**:

```bash
# Install ngrok
npm install -g ngrok

# Start ngrok
ngrok http 8080

# Copy the public URL (e.g., https://abc123.ngrok-free.app)
# Update the callback URL:
export MPESA_CALLBACK_URL=https://YOUR_NGROK_URL/api/v1/orders/payments/mpesa/callback
```

See `docs/setup/M-PESA-DARAJA-SETUP.md` for full details.

#### Step 4: Test the Flow

1. Add products to cart
2. Go to checkout
3. Select **M-Pesa**
4. Enter your Safaricom phone number (sandbox: any valid format like `254712345678`)
5. Click "Pay" — STK push is sent to Safaricom sandbox
6. In sandbox, the PIN prompt may not appear on your phone — the app polls for status automatically
7. Payment confirms after polling

---

## Local Callback Setup (ngrok)

All payment providers (M-Pesa, Stripe, Flutterwave) may send webhooks/callbacks to your server. For local development:

### 1. Install ngrok

```bash
# macOS
brew install ngrok

# Linux
curl -s https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz | sudo tar -xz -C /usr/local/bin

# Windows: download from https://ngrok.com/download
```

### 2. Sign up and authenticate

```bash
ngrok config add-authtoken YOUR_TOKEN
```

### 3. Start ngrok

```bash
ngrok http 8080
```

### 4. Configure callbacks

**M-Pesa:**
```bash
export MPESA_CALLBACK_URL=https://YOUR_NGROK_URL/api/v1/orders/payments/mpesa/callback
```

**Stripe:**
Go to https://dashboard.stripe.com/test/webhooks → Add endpoint:
- URL: `https://YOUR_NGROK_URL/api/v1/payments/stripe/webhook`
- Events: `payment_intent.succeeded`, `payment_intent.payment_failed`

**Flutterwave:**
Go to https://app.flutterwave.com/test → Settings → Webhooks:
- URL: `https://YOUR_NGROK_URL/api/v1/payments/flutterwave/callback`

---

## Environment Variables Reference

```bash
# M-Pesa (sandbox)
MPESA_CONSUMER_KEY=o6tPhH5EoSsGmtLZAG4yMT7b2GQp9e87X3h4T6dk0DuZh2aU
MPESA_CONSUMER_SECRET=LgJ5V6tShu5SCA9nhcyAA8qrkuq3nHUsbQdmCDRinBIPmjGEY2hVmxoqi28XSdPE
MPESA_SHORTCODE=174379
MPESA_PASSKEY=bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919
MPESA_BASE_URL=https://sandbox.safaricom.co.ke

# Stripe (test mode)
STRIPE_SECRET_KEY=sk_test_your_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_key
STRIPE_WEBHOOK_SECRET=whsec_your_secret

# Flutterwave (test mode)
FLUTTERWAVE_SECRET_KEY=FLWSECK_TEST-your_key
FLUTTERWAVE_PUBLIC_KEY=FLWPUBK_TEST-your_key
FLUTTERWAVE_ENCRYPTION_KEY=your_encryption_key
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Stripe is not configured" | Set `STRIPE_SECRET_KEY` env var |
| "Flutterwave is not configured" | Set `FLUTTERWAVE_SECRET_KEY` env var |
| M-Pesa callback not received | Start ngrok, update callback URL |
| Card payment fails immediately | Check test card number and expiry |
| 401 on payment endpoint | Ensure you're logged in |
| Payment stuck in "Processing" | Check webhook/callback is configured |
| "Request cancelled by user" (M-Pesa) | Normal in sandbox if PIN not entered within 60s |
