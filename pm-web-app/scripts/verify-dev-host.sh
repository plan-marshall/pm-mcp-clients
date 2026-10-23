#!/usr/bin/env bash
#
# Copyright © 2026-present Oliver Wolff
#
# SPDX-License-Identifier: LicenseRef-Proprietary
#
# All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
# No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
# the root of this repository.
#
# Proves that the host `start.sh` prints is one Vite accepts. A browser sends its lower-case
# `Host` header, and Vite compares that string with `server.allowedHosts` by exact equality, so
# the printed name must be the canonical lower-case form. `curl --resolve` supplies the
# name-to-loopback mapping, leaving DNS/mDNS out of the result.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
# The host `start.sh` actually prints: `reachable_host` is the same resolver, so an unset
# `PM_WEB_HOST` is tested by the machine's real name rather than a synthetic one.
# shellcheck source=connection-urls.sh
source "$SCRIPT_DIR/connection-urls.sh"
PORT="${PM_VERIFY_DEV_HOST_PORT:-5199}"
CONFIGURED_HOST="${PM_WEB_HOST:-$(reachable_host)}"
LOWER_HOST="$(printf '%s' "$CONFIGURED_HOST" | tr '[:upper:]' '[:lower:]')"
LOWER_HOST="${LOWER_HOST%.}"
UNKNOWN_HOST="not-this-machine.local"
LOG_FILE="$(mktemp "${TMPDIR:-/tmp}/pm-verify-dev-host.XXXXXX")"
SERVER_PID=""

cleanup() {
    if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
    rm -f "$LOG_FILE"
}
trap cleanup EXIT INT TERM

if [ -x "$WEB_APP_DIR/target/node/node" ]; then
    NODE_BIN="$WEB_APP_DIR/target/node/node"
elif command -v node >/dev/null 2>&1; then
    NODE_BIN="node"
else
    echo "Error: node not found in target/node or system PATH." >&2
    exit 1
fi
VITE_BIN="$WEB_APP_DIR/node_modules/vite/bin/vite.js"
if [ ! -f "$VITE_BIN" ]; then
    echo "Error: vite binary not found at $VITE_BIN" >&2
    exit 1
fi

# The test server stays on loopback; only the Host header carries the external name.
(
    cd "$WEB_APP_DIR"
    PM_WEB_HOST="$CONFIGURED_HOST" exec "$NODE_BIN" "$VITE_BIN" \
        --host 127.0.0.1 --port "$PORT" --strictPort
) >"$LOG_FILE" 2>&1 &
SERVER_PID=$!

READY=false
for _ in {1..60}; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "Error: Vite exited before accepting requests. Log output:" >&2
        cat "$LOG_FILE" >&2
        exit 1
    fi
    if curl --noproxy '*' -sS -o /dev/null "http://127.0.0.1:$PORT/" 2>/dev/null; then
        READY=true
        break
    fi
    sleep 0.5
done
if [ "$READY" = false ]; then
    echo "Error: Vite did not accept requests on 127.0.0.1:$PORT. Log output:" >&2
    cat "$LOG_FILE" >&2
    exit 1
fi

status_of() {
    # A configured proxy must not answer for the loopback server: every assertion is local.
    curl --noproxy '*' -sS -o /dev/null -w '%{http_code}' "$@"
}

FAILURES=0
check() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    printf '  %-42s %s (expected %s)\n' "$label" "$actual" "$expected"
    if [ "$actual" != "$expected" ]; then
        FAILURES=$((FAILURES + 1))
    fi
}

echo "Dev host check: configured '$CONFIGURED_HOST', printed and accepted as '$LOWER_HOST'"
check "GET / by lower-case name" 200 \
    "$(status_of --resolve "$LOWER_HOST:$PORT:127.0.0.1" "http://$LOWER_HOST:$PORT/")"
check "GET /pair by lower-case name" 200 \
    "$(status_of --resolve "$LOWER_HOST:$PORT:127.0.0.1" "http://$LOWER_HOST:$PORT/pair")"
check "GET / with the lower-case Host header" 200 \
    "$(status_of -H "Host: $LOWER_HOST:$PORT" "http://127.0.0.1:$PORT/")"
check "GET / with an unknown Host header" 403 \
    "$(status_of -H "Host: $UNKNOWN_HOST:$PORT" "http://127.0.0.1:$PORT/")"

if [ "$FAILURES" -ne 0 ]; then
    echo "verify-dev-host: $FAILURES case(s) failed" >&2
    exit 1
fi

echo 'verify-dev-host: all cases passed'
