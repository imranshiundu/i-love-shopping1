#!/usr/bin/env bash
set -euo pipefail

# ─── I Love Shopping — Development launcher ───────────────────────────────────
# Usage:
#   ./start.sh              Interactive menu (prompts for option 1 or 2)
#   ./start.sh --auto       Fully automated — option 2, no prompts, installs everything
#   ./start.sh --stop       Stop all services and clean up containers
# ───────────────────────────────────────────────────────────────────────────────

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$REPO_DIR/docker/docker-compose.yml"
COMPOSE=(docker compose)

# ── Colours ───────────────────────────────────────────────────────────────────
RED=$'\033[1;31m'; GRN=$'\033[1;32m'; YEL=$'\033[1;33m'
BLU=$'\033[1;34m'; BLD=$'\033[1m'; RST=$'\033[0m'
log()  { printf "%s[i-love-shopping]%s %s\n" "$BLU" "$RST" "$1"; }
ok()   { printf "%s[i-love-shopping]%s %s\n" "$GRN" "$RST" "$1"; }
warn() { printf "%s[i-love-shopping]%s %s\n" "$YEL" "$RST" "$1"; }
die()  { printf "%s[i-love-shopping]%s %s\n" "$RED" "$RST" "$1"; exit 1; }
have() { command -v "$1" >/dev/null 2>&1; }

# ── OS detection ──────────────────────────────────────────────────────────────
OS="linux"
case "$(uname -s)" in
  Darwin)                  OS="mac"  ;;
  MINGW*|MSYS*|CYGWIN*)   OS="win"  ;;
esac

# ── Parse flags ───────────────────────────────────────────────────────────────
AUTO=false
STOP=false
for arg in "$@"; do
  case "$arg" in
    --auto)  AUTO=true  ;;
    --stop)  STOP=true  ;;
  esac
done

# ── Stop mode ─────────────────────────────────────────────────────────────────
if $STOP; then
  log "Stopping all services..."
  "${COMPOSE[@]}" -f "$COMPOSE_FILE" down -v --remove-orphans 2>/dev/null || true
  # Kill any lingering java (backend) and next dev (frontend)
  pkill -f "iloveshopping-1.0.0.jar" 2>/dev/null || true
  pkill -f "next dev" 2>/dev/null || true
  ok "All services stopped."
  exit 0
fi

# ── Package install helpers ───────────────────────────────────────────────────
install_system_pkg() {
  if have sudo; then
    if have apt-get; then sudo apt-get update -qq && sudo apt-get install -y "$1"
    elif have dnf;    then sudo dnf install -y "$1"
    elif have yum;    then sudo yum install -y "$1"
    elif have apk;    then sudo apk add "$1"
    fi
  elif have apt-get; then apt-get install -y "$1"
  elif have dnf;     then dnf install -y "$1"
  fi
}

# ── Docker ────────────────────────────────────────────────────────────────────
check_docker() {
  if ! have docker; then
    warn "Docker is not installed — attempting to install..."
    if [ "$OS" = "mac" ]; then
      if have brew; then brew install --cask docker
      else die "Docker Desktop is required.
  Download: https://docs.docker.com/desktop/install/mac-install/
  Install the .dmg, open Docker Desktop, then re-run this script."
      fi
    elif [ "$OS" = "win" ]; then
      if have choco; then choco install docker-desktop -y
      else die "Docker Desktop is required.
  Download: https://docs.docker.com/desktop/install/windows-install/
  Install, restart your PC, open Docker Desktop, then re-run this script."
      fi
    else
      log "Installing Docker via your package manager..."
      install_system_pkg docker.io || install_system_pkg docker || true
      if ! have docker; then
        curl -fsSL https://get.docker.com | sudo sh 2>/dev/null || true
      fi
    fi
    have docker || die "Docker install failed.
  Install manually: https://docs.docker.com/get-docker/
  Then re-run this script."
  fi

  # Start daemon if not running
  if ! docker info >/dev/null 2>&1; then
    warn "Docker daemon is not running — starting it..."
    case "$OS" in
      mac)  open -a Docker 2>/dev/null || open /Applications/Docker.app 2>/dev/null || true ;;
      win)  start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe" 2>/dev/null || true  ;;
      *)    sudo systemctl start docker 2>/dev/null || sudo service docker start 2>/dev/null || true ;;
    esac
    log "Waiting for Docker daemon..."
    local retries=0
    while ! docker info >/dev/null 2>&1; do
      retries=$((retries + 1))
      if [ "$retries" -ge 90 ]; then
        die "Docker daemon did not start within 90 seconds.
  On macOS: open Docker Desktop, wait for the whale icon to stop bouncing.
  On Linux:  sudo systemctl start docker
  Then re-run this script."
      fi
      sleep 1
    done
    ok "Docker daemon is running."
  fi

  # Docker Compose
  if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
  elif have docker-compose; then
    COMPOSE=(docker-compose)
  else
    warn "Docker Compose not found — installing..."
    case "$OS" in
      mac)  have brew && brew install docker-compose ;;
      win)  have choco && choco install docker-compose -y ;;
      *)    install_system_pkg docker-compose-plugin || install_system_pkg docker-compose-v2 || install_system_pkg docker-compose ;;
    esac
    if docker compose version >/dev/null 2>&1; then
      COMPOSE=(docker compose)
    elif have docker-compose; then
      COMPOSE=(docker-compose)
    else
      die "Docker Compose install failed.
  Install manually: https://docs.docker.com/compose/install/
  Then re-run this script."
    fi
  fi
  ok "Docker and Docker Compose ready."
}

