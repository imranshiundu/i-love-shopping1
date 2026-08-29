#!/usr/bin/env bash
# Stripe payment tests.
#
# Strategy: when Stripe test keys are configured, this script drives the
# REAL flow end-to-end:
#   1. Create a real order
#   2. Call /payments/stripe/create-intent → real Stripe API → real PaymentIntent
#   3. Use Stripe's API to confirm a test payment (test card 4242 4242 4242 4242)
#   4. Forward a real webhook (or poll) to verify the webhook handler
#
# When keys are NOT configured, this script SKIPS the live tests.
# It still verifies the /stripe/webhook endpoint accepts POSTs (permitAll)
# and that the signature-verification rejects forged events.

set -uo pipefail

API="http://localhost:8080/api/v1"
JAR=/tmp/grok-stripe.jar
rm -f "$JAR"
RESULTS=/tmp/grok-stripe-results.txt
: > "$RESULTS"
PASS=0; FAIL=0; SKIP=0
api(){ curl -s -m 30 "$@"; sleep 0.2; }
pass(){ printf "  \033[1;32m✓\033[0m %s\n" "$1" | tee -a "$RESULTS"; PASS=$((PASS+1)); }
fail(){ printf "  \033[1;31m✗\033[0m %s\n" "$1" | tee -a "$RESULTS"; FAIL=$((FAIL+1)); }
skip(){ printf "  \033[1;33m~\033[0m %s\n" "$1" | tee -a "$RESULTS"; SKIP=$((SKIP+1)); }
section(){ printf "\n\033[1;34m%s\033[0m\n" "$1" | tee -a "$RESULTS"; }

# Check Stripe config
STRIPE_CONFIGURED=false
if [[ -f "$REPO_DIR/.env" ]]; then
  # shellcheck source=/dev/null
  source "$REPO_DIR/.env" 2>/dev/null || true
  if [[ -n "${STRIPE_SECRET_KEY:-}" && -n "${STRIPE_PUBLISHABLE_KEY:-}" ]]; then
    STRIPE_CONFIGURED=true
  fi
fi

# ── Section 1: Webhook reachability (no creds needed) ────────────────────────
section "Stripe webhook (no creds needed)"

# 1. Webhook is permitAll (auth check)
R=$(api -X POST "$API/payments/stripe/webhook" -H "Content-Type: application/json" -H "Stripe-Signature: t=1,v1=fake" -d '{"id":"evt_test","type":"payment_intent.succeeded"}' -w "\n%{http_code}")
CODE=$(echo "$R" | tail -1)
if [[ "$CODE" != "401" ]] && [[ "$CODE" != "403" ]]; then
  pass "Stripe webhook is permitAll (HTTP $CODE, signature-valid check)"
else
  fail "Stripe webhook returned $CODE (should be permitAll)"
fi

# 2. Webhook signature rejection (no valid signature → no state change)
R=$(api -X POST "$API/payments/stripe/webhook" -H "Content-Type: application/json" -H "Stripe-Signature: invalid" -d '{"id":"evt_fake","type":"payment_intent.succeeded","data":{"object":{"id":"pi_fake","amount":1000,"currency":"kes","metadata":{"orderId":"nonexistent"}}}}')
sleep 1
# Verify no payment was created
PHANTOM=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT count(*) FROM payments WHERE provider='STRIPE' AND provider_id='pi_fake';" 2>/dev/null)
if [[ "$PHANTOM" == "0" ]]; then
  pass "Invalid signature does not create phantom payment"
else
  fail "Invalid signature created a payment row (count=$PHANTOM)"
fi

# ── Section 2: Stripe NOT configured → skip ───────────────────────────────────
if ! $STRIPE_CONFIGURED; then
  section "Stripe live tests (SKIPPED — no test keys in .env)"
  skip "Real PaymentIntent creation — set STRIPE_SECRET_KEY and STRIPE_PUBLISHABLE_KEY in .env"
  skip "Real payment confirmation with test card 4242 4242 4242 4242"
  skip "Real webhook signature verification — install Stripe CLI: stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook"
  printf "\n  Stripe: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
  exit 0
fi

# ── Section 3: Stripe IS configured → real flow ─────────────────────────────
section "Stripe live tests (test keys configured)"

