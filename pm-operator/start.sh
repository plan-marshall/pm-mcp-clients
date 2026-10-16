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
PID_FILE="$SCRIPT_DIR/target/pm-operator.pid"
LOG_FILE="$SCRIPT_DIR/target/pm-operator.log"

# 1. Check if already running in background
if [ -f "$PID_FILE" ]; then
    EXISTING_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$EXISTING_PID" ] && kill -0 "$EXISTING_PID" 2>/dev/null; then
        echo "pm-operator is already running (PID: $EXISTING_PID)."
        echo "To stop: $SCRIPT_DIR/stop.sh"
        exit 0
    else
        rm -f "$PID_FILE"
    fi
fi

# 2. Smart build check (incremental, no clean)
CLASSES_DIR="$SCRIPT_DIR/target/classes"
CP_FILE="$SCRIPT_DIR/target/classpath.txt"
BUILD_NEEDED=false

if [ ! -d "$CLASSES_DIR" ] || [ ! -f "$CP_FILE" ]; then
    BUILD_NEEDED=true
else
    # Check if any sources in pm-operator or pm-api are newer than target/classes
    NEWEST_SRC="$((find "$SCRIPT_DIR/src" "$REPO_ROOT/pm-api/src" -type f -exec stat -f "%m" {} + 2>/dev/null || find "$SCRIPT_DIR/src" "$REPO_ROOT/pm-api/src" -type f -exec stat -c "%Y" {} + 2>/dev/null) | sort -nr | head -n1 || echo 0)"
    TARGET_MTIME="$(stat -f "%m" "$CLASSES_DIR" 2>/dev/null || stat -c "%Y" "$CLASSES_DIR" 2>/dev/null || echo 0)"
    if [ "${NEWEST_SRC:-0}" -gt "${TARGET_MTIME:-0}" ]; then
        BUILD_NEEDED=true
    fi
fi

if [ "$BUILD_NEEDED" = true ]; then
    echo "Building pm-operator (incremental ./mvnw -am, without clean)..."
    TMP_CP="$CP_FILE.tmp.$$"
    trap 'rm -f "$TMP_CP"' EXIT INT TERM
    (cd "$REPO_ROOT" && ./mvnw compile dependency:build-classpath -Dmdep.outputFile="$TMP_CP" -pl pm-operator -am)
    mv "$TMP_CP" "$CP_FILE"
    trap - EXIT INT TERM
fi

# 3. Parse options
DAEMON=false
PASSED_ARGS=()

while [ $# -gt 0 ]; do
    case "$1" in
        -d|--daemon)
            DAEMON=true
            shift
            ;;
        *)
            PASSED_ARGS+=("$1")
            shift
            ;;
    esac
done

# 4. Auto-detect PM_MCP_BASE from running mock runtime if not explicitly set
if [ -z "${PM_MCP_BASE:-}" ]; then
    MOCK_PID_FILE="$REPO_ROOT/pm-mock-runtime/target/pm-mock-runtime.pid"
    MOCK_LOG_FILE="$REPO_ROOT/pm-mock-runtime/target/pm-mock-runtime.log"
    if [ -f "$MOCK_PID_FILE" ] && [ -f "$MOCK_LOG_FILE" ]; then
        MOCK_PID="$(cat "$MOCK_PID_FILE" 2>/dev/null || true)"
        if [ -n "$MOCK_PID" ] && kill -0 "$MOCK_PID" 2>/dev/null; then
            DETECTED_BASE="$(grep -E "Base \(PM_MCP_BASE\):" "$MOCK_LOG_FILE" 2>/dev/null | tail -n1 | awk '{print $NF}' || true)"
            if [ -n "$DETECTED_BASE" ] && [ -d "$DETECTED_BASE" ]; then
                export PM_MCP_BASE="$DETECTED_BASE"
                echo "Using running mock runtime (PM_MCP_BASE=$PM_MCP_BASE)"
            fi
        fi
    fi
fi

CP="$CLASSES_DIR:$REPO_ROOT/pm-api/target/classes:$(cat "$CP_FILE")"
JAVA_OPTS=(-XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED)

JAVA_CMD=(java "${JAVA_OPTS[@]}" -cp "$CP" de.planmarshall.operator.PmOperator ${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"})

if [ "$DAEMON" = true ]; then
    echo "Starting pm-operator in background..."
    nohup "${JAVA_CMD[@]}" < /dev/null > "$LOG_FILE" 2>&1 &
    PID=$!
    echo "$PID" > "$PID_FILE"
    echo "pm-operator started in background (PID: $PID)."
    echo "Logs: $LOG_FILE"
    echo "To stop: $SCRIPT_DIR/stop.sh"
else
    # Run in foreground and record PID so stop.sh can track it
    echo "$$" > "$PID_FILE"
    trap 'rm -f "$PID_FILE"' EXIT INT TERM
    exec "${JAVA_CMD[@]}"
fi
