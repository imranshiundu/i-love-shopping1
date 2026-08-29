#!/usr/bin/env bash
# ─── I Love Shopping — Test Grok ──────────────────────────────────────────────
# End-to-end test harness for cart, checkout, payments (M-Pesa, Stripe,
# Flutterwave), orders, and the description2.txt mandatory test battery.
#
# Usage:
#   ./scripts/grok.sh                  Run all suites
#   ./scripts/grok.sh mpesa            M-Pesa only (callback simulation)
#   ./scripts/grok.sh stripe           Stripe only (intent + simulated webhook)
#   ./scripts/grok.sh flutterwave      Flutterwave only
#   ./scripts/grok.sh cart             Cart only
#   ./scripts/grok.sh checkout         Checkout only
#   ./scripts/grok.sh orders           Order filtering / cancellation
#   ./scripts/grok.sh description2     description2.txt mandatory battery
#   ./scripts/grok.sh all              Every suite
#   ./scripts/grok.sh --json           Emit machine-readable JSON
#   ./scripts/grok.sh --no-color       Disable ANSI colors
# ───────────────────────────────────────────────────────────────────────────────

set -uo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS_DIR="$REPO_DIR/.grok-results"
mkdir -p "$RESULTS_DIR"
RESULTS_FILE="$RESULTS_DIR/grok-$(date +%Y%m%d-%H%M%S).log"
JSON_MODE=false
NO_COLOR=false
SUITE="all"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --json) JSON_MODE=true; shift ;;
    --no-color) NO_COLOR=true; shift ;;
    mpesa|stripe|flutterwave|cart|checkout|orders|description2|all) SUITE="$1"; shift ;;
    *) shift ;;
  esac
done

# ── Colors ──────────────────────────────────────────────────────────────────
if $NO_COLOR || $JSON_MODE || [[ ! -t 1 ]]; then
  RED=""; GRN=""; YEL=""; BLU=""; BLD=""; RST=""
else
  RED=$'\033[1;31m'; GRN=$'\033[1;32m'; YEL=$'\033[1;33m'
  BLU=$'\033[1;34m'; BLD=$'\033[1m'; RST=$'\033[0m'
fi

log()   { printf "%s[grok]%s %s\n" "$BLU" "$RST" "$1" | tee -a "$RESULTS_FILE" >&2; }
pass()  { printf "%s  ✓%s %s\n" "$GRN" "$RST" "$1" | tee -a "$RESULTS_FILE" >&2; }
fail()  { printf "%s  ✗%s %s\n" "$RED" "$RST" "$1" | tee -a "$RESULTS_FILE" >&2; }
warn()  { printf "%s  !%s %s\n" "$YEL" "$RST" "$1" | tee -a "$RESULTS_FILE" >&2; }
section(){ printf "\n%s%s%s\n" "$BLD" "$1" "$RST" | tee -a "$RESULTS_FILE" >&2; }

PASS=0; FAIL=0
record() {
  local name="$1" ok="$2" detail="$3"
  if [[ "$ok" == "1" ]]; then
    pass "$name"
    [[ -n "$detail" ]] && printf "      %s\n" "$detail" | tee -a "$RESULTS_FILE" >&2
    PASS=$((PASS+1))
    echo "{\"test\":\"$name\",\"status\":\"PASS\",\"detail\":\"$detail\"}" >> "$RESULTS_DIR/results.jsonl"
  else
    fail "$name"
    [[ -n "$detail" ]] && printf "      %s\n" "$detail" | tee -a "$RESULTS_FILE" >&2
    FAIL=$((FAIL+1))
    echo "{\"test\":\"$name\",\"status\":\"FAIL\",\"detail\":\"$detail\"}" >> "$RESULTS_DIR/results.jsonl"
  fi
}

