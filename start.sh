#!/usr/bin/env bash
# =============================================================
# i-love-shopping — Quick Start (dev runs like production)
# =============================================================
# Boots the full stack with production-identical behavior:
# real JWT rotation, real Daraja sandbox, real Stripe test mode,
# payable invoices, STK watchdog, encrypted PII — only the
# credentials and data volumes are local.
#
# Usage:
#   ./start.sh            Start everything
#   ./start.sh --stop     Stop everything (keeps dev database)
#   ./start.sh --rebuild  Force backend + frontend rebuild
# =============================================================

set -e
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_DIR"

RED=$'\033[1;31m'; GRN=$'\033[1;32m'; YEL=$'\033[1;33m'
BLU=$'\033[1;34m'; RST=$'\033[0m'
log()  { printf "%s[start]%s %s\n" "$BLU" "$RST" "$1"; }
ok()   { printf "%s[start]%s %s\n" "$GRN" "$RST" "$1"; }
warn() { printf "%s[start]%s %s\n" "$YEL" "$RST" "$1"; }
die()  { printf "%s[start]%s %s\n" "$RED" "$RST" "$1"; exit 1; }

STOP=false; REBUILD=false
for arg in "$@"; do
  case "$arg" in
    --stop)     STOP=true ;;
    --rebuild)  REBUILD=true ;;
    --help|-h)
      echo "Usage: ./start.sh [--stop] [--rebuild]"
      echo "  --stop     Stop all services (dev database is kept)"
      echo "  --rebuild  Force backend + frontend rebuild"
      exit 0
      ;;
  esac
done

port_busy() {
  local port=$1
  if command -v ss >/dev/null 2>&1; then
    ss -tln 2>/dev/null | grep -qE "[:.]$port[[:space:]]" && return 0 || return 1
  fi
  (echo > /dev/tcp/127.0.0.1/$port) >/dev/null 2>&1
}

free_port() {
  local port=$1 guard=0
  while port_busy "$port"; do
    port=$((port + 1)); guard=$((guard + 1))
    [ "$guard" -gt 100 ] && die "No free port found — too many services running."
  done
  echo "$port"
}

# ── Stop mode (never deletes volumes: dev data survives) ──
if $STOP; then
  log "Stopping services (database kept)..."
  (cd "$REPO_DIR/docker" && docker compose down --remove-orphans 2>/dev/null) || true
  pkill -f "i-love-shopping-1.0.0-SNAPSHOT.jar" 2>/dev/null || true
  pkill -f "$REPO_DIR/frontend" 2>/dev/null || true
  ok "All stopped. To wipe dev data too: docker compose -f docker/docker-compose.yml down -v"
  exit 0
fi

# ── Check Docker ──
command -v docker >/dev/null 2>&1 || die "Docker is not installed. Install from https://docs.docker.com/get-docker/"
docker info >/dev/null 2>&1 || die "Docker daemon is not running. Start Docker Desktop and try again."

# ── Check .env + audit credentials ──
if [ ! -f ".env" ]; then
  [ -f ".env.example" ] || die ".env.example not found. Please create .env manually."
  cp .env.example .env
  ok "Created .env from .env.example — review it, then re-run."
fi

needs_val() { # needs_val VAR [minlen] — true if missing/too short/placeholder
  local val; val=$(grep -E "^$1=" .env 2>/dev/null | cut -d= -f2-)
  local minlen=${2:-1}
  [ -z "$val" ] && return 0
  [ "${#val}" -lt "$minlen" ] && return 0
  case "$val" in
    change-me*|your-*|dev-access-secret*|dev-refresh-secret*|i-love-shopping-dev-encryption*) return 0 ;;
  esac
  return 1
}
set_val() {
  local key=$1 val=$2
  if grep -qE "^$key=" .env; then
    sed -i "s|^$key=.*|$key=$val|" .env
  else
    printf '%s=%s\n' "$key" "$val" >> .env
  fi
}

if needs_val JWT_ACCESS_SECRET 32; then
  set_val JWT_ACCESS_SECRET "$(openssl rand -hex 32)"
  warn "Generated JWT_ACCESS_SECRET in .env"