# ── Java ──────────────────────────────────────────────────────────────────────
check_java() {
  if have java && java -version 2>&1 | head -1 | grep -qE '"2[1-9]'; then
    ok "Java $(java -version 2>&1 | head -1 | grep -oE '2[0-9]+') found."
    return
  fi
  warn "Java 21+ not found — attempting to install..."
  case "$OS" in
    mac)
      if have brew; then brew install --cask temurin 2>/dev/null || brew install openjdk@21 2>/dev/null || true; fi
      ;;
    win)
      if have choco; then choco install temurin21 -y 2>/dev/null || true; fi
      ;;
    *)
      install_system_pkg openjdk-21-jdk 2>/dev/null || install_system_pkg java-21-openjdk 2>/dev/null || install_system_pkg openjdk21 2>/dev/null || true
      ;;
  esac
  if have java && java -version 2>&1 | head -1 | grep -qE '"2[1-9]'; then
    ok "Java installed successfully."
  else
    die "Java 21+ is required but could not be auto-installed.
  Install manually:
    macOS:   brew install --cask temurin
    Ubuntu:  sudo apt install openjdk-21-jdk
    Windows: https://adoptium.net/temurin/releases/?version=21
  Then re-run this script."
  fi
}

# ── Maven ─────────────────────────────────────────────────────────────────────
check_maven() {
  if have mvn; then
    ok "Maven found."
  elif [ -x "$REPO_DIR/backend/mvnw" ]; then
    ok "Using Maven wrapper (backend/mvnw)."
  else
    warn "Maven not found — installing..."
    case "$OS" in
      mac)  have brew && brew install maven ;;
      win)  have choco && choco install maven -y ;;
      *)    install_system_pkg maven ;;
    esac
    if have mvn; then
      ok "Maven installed."
    elif [ -x "$REPO_DIR/backend/mvnw" ]; then
      ok "Using Maven wrapper."
    else
      die "Maven is required but could not be auto-installed.
  Install manually: https://maven.apache.org/install.html
  Or restore the Maven wrapper (backend/mvnw) and re-run."
    fi
  fi
}

# ── Node.js ───────────────────────────────────────────────────────────────────
check_node() {
  if have node && node -v 2>/dev/null | grep -qE 'v(1[89]|2[0-9]|3[0-9])'; then
    ok "Node.js $(node -v) found."
    return
  fi
  warn "Node.js 18+ not found — attempting to install..."
  case "$OS" in
    mac)  have brew && brew install node ;;
    win)  have choco && choco install nodejs-lts -y ;;
    *)    install_system_pkg nodejs 2>/dev/null || install_system_pkg npm 2>/dev/null || true ;;
  esac
  if have node && node -v 2>/dev/null | grep -qE 'v(1[89]|2[0-9]|3[0-9])'; then
    ok "Node.js installed."
  else
    die "Node.js 18+ is required but could not be auto-installed.
  Install manually: https://nodejs.org
  Then re-run this script."
  fi
}