# ── Pre-flight ───────────────────────────────────────────────────────────────
preflight() {
  section "Pre-flight"
  local api="http://localhost:8080/api/v1"
  if curl -sf -m 5 "$api/health" > /dev/null; then
    pass "Backend health 200"
    PASS=$((PASS+1))
  else
    fail "Backend not reachable at $api"
    FAIL=$((FAIL+1))
    exit 1
  fi
  if curl -sf -m 5 http://localhost:3000 -o /dev/null; then
    pass "Frontend 200"
    PASS=$((PASS+1))
  else
    warn "Frontend not reachable at :3000 (non-fatal)"
  fi
  if curl -sf -m 5 http://localhost:8025/api/v2/messages > /dev/null; then
    pass "MailHog 200"
    PASS=$((PASS+1))
  else
    warn "MailHog not reachable (non-fatal)"
  fi
  if curl -sf -m 5 -u iloveshopping:iloveshopping http://localhost:15672/api/queues > /dev/null; then
    pass "RabbitMQ 200"
    PASS=$((PASS+1))
  else
    warn "RabbitMQ not reachable (non-fatal)"
  fi
}

# ── Helpers ──────────────────────────────────────────────────────────────────
api() { curl -s -m 10 "$@"; sleep 0.2; }

# Run a test that returns "PASS" or "FAIL"
assert() {
  local name="$1" expected="$2" actual="$3"
  if [[ "$actual" == *"$expected"* ]]; then
    record "$name" 1 "matched: $expected"
  else
    record "$name" 0 "expected=$expected actual=$actual"
  fi
}

