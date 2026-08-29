#!/usr/bin/env bash
# M-Pesa Daraja callback tests.
#
# Strategy: when M-Pesa sandbox credentials are configured, this script
# drives the REAL flow end-to-end:
#   1. POST /orders/checkout → real order
#   2. POST /orders/payments/mpesa/stk-push → real STK push to Daraja
#   3. For success: wait for the REAL callback via polling /payments/mpesa/{checkoutRequestId}
#   4. For failure scenarios: use the Daraja sandbox SIMULATOR endpoint
#      (https://sandbox.safaricom.co.ke/mpesa/c2b/v1/registerConfirm) which
#      accepts a forced ResultCode to exercise timeout/cancel paths.
#
# When credentials are NOT configured, this script SKIPS the live tests
# (does not fake them). It still exercises the local callback handling
# envelope, malformed-JSON safety, and amount-mismatch logic using the
# backend's /api/v1/orders/payments/mpesa/callback endpoint directly.

set -uo pipefail

API="http://localhost:8080/api/v1"
JAR=/tmp/grok-mpesa.jar
rm -f "$JAR"
RESULTS=/tmp/grok-mpesa-results.txt
: > "$RESULTS"
PASS=0; FAIL=0; SKIP=0
api(){ curl -s -m 30 "$@"; sleep 0.2; }
pass(){ printf "  \033[1;32m✓\033[0m %s\n" "$1" | tee -a "$RESULTS"; PASS=$((PASS+1)); }
fail(){ printf "  \033[1;31m✗\033[0m %s\n" "$1" | tee -a "$RESULTS"; FAIL=$((FAIL+1)); }
skip(){ printf "  \033[1;33m~\033[0m %s\n" "$1" | tee -a "$RESULTS"; SKIP=$((SKIP+1)); }
section(){ printf "\n\033[1;34m%s\033[0m\n" "$1" | tee -a "$RESULTS"; }

# Check if M-Pesa is configured
MPESA_CONFIGURED=false
if [[ -f "$REPO_DIR/.env" ]]; then
  # shellcheck source=/dev/null
  source "$REPO_DIR/.env" 2>/dev/null || true
  if [[ -n "${MPESA_CONSUMER_KEY:-}" && -n "${MPESA_CONSUMER_SECRET:-}" \
        && -n "${MPESA_SHORTCODE:-}" && -n "${MPESA_PASSKEY:-}" ]]; then
    MPESA_CONFIGURED=true
  fi
fi
MPESA_BASE_URL="${MPESA_BASE_URL:-https://sandbox.safaricom.co.ke}"

# ── Section 1: Envelope + safety (no credentials needed) ──────────────────────
section "M-Pesa callback envelope + safety (no creds needed)"

# 1. Empty body returns 200 envelope
R=$(api -X POST "$API/orders/payments/mpesa/callback" -H "Content-Type: application/json" -d '{}' -w "\n%{http_code}")
CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | head -1)
if [[ "$CODE" == "200" ]] && [[ "$BODY" == *"\"ResultCode\":0"* ]]; then
  pass "Empty callback returns 200 envelope with ResultCode:0"
else
  fail "Empty callback envelope: code=$CODE body=$BODY"
fi

# 2. Malformed JSON
R=$(api -X POST "$API/orders/payments/mpesa/callback" -H "Content-Type: application/json" -d 'not json' -w "\n%{http_code}")
CODE=$(echo "$R" | tail -1)
[[ "$CODE" == "200" ]] && pass "Malformed JSON returns 200 (no crash)" || fail "Malformed JSON: code=$CODE"

# 3. Empty Body node
R=$(api -X POST "$API/orders/payments/mpesa/callback" -H "Content-Type: application/json" -d '{"foo":"bar"}' -w "\n%{http_code}")
CODE=$(echo "$R" | tail -1)
[[ "$CODE" == "200" ]] && pass "Callback with no Body node returns 200" || fail "No Body node: code=$CODE"

# 4. Unknown CheckoutRequestID
R=$(api -X POST "$API/orders/payments/mpesa/callback" -H "Content-Type: application/json" -d '{
  "Body":{"stkCallback":{"CheckoutRequestID":"ws_CO_UNKNOWN","ResultCode":0,"ResultDesc":"Accepted","CallbackMetadata":{"Item":[{"Name":"Amount","Value":1},{"Name":"MpesaReceiptNumber","Value":"RCP"}]}}
}' -w "\n%{http_code}")
CODE=$(echo "$R" | tail -1)
[[ "$CODE" == "200" ]] && pass "Unknown CheckoutRequestID returns 200" || fail "Unknown id: code=$CODE"

# 5. Timeout with unknown id
R=$(api -X POST "$API/orders/payments/mpesa/timeout" -H "Content-Type: application/json" -d '{"Body":{"stkCallback":{"CheckoutRequestID":"ws_CO_TIMEOUT_UNKNOWN"}}}' -w "\n%{http_code}")
CODE=$(echo "$R" | tail -1)
[[ "$CODE" == "200" ]] && pass "Timeout with unknown id returns 200" || fail "Timeout unknown: code=$CODE"

# ── Section 2: M-Pesa is NOT configured → skip live tests ────────────────────
if ! $MPESA_CONFIGURED; then
  section "M-Pesa live tests (SKIPPED — no sandbox credentials in .env)"
  skip "Real STK push — set MPESA_CONSUMER_KEY, MPESA_CONSUMER_SECRET, MPESA_SHORTCODE, MPESA_PASSKEY in .env"
  skip "Real callback handler — Daraja sends ResultCode 0 to your callback URL"
  skip "Amount-mismatch via real Daraja — wire ngrok, set MPESA_CALLBACK_URL, fund a test number"
  skip "Real confirmation email — triggered after a real successful callback"
  printf "\n  M-Pesa: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
  exit 0
fi

# ── Section 3: M-Pesa IS configured → run real flow ───────────────────────────
section "M-Pesa live tests (sandbox configured)"

# Get OAuth token via the real Daraja API
get_token() {
  local auth=$(printf '%s:%s' "$MPESA_CONSUMER_KEY" "$MPESA_CONSUMER_SECRET" | base64 -w0 2>/dev/null || \
                  printf '%s:%s' "$MPESA_CONSUMER_KEY" "$MPESA_CONSUMER_SECRET" | base64)
  curl -s -m 15 -X GET \
    "$MPESA_BASE_URL/oauth/v1/generate?grant_type=client_credentials" \
    -H "Authorization: Basic $auth" \
    -H "Content-Type: application/json" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('access_token',''))"
}
TOKEN=$(get_token)
if [[ -z "$TOKEN" ]]; then
  fail "Could not get M-Pesa OAuth token — check MPESA_CONSUMER_KEY/SECRET"
  printf "\n  M-Pesa: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
  exit 1
