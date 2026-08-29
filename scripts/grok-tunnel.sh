#!/usr/bin/env bash
# ngrok tunnel for M-Pesa/Stripe/Flutterwave callbacks.
#
# Usage:
#   ./scripts/grok-tunnel.sh start    # Start ngrok tunnel to :8080
#   ./scripts/grok-tunnel.sh stop     # Kill ngrok
#   ./scripts/grok-tunnel.sh status   # Show tunnel URL
#
# Once running, set the public URL in your .env:
#   MPESA_CALLBACK_URL=https://xxxx.ngrok.io/api/v1/orders/payments/mpesa/callback
#   MPESA_TIMEOUT_URL=https://xxxx.ngrok.io/api/v1/orders/payments/mpesa/timeout
#   STRIPE_WEBHOOK_SECRET=<from `stripe listen --print-secret` or ngrok webhook inspector>
#
# For Stripe, prefer using the Stripe CLI:
#   stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook
# It handles signature verification and event replay automatically.

set -uo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NGROK_LOG="/tmp/ngrok.log"
NGROK_PIDFILE="/tmp/ngrok.pid"

cmd=${1:-status}

case "$cmd" in
  start)
    if [[ -f "$NGROK_PIDFILE" ]] && kill -0 "$(cat $NGROK_PIDFILE)" 2>/dev/null; then
      echo "ngrok already running (PID $(cat $NGROK_PIDFILE))"
      exit 0
    fi
    if ! command -v ngrok >/dev/null 2>&1; then
      echo "ngrok not installed. Install: brew install ngrok / snap install ngrok"
      exit 1
    fi
    echo "Starting ngrok tunnel to :8080..."
    nohup ngrok http 8080 --log "$NGROK_LOG" > /dev/null 2>&1 &
    echo $! > "$NGROK_PIDFILE"
    sleep 3
    # Get the public URL from the ngrok API
    URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | python3 -c "
import sys, json
d = json.load(sys.stdin)
for t in d.get('tunnels', []):
    if t.get('proto') == 'https':
        print(t['public_url']); break
" 2>/dev/null)
    if [[ -n "$URL" ]]; then
      echo "ngrok tunnel: $URL"
      echo ""
      echo "Set these in your .env:"
      echo "  MPESA_CALLBACK_URL=${URL}/api/v1/orders/payments/mpesa/callback"
      echo "  MPESA_TIMEOUT_URL=${URL}/api/v1/orders/payments/mpesa/timeout"
      echo ""
      echo "For Stripe, use the Stripe CLI instead:"
      echo "  stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook"
    else
      echo "Failed to get tunnel URL. Check $NGROK_LOG"
    fi
    ;;

  stop)
    if [[ -f "$NGROK_PIDFILE" ]]; then
      kill "$(cat $NGROK_PIDFILE)" 2>/dev/null || true
      rm -f "$NGROK_PIDFILE"
      echo "ngrok stopped"
    else
      echo "ngrok not running"
    fi
    ;;

  status)
    if [[ -f "$NGROK_PIDFILE" ]] && kill -0 "$(cat $NGROK_PIDFILE)" 2>/dev/null; then
      URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | python3 -c "
import sys, json
d = json.load(sys.stdin)
for t in d.get('tunnels', []):
    if t.get('proto') == 'https':
        print(t['public_url']); break
" 2>/dev/null)
      echo "ngrok running (PID $(cat $NGROK_PIDFILE))"
      echo "  Public URL: ${URL:-<inspect at http://localhost:4040>}"
    else
      echo "ngrok not running. Start with: $0 start"
    fi
    ;;

  *)
    echo "Usage: $0 {start|stop|status}"
    exit 1
    ;;
esac
