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
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# The address resolution and the printed coordinates; sourced so the matrix has a test seam.
# shellcheck source=scripts/connection-urls.sh
source "$SCRIPT_DIR/scripts/connection-urls.sh"
mkdir -p "$SCRIPT_DIR/target"
PID_FILE="$SCRIPT_DIR/target/pm-web-app.pid"
PORT_FILE="$SCRIPT_DIR/target/pm-web-app.port"
LOG_FILE="$SCRIPT_DIR/target/pm-web-app.log"
NODE_MODULES="$SCRIPT_DIR/node_modules"

# Also clean up legacy dot-file if present
rm -f "$SCRIPT_DIR/.pm-web-app.pid" 2>/dev/null || true

# The port the running server listens on; 5173 unless overridden.
web_port_from_args() {
    local web_port=5173
    local expect_port=false
    for arg in "$@"; do
        if [ "$expect_port" = true ]; then
            web_port="$arg"
            expect_port=false
        elif [ "$arg" = "-p" ] || [ "$arg" = "--port" ]; then
            expect_port=true
        elif [[ "$arg" == --port=* ]]; then
            web_port="${arg#--port=}"
        fi
    done
    printf '%s' "$web_port"
}

# The code a browser can pair with right now.
#
# The live /mock/state is preferred; when the previous code was consumed, POST /mock/pairing
# issues the replacement the web-only development loop has no terminal client to ask for. The
# log file is the last fallback.
current_pairing_code() {
    local pairing_code=""
    local mock_state
    mock_state="$(curl -s -m 0.5 http://127.0.0.1:7420/mock/state 2>/dev/null || true)"
    if [ -n "$mock_state" ]; then
        pairing_code="$(echo "$mock_state" | grep -o '"pairing_code":"[^"]*' | cut -d'"' -f4 || true)"
    fi

    if [ -z "$pairing_code" ]; then
        local issued
        issued="$(curl -s -m 1 -X POST http://127.0.0.1:7420/mock/pairing 2>/dev/null || true)"
        if [ -n "$issued" ]; then
            pairing_code="$(echo "$issued" | grep -o '"code":"[^"]*' | cut -d'"' -f4 || true)"
        fi
    fi

    if [ -z "$pairing_code" ]; then
        local mock_pid_file="$REPO_ROOT/pm-mock-runtime/target/pm-mock-runtime.pid"
        local mock_log_file="$REPO_ROOT/pm-mock-runtime/target/pm-mock-runtime.log"

        if [ -f "$mock_pid_file" ] && [ -f "$mock_log_file" ]; then
            local mock_pid
            mock_pid="$(cat "$mock_pid_file" 2>/dev/null || true)"
            if [ -n "$mock_pid" ] && kill -0 "$mock_pid" 2>/dev/null; then
                if grep -q "Loopback Web Port:  [0-9]" "$mock_log_file" 2>/dev/null; then
                    pairing_code="$(grep -E "Pairing Code:" "$mock_log_file" 2>/dev/null | tail -n1 | awk '{print $NF}' || true)"
                fi
            fi
        fi
    fi

    printf '%s' "$pairing_code"
}

# 1. Check if already running
if [ -f "$PID_FILE" ]; then
    EXISTING_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$EXISTING_PID" ] && kill -0 "$EXISTING_PID" 2>/dev/null; then
        local_port="$(web_port_from_args "$@")"
        if [ -f "$PORT_FILE" ]; then
            saved_port="$(cat "$PORT_FILE" 2>/dev/null || true)"
            case "$saved_port" in
                ''|*[!0-9]*) ;;
                *) local_port="$saved_port" ;;
            esac
        fi
        echo "pm-web-app is already running (PID: $EXISTING_PID)."
        render_connection_urls "$local_port" "$(current_pairing_code)"
        echo "Logs:    $LOG_FILE"
        echo "To stop: $SCRIPT_DIR/stop.sh"
        exit 0
    else
        rm -f "$PID_FILE"
    fi
fi

# 2. Smart build check (incremental, no clean)
BUILD_NEEDED=false
if [ ! -d "$NODE_MODULES" ] || [ ! -f "$NODE_MODULES/vite/bin/vite.js" ]; then
    BUILD_NEEDED=true