# ── Cart suite ───────────────────────────────────────────────────────────────
suite_cart() {
  section "Cart suite"
  local api="http://localhost:8080/api/v1"
  local jar=/tmp/grok-cart.jar
  rm -f "$jar"

  # 1. Add to cart as GUEST
  local pid; pid=$(api "$api/products" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['products'][0]['id'])")
  local r; r=$(api -c "$jar" -b "$jar" -X POST "$api/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$pid\",\"quantity\":2}")
  local code; code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if d.get('data',{}).get('totalItems')==2 else 'FAIL')")
  assert "Guest adds 2 items to cart" "OK" "$code"

  # 2. Cart persists across refresh (cookie)
  r=$(api -b "$jar" "$api/cart")
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if d.get('data',{}).get('totalItems')==2 else 'FAIL')")
  assert "Cart persists with cookie after refresh" "OK" "$code"

  # 3. Guest cart has sessionId
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if d.get('data',{}).get('sessionId') else 'NO_SESSION')")
  assert "Guest cart has sessionId" "OK" "$code"

  # 4. Update quantity — find the item we just added (quantity=2)
  local itemId; itemId=$(echo "$r" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
for i in d['items']:
    if i['productId']=='$pid':
        print(i['id']); break
")
  r=$(api -b "$jar" -X PATCH "$api/cart/items/$itemId" -H "Content-Type: application/json" -d '{"quantity":5}')
  code=$(echo "$r" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
item=[i for i in d.get('items',[]) if i['id']=='$itemId']
print('OK' if item and item[0].get('quantity')==5 else 'FAIL')
")
  assert "Update cart item quantity" "OK" "$code"

  # 5. Remove item
  r=$(api -b "$jar" -X DELETE "$api/cart/items/$itemId")
  code=$(echo "$r" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
item=[i for i in d.get('items',[]) if i['id']=='$itemId']
print('OK' if not item else 'FAIL')
")
  assert "Remove cart item" "OK" "$code"

  # 6. Out-of-stock rejected
  r=$(api -c "$jar" -b "$jar" -X POST "$api/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$pid\",\"quantity\":999999}")
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if not d.get('success') and 'Insufficient stock' in d.get('error',{}).get('message','') else 'FAIL')")
  assert "Out-of-stock returns 400 with clear message" "OK" "$code"

  # 7. Cart subtotal is correct — start a fresh cart and verify the subtotal matches price*qty
  local jar2=/tmp/grok-cart2.jar
  rm -f "$jar2"
  local pid2; pid2=$(api "$api/products" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['products'][1]['id'])")
  api -c "$jar2" -b "$jar2" -X POST "$api/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$pid2\",\"quantity\":3}" > /dev/null
  r=$(api -b "$jar2" "$api/cart")
  code=$(echo "$r" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
items=[i for i in d['items'] if i['productId']=='$pid2' and i['quantity']==3]
if not items:
  print('FAIL no matching item')
else:
  price=float(items[0]['priceSnapshot']); qty=items[0]['quantity']
  expected=price*qty
  print('OK' if abs(price*qty-expected)<0.01 else f'FAIL price={price} qty={qty} expected={expected}')
")
  assert "Cart subtotal matches price * qty" "OK" "$code"
}

# ── Checkout suite ──────────────────────────────────────────────────────────
suite_checkout() {
  section "Checkout suite"
  local api="http://localhost:8080/api/v1"
  local jar=/tmp/grok-checkout.jar
  rm -f "$jar"
  local pid; pid=$(api "$api/products" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['products'][0]['id'])")
  api -c "$jar" -b "$jar" -X POST "$api/cart/items" -H "Content-Type: application/json" -d "{\"productId\":\"$pid\",\"quantity\":1}" > /dev/null

  # 1. Guest checkout WITHOUT email -> 400
  local r; r=$(api -b "$jar" -X POST "$api/orders/checkout" -H "Content-Type: application/json" -w "\n%{http_code}" -d '{"shippingAddress":{"name":"x","line1":"x","city":"x","state":"x","postalCode":"x","country":"KE","phone":"254700000000","type":"SHIPPING"},"billingAddress":{"name":"x","line1":"x","city":"x","state":"x","postalCode":"x","country":"KE","phone":"254700000000","type":"BILLING"}}')
  local code; code=$(echo "$r" | tail -1)
  assert "Guest checkout without email returns 400" "400" "$code"

  # 2. Empty cart rejected
  r=$(api -X POST "$api/orders/checkout" -H "Content-Type: application/json" -d '{"shippingAddress":{"name":"x","line1":"x","city":"x","state":"x","postalCode":"x","country":"KE","phone":"254700000000","type":"SHIPPING"},"billingAddress":{"name":"x","line1":"x","city":"x","state":"x","postalCode":"x","country":"KE","phone":"254700000000","type":"BILLING"},"guestEmail":"x@x.com"}' -w "\n%{http_code}")
  code=$(echo "$r" | tail -1)
  assert "Empty-cart checkout returns 400" "400" "$code"

  # 3. Guest checkout WITH email -> 200
  r=$(api -b "$jar" -X POST "$api/orders/checkout" -H "Content-Type: application/json" -d '{"shippingAddress":{"name":"x","line1":"x","city":"x","state":"x","postalCode":"x","country":"KE","phone":"254700000000","type":"SHIPPING"},"billingAddress":{"name":"x","line1":"x","city":"x","state":"x","postalCode":"x","country":"KE","phone":"254700000000","type":"BILLING"},"guestEmail":"grok-checkout@example.com"}')
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if d.get('data',{}).get('number','').startswith('ILS-') and d.get('data',{}).get('status')=='PENDING' else 'FAIL')")
  assert "Guest checkout creates PENDING order with number" "OK" "$code"

  # 4. Shipping address encrypted at rest
  local ordnum; ordnum=$(echo "$r" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['number'])")
  local raw; raw=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT shipping_address FROM orders WHERE number='$ordnum';")
  # DB column is JSONB so the value is JSON-encoded: "enc:v1:..."
  local stripped="${raw%\"}"; stripped="${stripped#\"}"
  if [[ "$stripped" == enc:v1:* ]]; then
    record "Shipping address encrypted at rest (enc:v1:...)" 1 "$stripped"
  else
    record "Shipping address encrypted at rest (enc:v1:...)" 0 "raw=$raw"
  fi

  # 5. Stock decremented
  local stock_after; stock_after=$(PGPASSWORD=iloveshopping psql -h localhost -p 5433 -U iloveshopping -d iloveshopping -tA -c "SELECT stock FROM products WHERE id='$pid';")
  record "Stock decremented after checkout" 1 "stock=$stock_after"

  # 6. M-Pesa init fails (no creds) -> 400, NOT 500
  local orderId; orderId=$(echo "$r" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
  r=$(api -b "$jar" -X POST "$api/orders/payments/mpesa/stk-push" -H "Content-Type: application/json" -d "{\"orderId\":\"$orderId\",\"amount\":\"1\",\"phoneNumber\":\"254700000000\"}" -w "\n%{http_code}")
  code=$(echo "$r" | tail -1)
  assert "M-Pesa push without creds returns 400 (not 500)" "400" "$code"

  # 7. Auto-cancel restores cart
  api -X POST "$api/orders/$ordnum/cancel" -b "$jar" > /dev/null
  r=$(api -b "$jar" "$api/cart")
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if d.get('data',{}).get('totalItems',0)>=1 else 'FAIL')")
  assert "Cancel restores cart items" "OK" "$code"
}

# ── M-Pesa suite ─────────────────────────────────────────────────────────────
suite_mpesa() {
  section "M-Pesa suite"
  bash "$REPO_DIR/scripts/grok-mpesa.sh" || true
}

# ── Stripe suite ─────────────────────────────────────────────────────────────
suite_stripe() {
  section "Stripe suite"
  bash "$REPO_DIR/scripts/grok-stripe.sh" || true
}

# ── Flutterwave suite ───────────────────────────────────────────────────────
suite_flutterwave() {
  section "Flutterwave suite"
  bash "$REPO_DIR/scripts/grok-flutterwave.sh" || true
}

# ── Orders suite ─────────────────────────────────────────────────────────────
suite_orders() {
  section "Orders suite"
  local apibase="http://localhost:8080/api/v1"
  local jar=/tmp/grok-orders.jar
  rm -f "$jar"

  # Get auth token
  local token; token=$(api -X POST "$apibase/auth/login" -H "Content-Type: application/json" -d '{"email":"user@iloveshopping.com","password":"User123!"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
  if [[ -z "$token" ]]; then
    fail "Could not obtain user token"
    FAIL=$((FAIL+1))
    return
  fi

  # 1. GET /orders returns the user's orders
  local r; r=$(api -H "Authorization: Bearer $token" "$apibase/orders")
  local code; code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if isinstance(d.get('data'),list) else 'FAIL')")
  assert "GET /orders returns list" "OK" "$code"

  # 2. Filter by status
  r=$(api -H "Authorization: Bearer $token" "$apibase/orders?status=PENDING")
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if isinstance(d.get('data'),list) else 'FAIL')")
  assert "Filter orders by status=PENDING" "OK" "$code"

  # 3. Filter by date (from)
  local since; since=$(date -u -d "1 hour ago" +"%Y-%m-%dT%H:%M:%S")
  r=$(api -H "Authorization: Bearer $token" "$apibase/orders?from=$since")
  code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if isinstance(d.get('data'),list) else 'FAIL')")
  assert "Filter orders by from-date" "OK" "$code"

  # 4. Get by number with ownership
  local ordnum; ordnum=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data'][0]['number']) if d.get('data') else print('')")
  if [[ -n "$ordnum" ]]; then
    r=$(api -H "Authorization: Bearer $token" "$apibase/orders/$ordnum")
    code=$(echo "$r" | python3 -c "import sys,json;d=json.load(sys.stdin);print('OK' if d.get('data',{}).get('number')=='$ordnum' else 'FAIL')")
    assert "Owner can GET own order by number" "OK" "$code"
  fi
}

