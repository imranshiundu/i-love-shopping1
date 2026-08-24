#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$REPO_DIR/docker/docker-compose.yml"
COMPOSE=(docker compose)

RED=$'\033[1;31m'
GRN=$'\033[1;32m'
YEL=$'\033[1;33m'
BLU=$'\033[1;34m'
RST=$'\033[0m'

log(){ printf "%s[i-love-shopping]%s %s\n" "$BLU" "$RST" "$1"; }
ok(){ printf "%s[i-love-shopping]%s %s\n" "$GRN" "$RST" "$1"; }
warn(){ printf "%s[i-love-shopping]%s %s\n" "$YEL" "$RST" "$1"; }
die(){ printf "%s[i-love-shopping]%s %s\n" "$RED" "$RST" "$1"; exit 1; }

have(){ command -v "$1" >/dev/null 2>&1; }

OS="linux"
case "$(uname -s)" in
  Darwin) OS="mac" ;;
  MINGW*|MSYS*|CYGWIN*) OS="win" ;;
esac

install_system_pkg() {
  if have sudo; then
    if have apt-get; then sudo apt-get update -qq && sudo apt-get install -y "$1"
    elif have dnf; then sudo dnf install -y "$1"
    elif have yum; then sudo yum install -y "$1"
    elif have apk; then sudo apk add "$1"
    fi
  elif have apt-get; then apt-get install -y "$1"
  elif have dnf; then dnf install -y "$1"
  fi
}

check_docker() {
  if ! have docker; then
    warn "Docker is not installed. Attempting to install it..."
    if [ "$OS" = "mac" ]; then
      if have brew; then brew install --cask docker; else die "Install Docker Desktop from https://docs.docker.com/get-docker/ and run this script again."; fi
    elif [ "$OS" = "win" ]; then
      if have choco; then choco install docker-desktop -y; else die "Install Docker Desktop from https://docs.docker.com/get-docker/ and run this script again."; fi
    else
      install_system_pkg docker.io || install_system_pkg docker
    fi
    have docker || die "Docker is still missing. Install it manually and run this script again."
  fi
  if ! docker info >/dev/null 2>&1; then
    die "The Docker daemon is not running. Start Docker Desktop (or the docker service) and run this script again."
  fi
  if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
  elif have docker-compose; then
    COMPOSE=(docker-compose)
  else
    warn "Docker Compose is not installed. Attempting to install it..."
    if [ "$OS" = "mac" ]; then
      have brew && brew install docker-compose
    elif [ "$OS" = "win" ]; then
      have choco && choco install docker-compose -y
    else
      install_system_pkg docker-compose-plugin || install_system_pkg docker-compose-v2 || install_system_pkg docker-compose
    fi
    (docker compose version >/dev/null 2>&1 || have docker-compose) || die "Docker Compose is still missing. Install it manually and run this script again."
  fi
  ok "Docker and Docker Compose are ready."
}

check_java() {
  if have java && java -version 2>&1 | head -1 | grep -qE '"2[1-9]'; then
    ok "Java 21 found: $(java -version 2>&1 | head -1)"
  else
    warn "Java 21 is required. Attempting to install it..."
    if [ "$OS" = "mac" ]; then
      have brew && (brew install --cask temurin || brew install openjdk@21)
    elif [ "$OS" = "win" ]; then
      have choco && choco install temurin21 -y
    else
      install_system_pkg openjdk-21-jdk || install_system_pkg java-21-openjdk || install_system_pkg openjdk21
    fi
    have java && java -version 2>&1 | head -1 | grep -qE '"2[1-9]' || die "Java 21 is required. Install it manually and run this script again."
  fi
}

check_maven() {
  if have mvn; then
    ok "Maven found: $(mvn -version 2>/dev/null | head -1)"
  elif [ -x "$REPO_DIR/backend/mvnw" ]; then
    ok "Using the Maven wrapper (backend/mvnw)."
  else
    warn "Maven is not installed. Attempting to install it..."
    if [ "$OS" = "mac" ]; then
      have brew && brew install maven
    elif [ "$OS" = "win" ]; then
      have choco && choco install maven -y
    else
      install_system_pkg maven
    fi
    have mvn || die "Maven is missing. Install it manually (or keep the Maven wrapper) and run this script again."
  fi
}

