# M-Pesa Daraja Callback Setup with ngrok

This guide helps you test M-Pesa STK Push end-to-end on your local machine by exposing your local backend to the internet via ngrok, so Safaricom's Daraja API can send payment callbacks to your machine.

## Prerequisites

1. **ngrok** — free account at https://ngrok.com
2. **Daraja sandbox credentials** — already configured in `.env` / `application.yml`
3. Backend running on port 8080

## Step 1: Install ngrok

```bash
# macOS
brew install ngrok

# Linux (Debian/Ubuntu)
curl -s https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz | sudo tar -xz -C /usr/local/bin

# Windows — download from https://ngrok.com/download
```

## Step 2: Authenticate ngrok

Sign up at https://ngrok.com and get your authtoken:

```bash
ngrok config add-authtoken YOUR_AUTH_TOKEN
```

## Step 3: Start ngrok

```bash
ngrok http 8080
```

This gives you a public URL like:
```
https://abc123.ngrok-free.app
```

## Step 4: Update M-Pesa callback URL

Set the callback URL to point to your ngrok tunnel:

```bash
# Option A: Export before starting the backend
export MPESA_CALLBACK_URL=https://YOUR_NGROK_URL/api/v1/orders/payments/mpesa/callback
export MPESA_TIMEOUT_URL=https://YOUR_NGROK_URL/api/v1/orders/payments/mpesa/timeout

# Option B: Update in application.yml (dev profile)
mpesa:
  callback-url: https://YOUR_NGROK_URL/api/v1/orders/payments/mpesa/callback
  timeout-url: https://YOUR_NGROK_URL/api/v1/orders/payments/mpesa/timeout
```

Then restart the backend.

## Step 5: Test M-Pesa STK Push

1. Add items to your cart
2. Go to checkout
3. Enter your Safaricom phone number (sandbox: any valid format)
4. Click "Pay via M-Pesa"
5. On sandbox, the STK push goes to the Safaricom test environment
6. Your backend receives the callback at the ngrok URL

## How It Works

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│ Frontend │────▶│ Your Backend │────▶│ Safaricom API   │────▶│ Customer     │
│ (3000)   │     │ (8080)       │     │ (Daraja)        │     │ Phone        │
└──────────┘     └──────┬───────┘     └─────────────────┘     └──────┬───────┘
                        │                                              │
                        │◀──── callback via ngrok ─────────────────────┘
                        │
                        ▼
                   Payment confirmed
```

## Sandbox Notes

- **Shortcode**: 174379 (test shortcode)
- **Phone number**: Use any valid format, e.g., 254712345678
- **Amounts**: Any amount works in sandbox
- **No real money moves** — sandbox is completely free
- **Callbacks**: In sandbox, callbacks may not always fire. The app also polls for status as a fallback.

## Production Deployment

For production, replace the sandbox credentials:

```bash
export MPESA_ENVIRONMENT=production
export MPESA_CONSUMER_KEY=your_production_key
export MPESA_CONSUMER_SECRET=your_production_secret
export MPESA_SHORTCODE=your_till_or_paybill
export MPESA_PASSKEY=your_production_passkey
export MPESA_BASE_URL=https://api.safaricom.co.ke
export MPESA_CALLBACK_URL=https://yourdomain.com/api/v1/orders/payments/mpesa/callback
```

Production requires:
- A registered Safaricom Daraja app
- A valid M-Pesa till number or paybill
- HTTPS callback URL on a public domain
- Safaricom IP allowlisting for callbacks

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Callback not received | ngrok not running | Start ngrok, update callback URL |
| 401 on callback | Wrong credentials | Check consumer key/secret |
| STK push not received | Wrong phone number | Ensure valid Safaricom number |
| Timeout error | Passkey mismatch | Verify passkey matches Daraja app |
| "Request cancelled by user" | User didn't enter PIN | Sandbox timeout after 60s |