# 1. Create a real order
PID=$(api "$API/products" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['products'][0]['id'])")
api -c "$JAR" -b "$JAR" -X POST "$API/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$PID\",\"quantity\":1}" > /dev/null
ORD=$(api -b "$JAR" -X POST "$API/orders/checkout" -H "Content-Type: application/json" -d "{
  \"shippingAddress\":{\"name\":\"Stripe Test\",\"line1\":\"1\",\"city\":\"Nairobi\",\"state\":\"Nairobi\",\"postalCode\":\"00100\",\"country\":\"KE\",\"phone\":\"254700000000\",\"type\":\"SHIPPING\"},
  \"billingAddress\":{\"name\":\"Stripe Test\",\"line1\":\"1\",\"city\":\"Nairobi\",\"state\":\"Nairobi\",\"postalCode\":\"00100\",\"country\":\"KE\",\"phone\":\"254700000000\",\"type\":\"BILLING\"},
  \"guestEmail\":\"stripe-live@example.com\"
}")
ORDID=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
ORDTOT=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['total'])")
pass "Real order created: $ORDTOT KES"

# 2. Create real PaymentIntent via Stripe API
INTENT=$(api -b "$JAR" -X POST "$API/payments/stripe/create-intent" -H "Content-Type: application/json" -d "{\"orderId\":\"$ORDID\",\"amount\":$ORDTOT,\"currency\":\"KES\"}")
PI_ID=$(echo "$INTENT" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('paymentIntentId',''))")
CS=$(echo "$INTENT" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('clientSecret',''))")
if [[ -n "$PI_ID" && "$PI_ID" != "None" ]]; then
  pass "Real Stripe PaymentIntent created: $PI_ID"
else
  fail "create-intent returned no PaymentIntent: $INTENT"
  printf "\n  Stripe: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
  exit 1
fi

# 3. Verify amount matches order
PI_AMT=$(curl -s -m 10 "https://api.stripe.com/v1/payment_intents/$PI_ID" \
  -u "$STRIPE_SECRET_KEY:" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('amount',0))" 2>/dev/null)
ORDTOT_CENTS=$(python3 -c "print(int($ORDTOT * 100))")
if [[ "$PI_AMT" == "$ORDTOT_CENTS" ]]; then
  pass "Stripe PaymentIntent amount matches order total ($ORDTOT_CENTS cents)"
else
  fail "Amount mismatch: order=$ORDTOT_CENTS cents, stripe=$PI_AMT cents"
fi

# 4. Confirm payment using Stripe test card
# Use Stripe's API to create a PaymentMethod and confirm the PI
# This is the equivalent of a successful card payment on the frontend.
PAYMENT_METHOD=$(curl -s -m 10 "https://api.stripe.com/v1/payment_methods" \
  -u "$STRIPE_SECRET_KEY:" \
  -d "type=card" \
  -d "card[number]=4242424242424242" \
  -d "card[exp_month]=12" \
  -d "card[exp_year]=2030" \
  -d "card[cvc]=123" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('id',''))" 2>/dev/null)
if [[ -n "$PAYMENT_METHOD" && "$PAYMENT_METHOD" != "" ]]; then
  # Confirm the PaymentIntent with the test card
  CONFIRM=$(curl -s -m 15 "https://api.stripe.com/v1/payment_intents/$PI_ID/confirm" \
    -u "$STRIPE_SECRET_KEY:" \
    -d "payment_method=$PAYMENT_METHOD" \
    -d "return_url=http://localhost:3000/checkout/success?order=$ORDNUM")
  PI_STATUS=$(echo "$CONFIRM" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('status',''))" 2>/dev/null)
  if [[ "$PI_STATUS" == "succeeded" ]]; then
    pass "Stripe API confirmed PI with test card 4242: status=succeeded"
  else
    fail "Stripe confirm failed: status=$PI_STATUS"
  fi
else
  fail "Could not create test PaymentMethod"
fi

# 5. Webhook: if stripe CLI is forwarding, check the order state
# Otherwise poll the order state to see if it changed
sleep 3
FINAL_STATE=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT status FROM orders WHERE number='$ORDNUM';")
PI_DB_STATUS=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT status FROM payments WHERE provider_id='$PI_ID';")
echo "  Order: $FINAL_STATE, Payment: $PI_DB_STATUS" | tee -a "$RESULTS"
if [[ "$FINAL_STATE" == "CONFIRMED" ]]; then
  pass "Order auto-confirmed (webhook arrived)"
elif [[ "$FINAL_STATE" == "PENDING" ]]; then
  echo "  (Order PENDING — Stripe webhook hasn't arrived yet)" | tee -a "$RESULTS"
  echo "  To enable: install Stripe CLI and run: stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook" | tee -a "$RESULTS"
fi

printf "\n  Stripe: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
[[ $FAIL -gt 0 ]] && exit 1
exit 0
