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
PID_FILE="$SCRIPT_DIR/target/pm-web-app.pid"
PORT_FILE="$SCRIPT_DIR/target/pm-web-app.port"

# Also clean up legacy dot-file if present
rm -f "$SCRIPT_DIR/.pm-web-app.pid" 2>/dev/null || true

STOPPED=false

if [ -f "$PID_FILE" ]; then
    PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
        echo "Stopping pm-web-app (PID: $PID)..."

        # Terminate child processes (e.g. vite / esbuild)
        pkill -P "$PID" 2>/dev/null || true
        kill "$PID" 2>/dev/null || true

        # Wait up to 5 seconds for clean exit
        for _ in {1..10}; do
            if ! kill -0 "$PID" 2>/dev/null; then
                break
            fi
            sleep 0.5
        done

        # Force kill if still running
        if kill -0 "$PID" 2>/dev/null; then
            echo "Process did not terminate gracefully; sending SIGKILL..."
            pkill -9 -P "$PID" 2>/dev/null || true
            kill -9 "$PID" 2>/dev/null || true
        fi
        rm -f "$PID_FILE" "$PORT_FILE"
        echo "pm-web-app stopped successfully."
        STOPPED=true
    else
        rm -f "$PID_FILE" "$PORT_FILE"
    fi
fi

if [ "$STOPPED" = false ]; then
    echo "pm-web-app is not running."
fi
