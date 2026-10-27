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
PID_FILE="$SCRIPT_DIR/target/pm-operator.pid"
# Also clean up legacy dot-file if present
rm -f "$SCRIPT_DIR/.pm-operator.pid" 2>/dev/null || true

STOPPED_BACKGROUND=false
if [ -f "$PID_FILE" ]; then
    PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
        echo "Stopping pm-operator (PID: $PID)..."
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
            kill -9 "$PID" 2>/dev/null || true
        fi
        rm -f "$PID_FILE"
        echo "pm-operator stopped successfully."
        STOPPED_BACKGROUND=true
    else
        rm -f "$PID_FILE"
        echo "Stale PID file removed."
    fi
fi

# A foreground run writes no PID file, so it is found by its process name; this also reaches a
# foreground run that lives beside the background one.
RUNNING_PIDS="$(pgrep -f "de.planmarshall.operator.PmOperator" 2>/dev/null || true)"
if [ -n "$RUNNING_PIDS" ]; then
    echo "Stopping pm-operator process(es): $RUNNING_PIDS..."
    kill $RUNNING_PIDS 2>/dev/null || true
    echo "pm-operator stopped successfully."
elif [ "$STOPPED_BACKGROUND" = false ]; then
    echo "pm-operator is not running."
fi