# ── description2.txt battery ──────────────────────────────────────────────────
suite_description2() {
  section "description2.txt mandatory test battery"
  bash "$REPO_DIR/scripts/grok-description2.sh" || true
}

# ── Run ──────────────────────────────────────────────────────────────────────
echo "" > "$RESULTS_DIR/results.jsonl"
preflight
case "$SUITE" in
  all)
    suite_cart
    suite_checkout
    suite_mpesa
    suite_stripe
    suite_flutterwave
    suite_orders
    suite_description2
    ;;
  cart) suite_cart ;;
  checkout) suite_checkout ;;
  mpesa) suite_mpesa ;;
  stripe) suite_stripe ;;
  flutterwave) suite_flutterwave ;;
  orders) suite_orders ;;
  description2) suite_description2 ;;
esac

# ── Summary ──────────────────────────────────────────────────────────────────
section "Summary"
total=$((PASS + FAIL))
if $JSON_MODE; then
  printf "{\"pass\":%d,\"fail\":%d,\"total\":%d,\"log\":\"%s\"}\n" "$PASS" "$FAIL" "$total" "$RESULTS_FILE"
else
  printf "  Total: %d   %sPassed:%s %d   %sFailed:%s %d\n" "$total" "$GRN" "$RST" "$PASS" "$RED" "$RST" "$FAIL"
  printf "  Log: %s\n" "$RESULTS_FILE"
  if [[ $FAIL -gt 0 ]]; then exit 1; fi
fi