fi
if needs_val JWT_REFRESH_SECRET 32; then
  set_val JWT_REFRESH_SECRET "$(openssl rand -hex 32)"
  warn "Generated JWT_REFRESH_SECRET in .env"
fi
if needs_val DATA_ENCRYPTION_KEY 32; then
  set_val DATA_ENCRYPTION_KEY "$(openssl rand -hex 32)"
  warn "Generated DATA_ENCRYPTION_KEY in .env"
fi
for key in MPESA_CONSUMER_KEY MPESA_CONSUMER_SECRET MPESA_SHORTCODE MPESA_PASSKEY; do
  needs_val "$key" && warn "$key is blank — M-Pesa payments will be disabled until set."
done
needs_val STRIPE_SECRET_KEY 10 && warn "STRIPE_SECRET_KEY is blank — card payments will be disabled until set."
[ -f ".env" ] && ok "Credentials checked."

# ── Ports (auto-shift when busy, e.g. another project on 8080/3000) ──
API_PORT=$(free_port "${SERVER_PORT:-8080}")
FE_PORT=$(free_port 3000)
[ "$API_PORT" != "${SERVER_PORT:-8080}" ] && warn "Port ${SERVER_PORT:-8080} busy — API will use $API_PORT."
[ "$FE_PORT" != "3000" ] && warn "Port 3000 busy — frontend will use $FE_PORT."

# ── Start Docker deps ──
log "Starting PostgreSQL, Redis, RabbitMQ, MailHog..."
(cd "$REPO_DIR/docker" && docker compose up -d postgres redis mailhog rabbitmq)

log "Waiting for services to be healthy..."
for i in $(seq 1 40); do
  HEALTHY=$( (cd "$REPO_DIR/docker" && docker compose ps 2>/dev/null) | grep -c "(healthy)" || true)
  if [ "$HEALTHY" -ge 3 ]; then
    ok "Dependencies healthy."
    break
  fi
  [ "$i" -eq 40 ] && die "Dependencies did not become healthy. Check: docker compose -f docker/docker-compose.yml ps"
  sleep 3
done

# ── Load env + export full runtime set ──
set -a
# shellcheck disable=SC1091
source .env
set +a
export SERVER_PORT="$API_PORT"
export FRONTEND_URL="http://localhost:$FE_PORT"
export CORS_ALLOWED_ORIGINS="http://localhost:$FE_PORT"
export DATABASE_URL DATABASE_USER DATABASE_PASSWORD
export REDIS_HOST REDIS_PORT
export RABBITMQ_HOST RABBITMQ_PORT RABBITMQ_USERNAME RABBITMQ_PASSWORD
export JWT_ACCESS_SECRET JWT_REFRESH_SECRET JWT_ACCESS_EXPIRY_MINUTES JWT_REFRESH_EXPIRY_DAYS
export DATA_ENCRYPTION_KEY
export MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_SMTP_AUTH MAIL_SMTP_STARTTLS MAIL_FROM MAIL_FROM_NAME
export RECAPTCHA_SECRET_KEY RECAPTCHA_SITE_KEY
export GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET GITHUB_CLIENT_ID GITHUB_CLIENT_SECRET
export MPESA_ENVIRONMENT MPESA_CONSUMER_KEY MPESA_CONSUMER_SECRET MPESA_SHORTCODE MPESA_PASSKEY
export MPESA_BASE_URL MPESA_CALLBACK_URL MPESA_TIMEOUT_URL MPESA_STK_TIMEOUT_SECONDS
export STRIPE_ENVIRONMENT STRIPE_SECRET_KEY STRIPE_PUBLISHABLE_KEY STRIPE_WEBHOOK_SECRET
export STRIPE_TEST_SECRET_KEY STRIPE_TEST_PUBLISHABLE_KEY STRIPE_TEST_WEBHOOK_SECRET
export STRIPE_LIVE_SECRET_KEY STRIPE_LIVE_PUBLISHABLE_KEY STRIPE_LIVE_WEBHOOK_SECRET
export STRIPE_MIN_AMOUNT
export FREE_SHIPPING_THRESHOLD SHIPPING_COST TAX_RATE MIN_PASSWORD_LENGTH
export RATE_LIMIT_AUTH_PER_MINUTE RATE_LIMIT_API_PER_MINUTE