else
    # Any file that changes the dependency tree or the Vite setup is newer than node_modules
    # (npm updates that directory on install).
    NM_MTIME="$(stat -f "%m" "$NODE_MODULES" 2>/dev/null || stat -c "%Y" "$NODE_MODULES" 2>/dev/null || echo 0)"
    for watched in package.json package-lock.json vite.config.ts pom.xml; do
        watched_file="$SCRIPT_DIR/$watched"
        if [ -f "$watched_file" ]; then
            FILE_MTIME="$(stat -f "%m" "$watched_file" 2>/dev/null || stat -c "%Y" "$watched_file" 2>/dev/null || echo 0)"
            if [ "${FILE_MTIME:-0}" -gt "${NM_MTIME:-0}" ]; then
                BUILD_NEEDED=true
                break
            fi
        fi
    done
fi

if [ "$BUILD_NEEDED" = true ]; then
    echo "Building pm-web-app dependencies (incremental ./mvnw -am, without clean)..."
    (cd "$REPO_ROOT" && ./mvnw compile -pl pm-web-app -am -DskipTests)
fi

# 3. Locate node and vite
NODE_BIN=""
if [ -x "$SCRIPT_DIR/target/node/node" ]; then
    NODE_BIN="$SCRIPT_DIR/target/node/node"
elif command -v node >/dev/null 2>&1; then
    NODE_BIN="node"
else
    echo "Error: node not found in target/node or system PATH." >&2
    exit 1
fi

VITE_BIN="$NODE_MODULES/vite/bin/vite.js"
if [ ! -f "$VITE_BIN" ]; then
    echo "Error: vite binary not found at $VITE_BIN" >&2
    exit 1
fi

# 4. Parse options
FOREGROUND=false
PASSED_ARGS=()

while [ $# -gt 0 ]; do
    case "$1" in
        -f|--foreground)
            FOREGROUND=true
            shift
            ;;
        -p)
            # Vite only knows the long form: translate it instead of forwarding an unknown option.
            if [ $# -lt 2 ]; then
                echo "Error: -p requires a port." >&2
                exit 1
            fi
            PASSED_ARGS+=(--port "$2")
            shift 2
            ;;
        *)
            PASSED_ARGS+=("$1")
            shift
            ;;
    esac
done

WEB_PORT="$(web_port_from_args ${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"})"

cd "$SCRIPT_DIR"

if [ "$FOREGROUND" = true ]; then
    echo "Starting pm-web-app in foreground..."
    render_connection_urls "$WEB_PORT" "$(current_pairing_code)"
    echo "$$" > "$PID_FILE"
    echo "$WEB_PORT" > "$PORT_FILE"
    trap 'rm -f "$PID_FILE" "$PORT_FILE"' EXIT INT TERM
    exec "$NODE_BIN" "$VITE_BIN" ${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"}
else
    # Check if the requested port is already in use
    EXISTING_PORT_PID="$(lsof -ti ":$WEB_PORT" 2>/dev/null || true)"
    if [ -n "$EXISTING_PORT_PID" ]; then
        echo "Port $WEB_PORT is already in use by process $EXISTING_PORT_PID."
        echo "To stop: $SCRIPT_DIR/stop.sh"
        exit 1
    fi

    echo "Starting pm-web-app in background..."
    : > "$LOG_FILE"

    # Spawn Vite in a detached session so esbuild and worker daemons survive terminal exit
    "$NODE_BIN" -e "
const { spawn } = require('child_process');
const fs = require('fs');
const out = fs.openSync(process.argv[2], 'w');
const args = [process.argv[3], ...process.argv.slice(4)];
const child = spawn(process.execPath, args, {
  detached: true,
  stdio: ['ignore', out, out]
});
child.unref();
fs.writeFileSync(process.argv[1], String(child.pid));
" "$PID_FILE" "$LOG_FILE" "$VITE_BIN" ${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"}

    PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -z "$PID" ]; then
        echo "Error: Failed to obtain PID for pm-web-app." >&2
        exit 1
    fi

    # Wait briefly for Vite dev server to bind
    for _ in {1..20}; do
        if ! kill -0 "$PID" 2>/dev/null; then
            echo "Error: pm-web-app failed to start. Check $LOG_FILE for details." >&2
            rm -f "$PID_FILE"
            exit 1
        fi
        if grep -q "Local:" "$LOG_FILE" 2>/dev/null || grep -q "ready in" "$LOG_FILE" 2>/dev/null; then
            break
        fi
        sleep 0.5
    done

    echo "$WEB_PORT" > "$PORT_FILE"
    echo "pm-web-app started successfully (PID: $PID)."
    render_connection_urls "$WEB_PORT" "$(current_pairing_code)"
    echo "Logs:    $LOG_FILE"
    echo "To stop: $SCRIPT_DIR/stop.sh"
fi