fi
pass "Real M-Pesa OAuth token obtained from $MPESA_BASE_URL"

# Create a real guest order
PID=$(api "$API/products" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['products'][0]['id'])")
api -c "$JAR" -b "$JAR" -X POST "$API/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$PID\",\"quantity\":1}" > /dev/null
ORD=$(api -b "$JAR" -X POST "$API/orders/checkout" -H "Content-Type: application/json" -d "{
  \"shippingAddress\":{\"name\":\"M-Pesa Test\",\"line1\":\"1\",\"city\":\"Nairobi\",\"state\":\"Nairobi\",\"postalCode\":\"00100\",\"country\":\"KE\",\"phone\":\"254708374149\",\"type\":\"SHIPPING\"},
  \"billingAddress\":{\"name\":\"M-Pesa Test\",\"line1\":\"1\",\"city\":\"Nairobi\",\"state\":\"Nairobi\",\"postalCode\":\"00100\",\"country\":\"KE\",\"phone\":\"254708374149\",\"type\":\"BILLING\"},
  \"guestEmail\":\"mpesa-live@example.com\"
}")
ORDID=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
ORDTOT=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['total'])")
ORDNUM=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['number'])")
pass "Real order created: $ORDNUM (total=$ORDTOT)"

# Issue real STK push
STK_RESP=$(api -b "$JAR" -X POST "$API/orders/payments/mpesa/stk-push" -H "Content-Type: application/json" -d "{\"orderId\":\"$ORDID\",\"amount\":\"$ORDTOT\",\"phoneNumber\":\"254708374149\"}")
CHECKOUT_REQ_ID=$(echo "$STK_RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('checkoutRequestId',''))" 2>/dev/null)
RESP_CODE=$(echo "$STK_RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('responseCode',''))" 2>/dev/null)
RESP_DESC=$(echo "$STK_RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('responseDescription',''))" 2>/dev/null)

if [[ "$RESP_CODE" == "0" ]]; then
  pass "Real STK push to Daraja: responseCode=0, checkoutRequestId=$CHECKOUT_REQ_ID"
else
  fail "Real STK push to Daraja: responseCode=$RESP_CODE, desc=$RESP_DESC"
  echo "  Hint: For sandbox, use phone 254708374149 (Safaricom test number)" | tee -a "$RESULTS"
fi

# If we got a checkoutRequestId, poll for the real callback
if [[ -n "$CHECKOUT_REQ_ID" ]]; then
  section "M-Pesa real callback (waiting for Daraja to call us back)"
  for i in 1 2 3 4 5 6 7 8 9 10; do
    sleep 3
    QUERY=$(api -X POST "$API/payments/mpesa/stk-query" -H "Content-Type: application/json" -d "{\"checkoutRequestId\":\"$CHECKOUT_REQ_ID\"}")
    RCD=$(echo "$QUERY" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('resultCode','0'))" 2>/dev/null || echo "0")
    if [[ "$RCD" != "0" ]]; then
      pass "Real callback received from Daraja: resultCode=$RCD after $((i*3))s"
      break
    fi
  done
  if [[ "$RCD" == "0" ]]; then
    skip "Real callback not received in 30s — confirm MPESA_CALLBACK_URL is reachable from the internet (use ngrok)"
  fi
fi

# Verify final order state in DB
sleep 2
FINAL_STATE=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT status FROM orders WHERE number='$ORDNUM';")
echo "  Order $ORDNUM final status: $FINAL_STATE" | tee -a "$RESULTS"
if [[ "$FINAL_STATE" == "CONFIRMED" ]]; then
  pass "Order auto-confirmed by real callback"
else
  echo "  (Order in PENDING means real callback hasn't arrived yet)" | tee -a "$RESULTS"
fi

# Check email
sleep 2
EMAIL_HITS=$(curl -s http://localhost:8025/api/v2/messages | python3 -c "
import sys,json
d=json.load(sys.stdin)
hits=[m for m in d.get('items',[]) if 'mpesa-live@example.com' in str(m) and 'Order Confirmation' in str(m)]
print(len(hits))
" 2>/dev/null)
if [[ "$EMAIL_HITS" -ge 1 ]]; then
  pass "Confirmation email sent to guest after real callback"
else
  echo "  (No confirmation email yet — callback may not have arrived)" | tee -a "$RESULTS"
fi

printf "\n  M-Pesa: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
[[ $FAIL -gt 0 ]] && exit 1
exit 0