# ── .env file ─────────────────────────────────────────────────────────────────
setup_env() {
  if [ -f "$REPO_DIR/.env" ]; then
    ok ".env already exists — keeping current settings."
    return
  fi
  if [ -f "$REPO_DIR/.env.example" ]; then
    cp "$REPO_DIR/.env.example" "$REPO_DIR/.env"
    # Auto-generate secrets if openssl is available
    if have openssl; then
      _sed_i() {
        if [ "$OS" = "mac" ]; then sed -i '' "$@"; else sed -i "$@"; fi
      }
      _sed_i "s|your-access-secret-minimum-32-characters-long|$(openssl rand -base64 32)|g" "$REPO_DIR/.env"
      _sed_i "s|your-refresh-secret-minimum-32-characters-long|$(openssl rand -base64 32)|g" "$REPO_DIR/.env"
      _sed_i "s|your-recaptcha-secret-key|dev-recaptcha-secret|g" "$REPO_DIR/.env"
      _sed_i "s|your-recaptcha-site-key|dev-recaptcha-site-key|g" "$REPO_DIR/.env"
    fi
    ok ".env created from .env.example with generated secrets."
  else
    warn "No .env.example found — using environment defaults."
  fi
}

# ── Port resolution ───────────────────────────────────────────────────────────
port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && { exec 3>&- 3<&-; return 0; }
  return 1
}

find_free_port() {
  local p="$1" tries=0
  while port_in_use "$p"; do
    p=$((p + 1))
    tries=$((tries + 1))
    [ "$tries" -ge 200 ] && die "Could not find a free port at or above $1."
  done
  printf '%s' "$p"
}

resolve_ports() {
  resolve_one() {
    local label="$1" var="$2" preferred="$3" chosen
    chosen=$(find_free_port "$preferred")
    [ "$chosen" != "$preferred" ] && warn "$label: port $preferred busy — using $chosen instead."
    export "$var=$chosen"
  }
  resolve_one "PostgreSQL"    POSTGRES_PORT      "${POSTGRES_PORT:-5433}"
  resolve_one "Redis"         REDIS_PORT         "${REDIS_PORT:-6380}"
  resolve_one "Mailhog SMTP"  MAILHOG_SMTP_PORT  "${MAILHOG_SMTP_PORT:-1025}"
  resolve_one "Mailhog UI"    MAILHOG_UI_PORT    "${MAILHOG_UI_PORT:-8025}"
  resolve_one "RabbitMQ"      RABBITMQ_AMQP_PORT "${RABBITMQ_AMQP_PORT:-5672}"
  resolve_one "RabbitMQ UI"   RABBITMQ_UI_PORT   "${RABBITMQ_UI_PORT:-15672}"
  resolve_one "API"           API_PORT           "${API_PORT:-8080}"
  resolve_one "Frontend"      FRONTEND_PORT      "${FRONTEND_PORT:-3000}"
}

# ── Print URLs ────────────────────────────────────────────────────────────────
print_urls() {
  echo
  ok "Frontend:            http://localhost:$FRONTEND_PORT"
  ok "API:                 http://localhost:$API_PORT/api/v1"
  ok "Swagger UI:          http://localhost:$API_PORT/api/v1/docs"
  ok "Mailhog Web UI:      http://localhost:$MAILHOG_UI_PORT"
  ok "RabbitMQ Management: http://localhost:$RABBITMQ_UI_PORT  (iloveshopping / iloveshopping)"
  echo
  log "Seeded accounts: admin@iloveshopping.com / Admin123!   user@iloveshopping.com / User123!"
  echo
}

# ── Wait for healthy container ────────────────────────────────────────────────
wait_healthy() {
  local svc="$1"
  for i in $(seq 1 40); do
    "${COMPOSE[@]}" -f "$COMPOSE_FILE" ps "$svc" 2>/dev/null | grep -q healthy && return 0
    sleep 3
  done
  die "Timed out waiting for $svc. Check: ${COMPOSE[*]} -f $COMPOSE_FILE ps"
}

# ── Run backend ───────────────────────────────────────────────────────────────
run_backend() {
  cd "$REPO_DIR/backend"
  if have mvn; then mvn spring-boot:run
  else ./mvnw spring-boot:run
  fi
}