check_node() {
  if have node && node -v 2>/dev/null | grep -qE 'v2[0-9]+'; then
    ok "Node.js found: $(node -v)"
  else
    warn "Node.js is required for the frontend. Attempting to install it..."
    if [ "$OS" = "mac" ]; then
      have brew && brew install node
    elif [ "$OS" = "win" ]; then
      have choco && choco install nodejs-lts -y
    else
      install_system_pkg nodejs || install_system_pkg npm
    fi
    have node || die "Node.js is required. Install it manually and run this script again."
  fi
}

run_backend() {
  if have mvn; then mvn spring-boot:run; else ./mvnw spring-boot:run; fi
}

run_frontend() {
  cd "$REPO_DIR/frontend"
  npm install 2>/dev/null
  npm run dev
}

wait_healthy() {
  local svc="$1"
  local i
  for i in $(seq 1 30); do
    if "${COMPOSE[@]}" -f "$COMPOSE_FILE" ps "$svc" 2>/dev/null | grep -q healthy; then
      return 0
    fi
    sleep 3
  done
  die "Timed out waiting for $svc to become healthy. Check: ${COMPOSE[*]} -f $COMPOSE_FILE ps"
}

run_all_docker() {
  ok "Starting PostgreSQL, Redis, Mailhog, RabbitMQ, API and Frontend with Docker Compose in the foreground."
  ok "Frontend: http://localhost:3000   API: http://localhost:8080/api/v1   Swagger UI: http://localhost:8080/api/v1/docs"
  ok "RabbitMQ Management: http://localhost:15672 (guest/guest)   Mailhog UI: http://localhost:8025"
  log "Press Ctrl+C to stop all services."
  echo
  "${COMPOSE[@]}" -f "$COMPOSE_FILE" up
}

run_local_backend() {
  check_java
  check_maven
  check_node
  log "Starting PostgreSQL, Redis, Mailhog and RabbitMQ with Docker Compose..."
  "${COMPOSE[@]}" -f "$COMPOSE_FILE" up -d postgres redis mailhog rabbitmq
  log "Waiting for dependencies to become healthy..."
  wait_healthy postgres
  wait_healthy redis
  wait_healthy rabbitmq
  ok "Dependencies are up."
  export DATABASE_URL="jdbc:postgresql://localhost:5433/iloveshopping?stringtype=unspecified"
  export DATABASE_USER=iloveshopping
  export DATABASE_PASSWORD=iloveshopping
  export REDIS_HOST=localhost
  export REDIS_PORT=6380
  export RECAPTCHA_SECRET_KEY=dev-test-secret
  export RECAPTCHA_SITE_KEY=dev-test-site
  export JWT_ACCESS_SECRET=dev-access-secret-min-32-chars-long-for-test
  export JWT_REFRESH_SECRET=dev-refresh-secret-min-32-chars-long-for-test
  export MAIL_HOST=localhost
  export MAIL_PORT=1025
  export FRONTEND_URL=http://localhost:3000
  export CORS_ALLOWED_ORIGINS=http://localhost:3000
  export RABBITMQ_HOST=localhost
  export RABBITMQ_PORT=5672
  export RABBITMQ_USERNAME=iloveshopping
  export RABBITMQ_PASSWORD=iloveshopping
  ok "Frontend: http://localhost:3000   API: http://localhost:8080/api/v1   Swagger UI: http://localhost:8080/api/v1/docs"
  ok "RabbitMQ Management: http://localhost:15672 (guest/guest)   Mailhog UI: http://localhost:8025"
  log "Press Ctrl+C to stop the API and the containers."
  echo
  cd "$REPO_DIR/backend"
  trap 'log "Stopping development containers..."; "${COMPOSE[@]}" -f "$COMPOSE_FILE" down' EXIT INT TERM
  run_backend &
  run_frontend &
  wait
}

main() {
  cd "$REPO_DIR"
  log "i-love-shopping development environment"
  log "Repository: $REPO_DIR"
  echo
  check_docker
  echo
  log "How do you want to run the project?"
  log "  1) Everything in Docker (PostgreSQL + Redis + Mailhog + RabbitMQ + API + Frontend) - only Docker"
  log "  2) Dependencies in Docker + run API & Frontend locally - requires Docker, Java 21, Maven and Node.js"
  echo
  read -rp "Choose an option [1/2]: " choice
  case "$choice" in
    1) run_all_docker ;;
    2) run_local_backend ;;
    *) die "Invalid choice. Run the script again and pick 1 or 2." ;;
  esac
}

main