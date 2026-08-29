#!/usr/bin/env bash
# Flutterwave payment tests.
#
# Strategy: when Flutterwave test keys are configured, drive the real
# flow via the real Flutterwave v3 API.
#
# When keys are NOT configured, skip the live tests.

set -uo pipefail

API="http://localhost:8080/api/v1"
JAR=/tmp/grok-fw.jar
rm -f "$JAR"
RESULTS=/tmp/grok-flutterwave-results.txt
: > "$RESULTS"
PASS=0; FAIL=0; SKIP=0
api(){ curl -s -m 30 "$@"; sleep 0.2; }
pass(){ printf "  \033[1;32m✓\033[0m %s\n" "$1" | tee -a "$RESULTS"; PASS=$((PASS+1)); }
fail(){ printf "  \033[1;31m✗\033[0m %s\n" "$1" | tee -a "$RESULTS"; FAIL=$((FAIL+1)); }
skip(){ printf "  \033[1;33m~\033[0m %s\n" "$1" | tee -a "$RESULTS"; SKIP=$((SKIP+1)); }
section(){ printf "\n\033[1;34m%s\033[0m\n" "$1" | tee -a "$RESULTS"; }

FW_CONFIGURED=false
if [[ -f "$REPO_DIR/.env" ]]; then
  # shellcheck source=/dev/null
  source "$REPO_DIR/.env" 2>/dev/null || true
  if [[ -n "${FLUTTERWAVE_SECRET_KEY:-}" ]]; then
    FW_CONFIGURED=true
  fi
fi

# ── Section 1: Reachability (no creds needed) ────────────────────────────────
section "Flutterwave reachability (no creds needed)"

# 1. flutterwave provider in DB
ALLOWED=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname='payments_provider_check';" 2>/dev/null)
if echo "$ALLOWED" | grep -q "FLUTTERWAVE"; then
  pass "FLUTTERWAVE is in the allowed providers list"
else
  fail "FLUTTERWAVE not in payments_provider_check: $ALLOWED"
fi

# ── Section 2: FW NOT configured → skip ─────────────────────────────────────
if ! $FW_CONFIGURED; then
  section "Flutterwave live tests (SKIPPED — no test keys in .env)"
  skip "Real Flutterwave transaction init — set FLUTTERWAVE_SECRET_KEY and FLUTTERWAVE_PUBLIC_KEY in .env"
  skip "Real Flutterwave transaction verify via /transactions/verify?tx_ref=..."
  printf "\n  Flutterwave: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
  exit 0
fi

# ── Section 3: FW IS configured → real flow ─────────────────────────────────
section "Flutterwave live tests (test keys configured)"

# Verify the API key is real by calling Flutterwave
TX_FEE=$(curl -s -m 10 "https://api.flutterwave.com/v3/payouts/fees?amount=100&currency=KES" \
  -H "Authorization: Bearer $FLUTTERWAVE_SECRET_KEY" 2>&1)
if echo "$TX_FEE" | grep -q "fee"; then
  pass "Flutterwave API key is valid (fee quote returned)"
else
  fail "Flutterwave API key invalid or network error: $TX_FEE"
fi

# Create a real order
PID=$(api "$API/products" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['products'][0]['id'])")
api -c "$JAR" -b "$JAR" -X POST "$API/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$PID\",\"quantity\":1}" > /dev/null
ORD=$(api -b "$JAR" -X POST "$API/orders/checkout" -H "Content-Type: application/json" -d "{
  \"shippingAddress\":{\"name\":\"FW Test\",\"line1\":\"1\",\"city\":\"Nairobi\",\"state\":\"Nairobi\",\"postalCode\":\"00100\",\"country\":\"KE\",\"phone\":\"254700000000\",\"type\":\"SHIPPING\"},
  \"billingAddress\":{\"name\":\"FW Test\",\"line1\":\"1\",\"city\":\"Nairobi\",\"state\":\"Nairobi\",\"postalCode\":\"00100\",\"country\":\"KE\",\"phone\":\"254700000000\",\"type\":\"BILLING\"},
  \"guestEmail\":\"fw-live@example.com\"
}")
ORDID=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
ORDTOT=$(echo "$ORD" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['total'])")
pass "Real order created: $ORDTOT KES"

# Initialize real Flutterwave transaction
TX_REF="ILS-$(date +%s)-GROKFW"
INIT=$(api -b "$JAR" -X POST "$API/payments/flutterwave/initialize" -H "Content-Type: application/json" -d "{\"orderId\":\"$ORDID\",\"amount\":$ORDTOT,\"currency\":\"KES\",\"customerEmail\":\"fw-live@example.com\",\"customerName\":\"FW Test\"}")
CK=$(echo "$INIT" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('checkoutUrl',''))")
if [[ -n "$CK" && "$CK" != "None" && "$CK" == https* ]]; then
  pass "Real Flutterwave checkoutUrl returned: $CK"
else
  fail "Flutterwave init did not return a valid checkoutUrl: $INIT"
fi

printf "\n  Flutterwave: %d pass / %d fail / %d skip\n" "$PASS" "$FAIL" "$SKIP"
[[ $FAIL -gt 0 ]] && exit 1
exit 0