# ── Mail check: fall back to MailHog when the SMTP host is unreachable ──
if ! timeout 8 bash -c "</dev/tcp/$MAIL_HOST/$MAIL_PORT" >/dev/null 2>&1; then
  warn "Cannot reach $MAIL_HOST:$MAIL_PORT — using MailHog for this run (http://localhost:8025)."
  export MAIL_HOST=localhost MAIL_PORT=1025 MAIL_USERNAME= MAIL_PASSWORD=
  export MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS=false MAIL_FROM=noreply@iloveshopping.com
else
  ok "Mail server $MAIL_HOST:$MAIL_PORT reachable."
fi

# ── Backend (rebuild when sources changed) ──
log "Preparing backend..."
cd "$REPO_DIR/backend"
JAR="target/i-love-shopping-1.0.0-SNAPSHOT.jar"
if $REBUILD || [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit 2>/dev/null)" ]; then
  log "Building backend (may take a few minutes)..."
  if command -v mvn >/dev/null 2>&1; then mvn package -q -DskipTests; else ./mvnw package -q -DskipTests; fi
fi
pkill -f "i-love-shopping-1.0.0-SNAPSHOT.jar" 2>/dev/null || true
sleep 2
setsid java -jar "$JAR" < /dev/null > /tmp/ils-backend.log 2>&1 &
disown
API_PID=$!
log "Backend starting (PID $API_PID, log: /tmp/ils-backend.log)..."

log "Waiting for API health..."
for i in $(seq 1 36); do
  if curl -s -m 5 "http://localhost:$API_PORT/api/v1/health" | grep -q '"application":"i-love-shopping"'; then
    ok "API is UP on $API_PORT."
    break
  fi
  kill -0 "$API_PID" 2>/dev/null || die "Backend died at startup. See /tmp/ils-backend.log"
  [ "$i" -eq 36 ] && die "API did not become healthy. See /tmp/ils-backend.log"
  sleep 5
done

# ── Frontend (rebuild when sources/env changed) ──
log "Preparing frontend..."
cd "$REPO_DIR/frontend"
command -v node >/dev/null 2>&1 || die "Node.js is not installed (need 20+)."
[ -d "node_modules" ] || { log "Installing frontend dependencies..."; npm install; }
# Bake the full runtime config into the build so testers get working
# payments/captcha without hand-editing frontend env files.
export NEXT_PUBLIC_API_URL="http://localhost:$API_PORT/api/v1"
export NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY="${STRIPE_TEST_PUBLISHABLE_KEY:-$STRIPE_PUBLISHABLE_KEY}"
export NEXT_PUBLIC_STRIPE_MIN_AMOUNT="${STRIPE_MIN_AMOUNT:-100}"
export NEXT_PUBLIC_APP_NAME="${APP_NAME:-i-love-shopping}"
export NEXT_PUBLIC_APP_URL="http://localhost:$FE_PORT"
export NEXT_PUBLIC_SUPPORT_EMAIL="${SUPPORT_EMAIL:-support@iloveshopping.com}"
export NEXT_PUBLIC_COMPANY_LOCATION="Nairobi, Kenya"
export NEXT_PUBLIC_DEFAULT_COUNTRY="${DEFAULT_COUNTRY:-KE}"
export NEXT_PUBLIC_FREE_SHIPPING_THRESHOLD="${FREE_SHIPPING_THRESHOLD:-5000}"
export NEXT_PUBLIC_SHIPPING_COST="${SHIPPING_COST:-10}"
export NEXT_PUBLIC_TAX_RATE="${TAX_RATE:-0.16}"
export NEXT_PUBLIC_MIN_PASSWORD_LENGTH="${MIN_PASSWORD_LENGTH:-8}"
# reCAPTCHA + OAuth buttons only when the backend can actually honor them.
if [ -n "$RECAPTCHA_SITE_KEY" ] && [ "$RECAPTCHA_SITE_KEY" != "dev-recaptcha-site-key" ] \
    && [ -n "$RECAPTCHA_SECRET_KEY" ] && [ "$RECAPTCHA_SECRET_KEY" != "dev-test-secret" ]; then
  export NEXT_PUBLIC_RECAPTCHA_ENABLED=true NEXT_PUBLIC_RECAPTCHA_SITE_KEY="$RECAPTCHA_SITE_KEY"
