#!/usr/bin/env bash
set -e

# Always ensure we are running from the project root directory
cd "$(dirname "${BASH_SOURCE[0]}")"

# Forward to the actual development script
bash scripts/dev.sh
