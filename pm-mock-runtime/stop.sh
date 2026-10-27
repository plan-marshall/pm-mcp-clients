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
LOG_FILE="$SCRIPT_DIR/target/pm-mock-runtime.log"
# Also clean up legacy dot-file if present
rm -f "$SCRIPT_DIR/.pm-mock-runtime.pid" 2>/dev/null || true

STOPPED=false

stop_pid() {
    local pid="$1"
    kill -TERM "$pid" 2>/dev/null || true
    for _ in {1..50}; do
        if ! kill -0 "$pid" 2>/dev/null; then
            return 0
        fi
        sleep 0.1
    done
    if kill -0 "$pid" 2>/dev/null; then
        echo "Process $pid did not exit gracefully, terminating with SIGKILL..."
        kill -KILL "$pid" 2>/dev/null || true
    fi
}

# A mock runtime can be started from another worktree or through `mvn exec:java`, so a process
# on the mock's web port is ours to stop when its command line says it is the mock.
is_mock_process() {
    ps -p "$1" -o command= 2>/dev/null | grep -qE 'MockRuntimeCli|pm-mock-runtime'
}

if [ -f "$PID_FILE" ]; then
    PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null && is_mock_process "$PID"; then
        echo "Stopping pm-mock-runtime (PID: $PID)..."
        stop_pid "$PID"
        STOPPED=true
    fi
    rm -f "$PID_FILE"
fi

if [ "$STOPPED" = true ]; then
    echo "pm-mock-runtime stopped successfully."
else
    echo "pm-mock-runtime is not running."
fi
