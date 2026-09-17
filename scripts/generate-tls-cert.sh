#!/usr/bin/env bash
# =============================================================
# i-love-shopping — Self-signed TLS certificate generator
# =============================================================
# Generates cert.pem + key.pem for the production nginx reverse
# proxy (docker/nginx/nginx.conf serves :443 with these files).
#
# Usage:
#   ./scripts/generate-tls-cert.sh                 # default CN=localhost
#   ./scripts/generate-tls-cert.sh mydomain.com    # custom Common Name
#   ./scripts/generate-tls-cert.sh --force         # overwrite existing certs
# =============================================================
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SSL_DIR="$REPO_DIR/docker/nginx/ssl"
CN="${1:-localhost}"
DAYS=825  # ~2 years, the practical max for private certs in most browsers

if [ "$CN" = "--force" ]; then
  CN="localhost"
  FORCE=true
fi

if [ -f "$SSL_DIR/cert.pem" ] && [ -f "$SSL_DIR/key.pem" ] && [ "${FORCE:-false}" != "true" ]; then
  echo "[tls] Certificates already exist at $SSL_DIR (use --force to regenerate)."
  exit 0
fi

command -v openssl >/dev/null 2>&1 || { echo "[tls] openssl not installed."; exit 1; }

mkdir -p "$SSL_DIR"

SUBJECT="/C=KE/ST=Nairobi/L=Nairobi/O=i-love-shopping/CN=$CN"

openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "$SSL_DIR/key.pem" \
  -out "$SSL_DIR/cert.pem" \
  -days "$DAYS" \
  -subj "$SUBJECT" \
  -addext "subjectAltName=DNS:$CN,DNS:localhost,IP:127.0.0.1" \
  -addext "keyUsage=digitalSignature,keyEncipherment" \
  -addext "extendedKeyUsage=serverAuth" 2>/dev/null

chmod 600 "$SSL_DIR/key.pem"
chmod 644 "$SSL_DIR/cert.pem"

echo "[tls] Self-signed certificate generated:"
echo "[tls]   cert: $SSL_DIR/cert.pem"
echo "[tls]   key:  $SSL_DIR/key.pem"
echo "[tls]   CN:   $CN  (SANs: $CN, localhost, 127.0.0.1)"
echo "[tls]   valid for $DAYS days"
echo
echo "[tls] Browsers will warn about the self-signed cert — that is expected."
echo "[tls] Docker prod (docker compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up)"
echo "[tls] mounts these into nginx automatically. Accept the warning to proceed."
