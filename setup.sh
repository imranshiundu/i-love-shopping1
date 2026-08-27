#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# I Love Shopping — cross-platform setup script (Linux / macOS)
# Usage:  chmod +x setup.sh && ./setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── colours ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

ok()   { printf "${GREEN}✓${NC} %s\n" "$*"; }
warn() { printf "${YELLOW}⚠${NC} %s\n" "$*"; }
fail() { printf "${RED}✗${NC} %s\n" "$*"; exit 1; }
step() { printf "\n${BOLD}${CYAN}── %s ──${NC}\n" "$*"; }

# ── OS detection ───────────────────────────────────────────────────────────────
OS="$(uname -s)"
case "$OS" in
  Darwin) PLATFORM="macOS";  PKG_MGR="brew" ;;
  Linux)  PLATFORM="Linux";  PKG_MGR=""     ;;
  *)      fail "Unsupported OS: $OS. Use the Windows .bat script instead." ;;
esac

if [ "$PLATFORM" = "Linux" ]; then
  if command -v apt-get &>/dev/null; then PKG_MGR="apt"
  elif command -v dnf &>/dev/null;  then PKG_MGR="dnf"
  elif command -v pacman &>/dev/null; then PKG_MGR="pacman"
  elif command -v apk &>/dev/null;  then PKG_MGR="apk"
  fi
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║           I Love Shopping — Development Setup               ║"
echo "║         https://github.com/imranshiundu/i-love-shopping1   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
printf "  Platform: ${BOLD}%s${NC}  |  Package manager: ${BOLD}%s${NC}\n" "$PLATFORM" "${PKG_MGR:-manual}"
echo ""

# ── 1. Docker ──────────────────────────────────────────────────────────────────
step "1/7  Checking Docker"

if ! command -v docker &>/dev/null; then
  echo ""
  warn "Docker is not installed."
  echo ""
  case "$PLATFORM" in
    macOS)
      echo "  Install Docker Desktop for Mac:"
      echo "    1. Download: https://docs.docker.com/desktop/install/mac-install/"
      echo "    2. Open the .dmg and drag Docker to Applications"
      echo "    3. Launch Docker Desktop from Applications"
      echo "    4. Wait for 'Docker Desktop is running' in the menu bar"
      echo ""
      read -rp "Press Enter once Docker Desktop is running (or Ctrl+C to abort)..." _
      ;;
    Linux)
      echo "  Quick install options:"
      echo "    Ubuntu/Debian:  curl -fsSL https://get.docker.com | sudo sh"
      echo "    Fedora/RHEL:    sudo dnf install docker-ce docker-compose-plugin"
      echo "    Arch:           sudo pacman -S docker docker-compose"
      echo ""
      read -rp "Press Enter once Docker is installed, or install now (y/N): " _install_docker
      if [[ "$_install_docker" =~ ^[Yy]$ ]]; then
        if command -v curl &>/dev/null; then
          curl -fsSL https://get.docker.com | sudo sh
          sudo usermod -aG docker "$USER" 2>/dev/null || true
          warn "Log out and back in (or run 'newgrp docker') for group changes to take effect."
        else
          fail "curl not found. Install Docker manually: https://docs.docker.com/engine/install/"
        fi
      fi
      ;;
  esac
fi

if ! command -v docker &>/dev/null; then
  fail "Docker is still not available. Install it and re-run this script."
fi
ok "Docker found: $(docker --version)"