# ── Run frontend ──────────────────────────────────────────────────────────────
run_frontend() {
  cd "$REPO_DIR/frontend"
  npm install
  npm run dev
}

# ── Option 1: everything in Docker ────────────────────────────────────────────
run_all_docker() {
  resolve_ports
  ok "Starting everything in Docker Compose (foreground)..."
  print_urls
  log "Press Ctrl+C to stop all services."
  echo
  "${COMPOSE[@]}" -f "$COMPOSE_FILE" up
}

# ── Option 2: deps in Docker, API + frontend local ────────────────────────────
run_local_backend() {
  check_java
  check_maven
  check_node
  setup_env
  resolve_ports

  log "Starting PostgreSQL, Redis, Mailhog and RabbitMQ..."
  "${COMPOSE[@]}" -f "$COMPOSE_FILE" up -d postgres redis mailhog rabbitmq

  log "Waiting for dependencies to become healthy..."
  wait_healthy postgres
  wait_healthy redis
  wait_healthy rabbitmq
  ok "All dependencies are up."

  # Export env vars for the backend and frontend
  export DATABASE_URL="jdbc:postgresql://localhost:$POSTGRES_PORT/iloveshopping?stringtype=unspecified"
  export DATABASE_USER=iloveshopping
  export DATABASE_PASSWORD=iloveshopping
  export REDIS_HOST=localhost
  export RECAPTCHA_SECRET_KEY=dev-test-secret
  export RECAPTCHA_SITE_KEY=dev-test-site
  export JWT_ACCESS_SECRET=dev-access-secret-min-32-chars-long-for-test
  export JWT_REFRESH_SECRET=dev-refresh-secret-min-32-chars-long-for-test
  export MAIL_HOST=localhost
  export MAIL_PORT="$MAILHOG_SMTP_PORT"
  export MAIL_SMTP_AUTH=false
  export MAIL_SMTP_STARTTLS=false
  export SERVER_PORT="$API_PORT"
  export FRONTEND_URL="http://localhost:$FRONTEND_PORT"
  export CORS_ALLOWED_ORIGINS="http://localhost:$FRONTEND_PORT"
  export RABBITMQ_HOST=localhost
  export RABBITMQ_PORT="$RABBITMQ_AMQP_PORT"
  export RABBITMQ_USERNAME=iloveshopping
  export RABBITMQ_PASSWORD=iloveshopping
  export NEXT_PUBLIC_API_URL="http://localhost:$API_PORT/api/v1"
  export BACKEND_INTERNAL_URL="http://localhost:$API_PORT"
  export PORT="$FRONTEND_PORT"

  print_urls
  log "Press Ctrl+C to stop the API, the frontend and the containers."
  echo

  trap 'log "Shutting down..."; "${COMPOSE[@]}" -f "$COMPOSE_FILE" rm -sf postgres redis mailhog rabbitmq 2>/dev/null; pkill -f "iloveshopping-1.0.0.jar" 2>/dev/null; pkill -f "next dev" 2>/dev/null' EXIT INT TERM

  run_backend  &
  BACKEND_PID=$!
  run_frontend &
  FRONTEND_PID=$!
  wait
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
  cd "$REPO_DIR"
  echo
  echo "╔══════════════════════════════════════════════════════════╗"
  echo "║          I Love Shopping — Development Setup            ║"
  echo "║    https://github.com/imranshiundu/i-love-shopping1    ║"
  echo "╚══════════════════════════════════════════════════════════╝"
  echo
  log "Repository: $REPO_DIR"
  echo

  check_docker
  echo

  if $AUTO; then
    log "Running in automated mode (option 2: local backend)..."
    run_local_backend
  else
    log "How do you want to run the project?"
    log "  1) Everything in Docker (API + Frontend included) — only Docker needed"
    log "  2) Dependencies in Docker, API & Frontend run locally — needs Java 21, Maven, Node.js"
    echo
    log "Busy ports auto-shift to the next free port."
    echo
    read -rp "Choose [1/2]: " choice
    case "$choice" in
      1) run_all_docker     ;;
      2) run_local_backend  ;;
      *) die "Invalid choice. Run again and pick 1 or 2." ;;
    esac
  fi
}

main
