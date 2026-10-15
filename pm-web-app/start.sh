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
mkdir -p "$SCRIPT_DIR/target"
PID_FILE="$SCRIPT_DIR/target/pm-web-app.pid"
LOG_FILE="$SCRIPT_DIR/target/pm-web-app.log"
NODE_MODULES="$SCRIPT_DIR/node_modules"

# Also clean up legacy dot-file if present
rm -f "$SCRIPT_DIR/.pm-web-app.pid" 2>/dev/null || true

# 1. Check if already running
if [ -f "$PID_FILE" ]; then
    EXISTING_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$EXISTING_PID" ] && kill -0 "$EXISTING_PID" 2>/dev/null; then
        echo "pm-web-app is already running (PID: $EXISTING_PID)."
        echo "Local URL: http://localhost:5173"
        echo "Logs: $LOG_FILE"
        echo "To stop: $SCRIPT_DIR/stop.sh"
        exit 0
    else
        rm -f "$PID_FILE"
    fi
fi

# 2. Smart build check (incremental, no clean)
BUILD_NEEDED=false
if [ ! -d "$NODE_MODULES" ]; then
    BUILD_NEEDED=true
else
    # Check if package.json is newer than node_modules
    PACKAGE_JSON="$SCRIPT_DIR/package.json"
    if [ -f "$PACKAGE_JSON" ]; then
        PKG_MTIME="$(stat -f "%m" "$PACKAGE_JSON" 2>/dev/null || stat -c "%Y" "$PACKAGE_JSON" 2>/dev/null || echo 0)"
        NM_MTIME="$(stat -f "%m" "$NODE_MODULES" 2>/dev/null || stat -c "%Y" "$NODE_MODULES" 2>/dev/null || echo 0)"
        if [ "${PKG_MTIME:-0}" -gt "${NM_MTIME:-0}" ]; then
            BUILD_NEEDED=true
        fi
    fi
fi

if [ "$BUILD_NEEDED" = true ]; then
    echo "Building pm-web-app dependencies (incremental ./mvnw -am, without clean)..."
    "$REPO_ROOT/mvnw" compile -pl pm-web-app -am -DskipTests
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
        *)
            PASSED_ARGS+=("$1")
            shift
            ;;
    esac
done

cd "$SCRIPT_DIR"

if [ "$FOREGROUND" = true ]; then
    echo "Starting pm-web-app in foreground..."
    echo "$$" > "$PID_FILE"
    trap 'rm -f "$PID_FILE"' EXIT INT TERM
    if [ ${#PASSED_ARGS[@]} -gt 0 ]; then
        exec "$NODE_BIN" "$VITE_BIN" "${PASSED_ARGS[@]}"
    else
        exec "$NODE_BIN" "$VITE_BIN"
    fi
else
    # Check if port 5173 is already in use
    EXISTING_PORT_PID="$(lsof -ti :5173 2>/dev/null || true)"
    if [ -n "$EXISTING_PORT_PID" ]; then
        echo "Port 5173 is already in use by process $EXISTING_PORT_PID."
        echo "To stop: $SCRIPT_DIR/stop.sh"
        exit 1
    fi

    echo "Starting pm-web-app in background..."
    : > "$LOG_FILE"

    # Spawn Vite in a detached session so esbuild and worker daemons survive terminal exit
    if [ ${#PASSED_ARGS[@]} -gt 0 ]; then
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
" "$PID_FILE" "$LOG_FILE" "$VITE_BIN" "${PASSED_ARGS[@]}"
    else
        "$NODE_BIN" -e "
const { spawn } = require('child_process');
const fs = require('fs');
const out = fs.openSync(process.argv[2], 'w');
const args = [process.argv[3]];
const child = spawn(process.execPath, args, {
  detached: true,
  stdio: ['ignore', out, out]
});
child.unref();
fs.writeFileSync(process.argv[1], String(child.pid));
" "$PID_FILE" "$LOG_FILE" "$VITE_BIN"
    fi

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

    echo "pm-web-app started successfully (PID: $PID)."
    echo "Local:   http://localhost:5173"
    echo "Logs:    $LOG_FILE"
    echo "To stop: $SCRIPT_DIR/stop.sh"
fi