# ── Docker daemon running? ────────────────────────────────────────────────────
if ! docker info &>/dev/null 2>&1; then
  echo ""
  warn "Docker daemon is not running."
  case "$PLATFORM" in
    macOS)
      echo "  Starting Docker Desktop..."
      open -a Docker 2>/dev/null || open /Applications/Docker.app 2>/dev/null || {
        warn "Could not start Docker Desktop automatically."
        echo "  Please open Docker Desktop manually from Applications."
      }
      ;;
    Linux)
      echo "  Attempting to start Docker service..."
      sudo systemctl start docker 2>/dev/null || sudo service docker start 2>/dev/null || {
        warn "Could not start Docker service."
        echo "  Try: sudo systemctl start docker"
      }
      ;;
  esac

  echo -n "  Waiting for Docker daemon"
  _retries=0
  while ! docker info &>/dev/null 2>&1 && [ $_retries -lt 60 ]; do
    printf "."
    sleep 1
    _retries=$((_retries + 1))
  done
  echo ""
  if [ $_retries -ge 60 ]; then
    fail "Docker daemon still not running after 60 seconds.
  On macOS: open Docker Desktop and wait for the whale icon to stop animating.
  On Linux:  sudo systemctl status docker"
  fi
fi
ok "Docker daemon is running"

# ── Docker Compose ─────────────────────────────────────────────────────────────
step "2/7  Checking Docker Compose"

# docker compose v2 (plugin) or standalone docker-compose?
DOCKER_COMPOSE=""
if docker compose version &>/dev/null 2>&1; then
  DOCKER_COMPOSE="docker compose"
elif command -v docker-compose &>/dev/null; then
  DOCKER_COMPOSE="docker-compose"
fi

if [ -z "$DOCKER_COMPOSE" ]; then
  warn "Docker Compose not found."
  echo ""
  case "$PLATFORM" in
    macOS)
      echo "  Docker Compose is included with Docker Desktop for Mac."
      echo "  Update Docker Desktop to the latest version."
      ;;
    Linux)
      echo "  Install the Compose plugin:"
      case "$PKG_MGR" in
        apt)    echo "    sudo apt install docker-compose-plugin" ;;
        dnf)    echo "    sudo dnf install docker-compose-plugin" ;;
        *)      echo "    Follow: https://docs.docker.com/compose/install/linux/" ;;
      esac
      ;;
  esac
  read -rp "Press Enter once Docker Compose is available (or Ctrl+C to abort)..." _
  if docker compose version &>/dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
  elif command -v docker-compose &>/dev/null; then
    DOCKER_COMPOSE="docker-compose"
  else
    fail "Docker Compose is still not available."
  fi
fi
ok "Docker Compose: $($DOCKER_COMPOSE version 2>/dev/null || echo 'available')"

# ── 3. Java & Maven ──────────────────────────────────────────────────────────
step "3/7  Checking Java 21 and Maven"

JAVA_OK=false
if command -v java &>/dev/null; then
  _java_ver=$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1)
  if [ "$_java_ver" -ge 21 ] 2>/dev/null; then
    JAVA_OK=true
    ok "Java $(_java_ver) found"
  else
    warn "Java found but version $_java_ver (need 21+)"
  fi
fi

if [ "$JAVA_OK" = false ]; then
  warn "Java 21+ is required for the backend."
  echo ""
  echo "  Install options:"
  echo "    macOS:   brew install openjdk@21"
  echo "    Ubuntu:  sudo apt install openjdk-21-jdk"
  echo "    Fedora:  sudo dnf install java-21-openjdk-devel"
  echo "    Arch:    sudo pacman -S jdk21-openjdk"
  echo "    Or use SDKMAN: curl -s 'https://get.sdkman.io' | bash && sdk install java 21.0.3-tem"
  echo ""
  read -rp "Press Enter once Java 21+ is installed (or Ctrl+C to abort)..." _
  # Re-check
  if command -v java &>/dev/null; then
    _java_ver=$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1)
    [ "$_java_ver" -ge 21 ] 2>/dev/null || fail "Java version is still $_java_ver (need 21+)"
    ok "Java $(_java_ver) confirmed"
  else
    fail "Java is still not available."
  fi
fi

# Maven wrapper handles Maven itself — just check mvnw exists
if [ ! -f "./backend/mvnw" ]; then
  warn "Maven wrapper (backend/mvnw) not found. Using system Maven."
  command -v mvn &>/dev/null || fail "Maven is not installed. Install it or restore the mvnw wrapper."
  ok "Maven found: $(mvn --version 2>/dev/null | head -1)"
