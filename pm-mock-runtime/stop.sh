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
PID_FILE="$SCRIPT_DIR/target/pm-mock-runtime.pid"
# Also clean up legacy dot-file if present
rm -f "$SCRIPT_DIR/.pm-mock-runtime.pid" 2>/dev/null || true

if [ ! -f "$PID_FILE" ]; then
    echo "pm-mock-runtime is not running (no PID file at $PID_FILE)."
    exit 0
fi

PID="$(cat "$PID_FILE" 2>/dev/null || true)"

if [ -z "$PID" ]; then
    rm -f "$PID_FILE"
    echo "Stale empty PID file removed. pm-mock-runtime is not running."
    exit 0
fi

if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "Process $PID is not running. Cleaned up stale PID file."
    exit 0
fi

echo "Stopping pm-mock-runtime (PID: $PID)..."
kill -TERM "$PID" 2>/dev/null || true

# Wait up to 5 seconds for clean shutdown
for _ in {1..50}; do
    if ! kill -0 "$PID" 2>/dev/null; then
        break
    fi
    sleep 0.1
done

if kill -0 "$PID" 2>/dev/null; then
    echo "Process $PID did not exit gracefully, terminating with SIGKILL..."
    kill -KILL "$PID" 2>/dev/null || true
fi

rm -f "$PID_FILE"
echo "pm-mock-runtime stopped successfully."
