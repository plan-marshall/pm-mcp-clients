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

# Free the port as well: a runtime started outside this worktree keeps no PID file here. When
# this worktree's own process was stopped above, the port is its business and we leave any
# other runtime alone.
if [ "$STOPPED" = false ]; then
    PORT="$(grep -oE 'Loopback Web Port:  [0-9]+' "$LOG_FILE" 2>/dev/null | tail -n1 | grep -oE '[0-9]+' || true)"
    [ -n "$PORT" ] || PORT=7420
    PORT_PIDS="$(lsof -ti ":$PORT" 2>/dev/null || true)"
    for pid in $PORT_PIDS; do
        if is_mock_process "$pid"; then
            echo "Stopping pm-mock-runtime listening on port $PORT (PID: $pid)..."
            stop_pid "$pid"
            STOPPED=true
        else
            echo "Port $PORT is held by a non-mock process (PID: $pid); leaving it alone."
        fi
    done

    # Wait for the port to be released so the next start binds immediately.
    if [ -n "$PORT_PIDS" ]; then
        for _ in {1..50}; do
            if [ -z "$(lsof -ti ":$PORT" 2>/dev/null || true)" ]; then
                break
            fi
            sleep 0.1
        done
        REMAINING="$(lsof -ti ":$PORT" 2>/dev/null || true)"
        if [ -n "$REMAINING" ]; then
            for pid in $REMAINING; do
                if is_mock_process "$pid"; then
                    echo "Port $PORT is still held by pm-mock-runtime (PID: $pid)." >&2
                    exit 1
                fi
            done
        fi
    fi
fi

if [ "$STOPPED" = true ]; then
    echo "pm-mock-runtime stopped successfully."
else
    echo "pm-mock-runtime is not running."
fi
