#!/usr/bin/env bash
# =============================================================
# i-love-shopping — Quick Start
# =============================================================
# Starts the full stack: Docker deps + backend + frontend.
# Usage:
#   ./start.sh         Start everything
#   ./start.sh --stop  Stop everything
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

STOP=false
for arg in "$@"; do
  case "$arg" in
    --stop)  STOP=true  ;;
    --help|-h)
      echo "Usage: ./start.sh [--stop]"
      echo "  --stop  Stop all services"
      exit 0
      ;;
  esac
done

# ── Stop mode ──
if $STOP; then
  log "Stopping services..."
  cd "$REPO_DIR/docker"
  docker compose down -v --remove-orphans 2>/dev/null || true
  pkill -f "i-love-shopping-1.0.0-SNAPSHOT.jar" 2>/dev/null || true
  pkill -f "next/dist/bin/next start" 2>/dev/null || true
  pkill -f "next start" 2>/dev/null || true
  ok "All stopped."
  exit 0
fi

# ── Check .env ──
cd "$REPO_DIR"
if [ ! -f ".env" ]; then
  if [ -f ".env.example" ]; then
    cp .env.example .env
    ok "Created .env from .env.example"
  else
    die ".env.example not found. Please create .env manually."
  fi
fi

# ── Check Docker ──
if ! command -v docker >/dev/null 2>&1; then
  die "Docker is not installed. Install from https://docs.docker.com/get-docker/"
fi
if ! docker info >/dev/null 2>&1; then
  die "Docker daemon is not running. Start Docker Desktop and try again."
fi

# ── Start Docker deps ──
log "Starting PostgreSQL, Redis, RabbitMQ, MailHog..."
cd "$REPO_DIR/docker"
docker compose up -d postgres redis mailhog rabbitmq

# ── Wait for healthy ──
log "Waiting for services to be healthy..."
for i in $(seq 1 30); do
  HEALTHY=$(docker compose ps 2>/dev/null | grep -c "(healthy)" || true)
  if [ "$HEALTHY" -ge 4 ]; then
    ok "All services healthy."
    break
  fi
  if [ "$i" -eq 30 ]; then
    warn "Services taking longer than expected. Check: docker compose -f docker/docker-compose.yml ps"
  fi
  sleep 2
done

# ── Load env vars ──
cd "$REPO_DIR"
set -a
source .env
set +a
export DATABASE_URL DATABASE_USER DATABASE_PASSWORD
export REDIS_HOST REDIS_PORT
export RABBITMQ_HOST RABBITMQ_PORT RABBITMQ_USERNAME RABBITMQ_PASSWORD
export JWT_SECRET
export MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_SMTP_AUTH MAIL_SMTP_STARTTLS MAIL_FROM MAIL_FROM_NAME
export MPESA_CONSUMER_KEY MPESA_CONSUMER_SECRET MPESA_SHORTCODE MPESA_PASSKEY MPESA_BASE_URL
export MPESA_CALLBACK_URL MPESA_TIMEOUT_URL MPESA_ENVIRONMENT
export SERVER_PORT RECAPTCHA_SECRET_KEY RECAPTCHA_SITE_KEY

# ── Start backend ──
log "Starting backend on port $SERVER_PORT..."
cd "$REPO_DIR/backend"
if [ ! -f "target/i-love-shopping-1.0.0-SNAPSHOT.jar" ]; then
  log "Building backend (first time, may take a few minutes)..."
  if command -v mvn >/dev/null 2>&1; then
    mvn package -q -DskipTests
  else
    ./mvnw package -q -DskipTests
  fi
fi
setsid java -jar target/i-love-shopping-1.0.0-SNAPSHOT.jar < /dev/null > /tmp/backend.log 2>&1 &
disown
ok "Backend launched (log: /tmp/backend.log)"

# ── Start frontend ──
log "Starting frontend on port 3000..."
cd "$REPO_DIR/frontend"
if [ ! -d ".next" ]; then
  log "Building frontend (first time, may take a few minutes)..."
  NODE_OPTIONS="--max-old-space-size=1024" npx next build
fi
setsid node node_modules/next/dist/bin/next start -p 3000 < /dev/null > /tmp/frontend.log 2>&1 &
disown
ok "Frontend launched (log: /tmp/frontend.log)"

# ── Wait & show URLs ──
sleep 15
echo
echo "============================================================"
ok "i-love-shopping is running!"
echo "============================================================"
echo
echo "  Frontend:       http://localhost:3000"
echo "  API:            http://localhost:8080/api/v1"
echo "  Swagger:        http://localhost:8080/api/v1/docs"
echo "  MailHog:        http://localhost:8025  (view captured emails)"
echo "  RabbitMQ:       http://localhost:15672  (iloveshopping / iloveshopping)"
echo
echo "  Test accounts:"
echo "    Admin:   admin@iloveshopping.com / Admin123!"
echo "    User:    user@iloveshopping.com  / User123!"
echo
echo "  To stop:  ./start.sh --stop"
echo "============================================================"