else
  chmod +x ./backend/mvnw 2>/dev/null || true
  ok "Maven wrapper ready"
fi

# ── 4. Node.js ────────────────────────────────────────────────────────────────
step "4/7  Checking Node.js 18+"

NODE_OK=false
if command -v node &>/dev/null; then
  _node_major=$(node --version 2>/dev/null | grep -oE '[0-9]+' | head -1)
  if [ "$_node_major" -ge 18 ] 2>/dev/null; then
    NODE_OK=true
    ok "Node.js $(node --version) found"
  else
    warn "Node.js found but version $(node --version) (need 18+)"
  fi
fi

if [ "$NODE_OK" = false ]; then
  warn "Node.js 18+ is required for the frontend."
  echo ""
  echo "  Install options:"
  echo "    macOS:   brew install node"
  echo "    Ubuntu:  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - && sudo apt install -y nodejs"
  echo "    Fedora:  sudo dnf module install nodejs:20/common"
  echo "    Or use nvm: curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash && nvm install 20"
  echo ""
  read -rp "Press Enter once Node.js 18+ is installed (or Ctrl+C to abort)..." _
  if command -v node &>/dev/null; then
    _node_major=$(node --version 2>/dev/null | grep -oE '[0-9]+' | head -1)
    [ "$_node_major" -ge 18 ] 2>/dev/null || fail "Node version is still too old (need 18+)"
    ok "Node.js $(node --version) confirmed"
  else
    fail "Node.js is still not available."
  fi
fi

# ── 5. .env file ──────────────────────────────────────────────────────────────
step "5/7  Preparing environment file"

if [ ! -f "./.env" ]; then
  cp .env.example .env
  ok ".env created from .env.example"

  # Auto-generate secrets
  if command -v openssl &>/dev/null; then
    _access_secret=$(openssl rand -base64 32)
    _refresh_secret=$(openssl rand -base64 32)
    _encryption_key=$(openssl rand -base64 32)
    _sed_i() {
      if [ "$PLATFORM" = "macOS" ]; then sed -i '' "$@"; else sed -i "$@"; fi
    }
    _sed_i "s|your-access-secret-minimum-32-characters-long|$_access_secret|g" .env
    _sed_i "s|your-refresh-secret-minimum-32-characters-long|$_refresh_secret|g" .env
    _sed_i "s|your-recaptcha-secret-key|dev-recaptcha-secret|g" .env
    _sed_i "s|your-recaptcha-site-key|dev-recaptcha-site-key|g" .env
    ok "Generated JWT secrets and reCAPTCHA dev keys"
    echo ""
    warn "Edit .env later to add real values for: email, M-Pesa, OAuth, encryption key"
  fi
else
  ok ".env already exists — keeping current settings"
fi

# ── 6. Docker services ────────────────────────────────────────────────────────
step "6/7  Starting Docker services"

echo ""
echo "  Ports used by default:"
echo "    PostgreSQL   5433    RabbitMQ      5672 / 15672"
echo "    Redis        6380    Mailhog       1025 / 8025"
echo ""

# Check for port conflicts before starting
_ports_busy=false
for _port in 5433 6380 5672 15672 1025 8025; do
  if command -v lsof &>/dev/null; then
    if lsof -i :$_port &>/dev/null 2>&1; then
      warn "Port $_port is already in use — Docker will auto-shift to the next free port"
      _ports_busy=true
    fi
  elif command -v ss &>/dev/null; then
    if ss -ltnp 2>/dev/null | grep -q ":$_port "; then
      warn "Port $_port is already in use — Docker will auto-shift to the next free port"
      _ports_busy=true
    fi
  elif command -v netstat &>/dev/null; then
    if netstat -an 2>/dev/null | grep -q ":$_port "; then
      warn "Port $_port is already in use — Docker will auto-shift to the next free port"
      _ports_busy=true
    fi
  fi