else
  export NEXT_PUBLIC_RECAPTCHA_ENABLED=false
fi
export NEXT_PUBLIC_GOOGLE_ENABLED=false NEXT_PUBLIC_GITHUB_ENABLED=false
MARKER=".next/.api-port"
MARKER_VAL="http://localhost:$API_PORT/api/v1|${NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY}|${NEXT_PUBLIC_RECAPTCHA_ENABLED}"
if $REBUILD || [ ! -d ".next" ] || [ ! -f "$MARKER" ] || [ "$(cat "$MARKER")" != "$MARKER_VAL" ] \
    || [ -n "$(find src -newer .next -print -quit 2>/dev/null)" ]; then
  log "Building frontend (may take a few minutes)..."
  NEXT_PUBLIC_API_URL="http://localhost:$API_PORT/api/v1" npm run build
  echo "$MARKER_VAL" > "$MARKER"
fi
pkill -f "$REPO_DIR/frontend" 2>/dev/null || true
sleep 2
setsid node node_modules/next/dist/bin/next start -p "$FE_PORT" < /dev/null > /tmp/ils-frontend.log 2>&1 &
disown
FE_PID=$!
log "Frontend starting (PID $FE_PID, log: /tmp/ils-frontend.log)..."

log "Waiting for frontend..."
for i in $(seq 1 12); do
  # Match our own storefront by name — a neighbor's 200 on this port must not count.
  if curl -s -m 5 "http://localhost:$FE_PORT/" | grep -qi "i-love-shopping"; then
    ok "Frontend is UP on $FE_PORT."
    break
  fi
  kill -0 "$FE_PID" 2>/dev/null || die "Frontend died at startup. See /tmp/ils-frontend.log"
  [ "$i" -eq 12 ] && die "Frontend did not start. See /tmp/ils-frontend.log"
  sleep 5
done

# ── Done ──
MPESA_STATE="disabled (set MPESA_* in .env)"; [ -n "$MPESA_CONSUMER_KEY" ] && MPESA_STATE="sandbox, STK to 254708374149 (Africa only)"
STRIPE_STATE="disabled (set STRIPE_TEST_SECRET_KEY in .env)"
if [ -n "$STRIPE_TEST_SECRET_KEY" ] || [ -n "$STRIPE_SECRET_KEY" ]; then STRIPE_STATE="test mode, card 4242... (worldwide)"; fi
[ "$STRIPE_ENVIRONMENT" = "live" ] && STRIPE_STATE="LIVE — real money moves!"
MAIL_STATE="MailHog http://localhost:8025"; [ "$MAIL_HOST" != "localhost" ] && MAIL_STATE="$MAIL_HOST (real delivery)"
OAUTH_STATE="off (set GOOGLE_*/GITHUB_* + frontend flags to enable)"
echo
echo "============================================================"
ok "i-love-shopping is running (dev behaves like production)!"
echo "============================================================"
echo
echo "  Frontend:       http://localhost:$FE_PORT"
echo "  API:            http://localhost:$API_PORT/api/v1"
echo "  Swagger:        http://localhost:$API_PORT/api/v1/docs"
echo "  MailHog:        http://localhost:8025  (only if SMTP was unreachable)"
echo "  RabbitMQ:       http://localhost:15672  (iloveshopping / iloveshopping)"
echo
echo "  Test accounts:"
echo "    Admin:   admin@iloveshopping.com / Admin123!"
echo "    User:    user@iloveshopping.com  / User123!"
echo
echo "  Test payments:"
echo "    M-Pesa:  $MPESA_STATE"
echo "    Stripe:  $STRIPE_STATE"
echo "    Email:   $MAIL_STATE"
echo "    OAuth:   $OAUTH_STATE"
echo
echo "  To stop (keeps dev database):  ./start.sh --stop"
echo "============================================================"
