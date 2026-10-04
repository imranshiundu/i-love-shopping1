# Payments

## M-Pesa Payments (African markets only)

- ✅ C2B Payments
- ✅ STK Push initiation
- ✅ Callback processing (success/failure/timeout)
- ✅ Payment status polling
- ✅ Retry failed payments
- ✅ Payment metadata storage

## Card Payments (Stripe)
- ✅ Stripe PaymentIntent create + confirm (test/live mode)
- ✅ Stripe webhook handling (`payment_intent.succeeded`, `payment_intent.payment_failed`)
- ✅ Test card: Stripe `4242 4242 4242 4242`
- ✅ No card data ever touches the server - PCI-friendly tokenized flow
- ✅ Failure scenarios: declined cards, gateway errors, invalid payment IDs

## Payments
- **Two payment rails at checkout** - M-Pesa Daraja (mobile money, **only in supported African markets**: Kenya, Tanzania, DRC, Mozambique, Lesotho, Ghana, Ethiopia) and card via Stripe (testable worldwide). Both selectable in a single payment step. (Flutterwave/Airtel entries were removed: enum-only placeholders with no implementation. Stripe covers Visa/Mastercard worldwide including Kenya; M-Pesa covers mobile money.)
- **Real M-Pesa Daraja** - STK push via Safaricom sandbox/production. Configure keys in `.env`. Callback polling fallback for local dev.
- **STK expiry watchdog** - Safaricom phone prompts last ~60–120s. A scheduled job (`MPESA_STK_TIMEOUT_SECONDS`, default 120) auto-marks unanswered STK sessions FAILED so orders never get stuck.
- **Pay-later invoices** - every unpaid order triggers a payable invoice email (`/checkout?retry=ORDER-NUMBER`); fresh invoices are re-sent on every failed/expired payment. Customers can leave mid-payment and resume anytime.
- **Unpaid order self-service** - customers can retry payment (PENDING/EXPIRED/CANCELLED with stock re-check), cancel (restores stock + cart), or permanently delete unpaid orders from their account.
- **Client-side card validation** - Luhn check, expiry and CVV validation happen in the browser before any request; card data is never sent to or stored on our servers.

## Payment test keys (sandbox)

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

## Going live with a real bank card

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