done
if [ "$_ports_busy" = false ]; then
  ok "All default ports are available"
fi

echo ""
$DOCKER_COMPOSE -f docker/docker-compose.yml up -d postgres redis mailhog rabbitmq
ok "Docker services started"

echo -n "  Waiting for RabbitMQ"
_retries=0
while ! $DOCKER_COMPOSE exec -T rabbitmq rabbitmqctl await_startup_nodes 1 &>/dev/null 2>&1 && [ $_retries -lt 30 ]; do
  printf "."
  sleep 1
  _retries=$((_retries + 1))
done
echo ""
ok "RabbitMQ ready"

# ── 7. Build & run ────────────────────────────────────────────────────────────
step "7/7  Building and starting the application"

echo ""
echo "  Building backend (this may take a few minutes on first run)..."
if [ -f "./backend/mvnw" ]; then
  ./backend/mvnw -f backend/pom.xml clean package -DskipTests -q 2>&1 | tail -3
else
  mvn -f backend/pom.xml clean package -DskipTests -q 2>&1 | tail -3
fi
ok "Backend JAR built"

echo ""
echo "  Starting backend server..."
nohup java -jar backend/target/iloveshopping-1.0.0.jar --spring.profiles.active=dev >/tmp/ilove-shopping-backend.log 2>&1 &
_backend_pid=$!
echo "$_backend_pid" > /tmp/ilove-shopping-backend.pid
ok "Backend starting (PID: $_backend_pid) on http://localhost:8080"

echo ""
echo "  Installing frontend dependencies..."
cd frontend && npm install --silent 2>/dev/null && cd ..
ok "Frontend dependencies installed"

echo ""
echo "  Starting frontend dev server..."
cd frontend && setsid npx next dev > /tmp/ilove-shopping-frontend.log 2>&1 &
_disown_pid=$!
cd ..
sleep 1
_ok_frontend=false
for _try in 1 2 3 4 5; do
  if curl -s --max-time 5 -o /dev/null http://localhost:3000 2>/dev/null; then
    _ok_frontend=true; break
  fi
  sleep 2
done
if [ "$_ok_frontend" = true ]; then
  ok "Frontend running on http://localhost:3000"
else
  warn "Frontend is starting — check http://localhost:3000 in a few seconds"
fi

# ── Done ───────────────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    Setup Complete!                          ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║                                                            ║"
echo "║  Frontend:  http://localhost:3000                           ║"
echo "║  Backend:   http://localhost:8080                           ║"
echo "║  Swagger:   http://localhost:8080/api/v1/docs               ║"
echo "║  Mailhog:   http://localhost:8025                           ║"
echo "║  RabbitMQ:  http://localhost:15672 (guest/guest)            ║"
echo "║                                                            ║"
echo "║  Test accounts:                                            ║"
echo "║    admin@iloveshopping.com / Admin123!                      ║"
echo "║    user@iloveshopping.com  / User123!                       ║"
echo "║                                                            ║"
echo "║  Stop:  ./setup.sh stop                                    ║"
echo "║  Logs:  tail -f /tmp/ilove-shopping-backend.log             ║"
echo "║                                                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ── Handle "stop" argument ─────────────────────────────────────────────────────
if [ "${1:-}" = "stop" ]; then
  step "Stopping all services"
  if [ -f /tmp/ilove-shopping-backend.pid ]; then
    kill "$(cat /tmp/ilove-shopping-backend.pid)" 2>/dev/null && ok "Backend stopped" || warn "Backend was not running"
    rm -f /tmp/ilove-shopping-backend.pid
  fi
  # Kill any next dev on port 3000
  if command -v lsof &>/dev/null; then
    lsof -ti :3000 2>/dev/null | xargs -r kill 2>/dev/null && ok "Frontend stopped" || warn "Frontend was not running"
  fi
  $DOCKER_COMPOSE -f docker/docker-compose.yml stop
  ok "Docker services stopped"
  echo ""
fi
