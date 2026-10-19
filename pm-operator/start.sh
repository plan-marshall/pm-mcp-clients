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

# The operator resolves <PM_MCP_HOME> from its executable. Launched from the Maven classpath that
# executable is `java`, which is not one of the release binaries, so the layout resolution refuses
# the process. A development home keeps that resolution working; it is only used to locate a
# sibling runtime, and a development run either has one running already or starts it separately.
DEV_HOME=false
if [ -z "${PM_MCP_HOME:-}" ]; then
    PM_MCP_HOME="$REPO_ROOT/target/pm-operator-dev-home"
    mkdir -p "$PM_MCP_HOME/bin"
    export PM_MCP_HOME
    DEV_HOME=true
fi

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

# 2. Smart build check
CLASSES_DIR="$SCRIPT_DIR/target/classes"
CP_FILE="$SCRIPT_DIR/target/classpath.txt"
BUILD_STAMP="$SCRIPT_DIR/target/.build-stamp"
BUILD_NEEDED=false

# Newest modification time under the given paths, portable across BSD and GNU stat.
newest_mtime() {
    if stat -f "%m" "$1" >/dev/null 2>&1; then
        find "$@" -type f -exec stat -f "%m" {} + 2>/dev/null
    else
        find "$@" -type f -exec stat -c "%Y" {} + 2>/dev/null
    fi | sort -nr | head -n1 || true
}

if [ ! -d "$CLASSES_DIR" ] || [ ! -f "$CP_FILE" ] || [ ! -f "$BUILD_STAMP" ]; then
    BUILD_NEEDED=true
else
    # Only main sources: `compile` does not build tests, and editing a test must not force a
    # rebuild of the operator before every verb.
    NEWEST_SRC="$(newest_mtime "$SCRIPT_DIR/src/main" "$REPO_ROOT/pm-api/src/main" \
        "$SCRIPT_DIR/pom.xml" "$REPO_ROOT/pm-api/pom.xml" "$REPO_ROOT/pom.xml")"
    STAMP_MTIME="$(stat -f "%m" "$BUILD_STAMP" 2>/dev/null || stat -c "%Y" "$BUILD_STAMP" 2>/dev/null || echo 0)"
    if [ "${NEWEST_SRC:-0}" -gt "${STAMP_MTIME:-0}" ]; then
        BUILD_NEEDED=true
    fi
    # A class file newer than the stamp was written after the build (an IDE compiler), so the
    # tree is not the one that was verified; compile it again.
    NEWEST_CLASS="$(newest_mtime "$CLASSES_DIR" "$REPO_ROOT/pm-api/target/classes")"
    if [ "${NEWEST_CLASS:-0}" -gt "${STAMP_MTIME:-0}" ]; then
        BUILD_NEEDED=true
    fi
fi

if [ "$BUILD_NEEDED" = true ]; then
    echo "Building pm-operator (full recompile of pm-operator and pm-api)..."
    TMP_CP="$CP_FILE.tmp.$$"
    trap 'rm -f "$TMP_CP"' EXIT INT TERM
    # Discard previously compiled classes instead of trusting them: the IDE's incremental
    # compiler can overwrite target/classes with bytecode whose Lombok @Value constructors miss
    # the Object super call, which fails at runtime with "Constructor must call super()".
    rm -rf "$CLASSES_DIR" "$REPO_ROOT/pm-api/target/classes"
    (cd "$REPO_ROOT" && ./mvnw compile dependency:build-classpath -Dmdep.outputFile="$TMP_CP" -pl pm-operator -am)
    mkdir -p "$SCRIPT_DIR/target"
    mv "$TMP_CP" "$CP_FILE"
    touch "$BUILD_STAMP"
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

# 5. A source checkout has only the mock runtime. The console starts a runtime on demand and
# needs an installed pm-mcpd, which this tree does not build; point at the development runtime
# instead of letting the operator fail on a missing binary.
RUNTIME_BASE="${PM_MCP_BASE:-$HOME/.plan-marshall-mcp}"
if [ "$DEV_HOME" = true ] && [ "${#PASSED_ARGS[@]}" -eq 0 ] \
        && [ ! -S "$RUNTIME_BASE/run/runtime.sock" ]; then
    echo "No runtime is available." >&2
    echo "pm-mcpd is the product's runtime and is not built by this repository. The development" >&2
    echo "runtime is pm-mock-runtime: start it first, then run this script again." >&2
    echo "  $REPO_ROOT/pm-mock-runtime/start.sh" >&2
    exit 1
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
