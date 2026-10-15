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
PID_FILE="$SCRIPT_DIR/target/pm-mock-runtime.pid"
LOG_FILE="$SCRIPT_DIR/target/pm-mock-runtime.log"
SCENARIOS_DIR="$SCRIPT_DIR/src/main/resources/scenarios"

# 1. Check if already running
if [ -f "$PID_FILE" ]; then
    EXISTING_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$EXISTING_PID" ] && kill -0 "$EXISTING_PID" 2>/dev/null; then
        echo "pm-mock-runtime is already running (PID: $EXISTING_PID)."
        echo "Logs: $LOG_FILE"
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
    # Check if any sources in pm-mock-runtime or pm-api are newer than target/classes
    NEWEST_SRC="$(find "$SCRIPT_DIR/src" "$REPO_ROOT/pm-api/src" -type f 2>/dev/null | xargs stat -f "%m" 2>/dev/null | sort -nr | head -n1 || echo 0)"
    TARGET_MTIME="$(stat -f "%m" "$CLASSES_DIR" 2>/dev/null || echo 0)"
    if [ "$NEWEST_SRC" -gt "$TARGET_MTIME" ]; then
        BUILD_NEEDED=true
    fi
fi

if [ "$BUILD_NEEDED" = true ]; then
    echo "Building pm-mock-runtime (incremental ./mvnw -am, without clean)..."
    "$REPO_ROOT/mvnw" compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -pl pm-mock-runtime -am
fi

# 3. Dynamic scenario selection
HAS_SCENARIO=false
HAS_PORT=false
FOREGROUND=false
PASSED_ARGS=()

while [ $# -gt 0 ]; do
    case "$1" in
        -s|--scenario)
            HAS_SCENARIO=true
            PASSED_ARGS+=("$1" "$2")
            shift 2
            ;;
        --scenario=*)
            HAS_SCENARIO=true
            PASSED_ARGS+=("$1")
            shift
            ;;
        -p|--web-port)
            HAS_PORT=true
            PASSED_ARGS+=("$1" "$2")
            shift 2
            ;;
        --web-port=*)
            HAS_PORT=true
            PASSED_ARGS+=("$1")
            shift
            ;;
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

FINAL_ARGS=()

if [ "$HAS_SCENARIO" = false ]; then
    # Discover available scenarios dynamically from source
    scenarios=()
    if [ -d "$SCENARIOS_DIR" ]; then
        for f in "$SCENARIOS_DIR"/*.json; do
            if [ -f "$f" ]; then
                bname="$(basename "$f" .json)"
                scenarios+=("$bname")
            fi
        done
    fi

    if [ ${#scenarios[@]} -gt 0 ]; then
        echo ""
        echo "No scenario specified. Available scenarios (loaded dynamically from source):"
        default_idx=1
        for i in "${!scenarios[@]}"; do
            idx=$((i + 1))
            name="${scenarios[$i]}"
            if [ "$name" = "onboarding-happy" ]; then
                default_idx=$idx
            fi
            echo "  $idx) $name"
        done

        if [ -t 0 ]; then
            echo -n "Select a scenario [1-${#scenarios[@]}] (default: $default_idx): "
            read -r choice || choice=""
            if [ -z "$choice" ]; then
                choice=$default_idx
            fi
            if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le "${#scenarios[@]}" ]; then
                selected_scenario="${scenarios[$((choice - 1))]}"
            else
                echo "Invalid selection '$choice', defaulting to '${scenarios[$((default_idx - 1))]}'."
                selected_scenario="${scenarios[$((default_idx - 1))]}"
            fi
        else
            selected_scenario="${scenarios[$((default_idx - 1))]}"
            echo "Non-interactive environment detected, defaulting to '$selected_scenario'."
        fi
        FINAL_ARGS+=(--scenario "$selected_scenario")
    fi
fi

if [ "$HAS_PORT" = false ]; then
    FINAL_ARGS+=(--web-port 7420)
fi

FINAL_ARGS+=("${PASSED_ARGS[@]}")

# 4. Launch runtime
CP="$(cat "$CP_FILE"):$CLASSES_DIR:$REPO_ROOT/pm-api/target/classes"

if [ "$FOREGROUND" = true ]; then
    echo "Starting pm-mock-runtime in foreground..."
    exec java -cp "$CP" de.planmarshall.mock.cli.MockRuntimeCli "${FINAL_ARGS[@]}"
else
    echo "Starting pm-mock-runtime in background..."
    nohup java -cp "$CP" de.planmarshall.mock.cli.MockRuntimeCli "${FINAL_ARGS[@]}" < /dev/null > "$LOG_FILE" 2>&1 &
    PID=$!
    echo "$PID" > "$PID_FILE"

    # Wait briefly to confirm successful start and display coordinates
    sleep 1
    if ! kill -0 "$PID" 2>/dev/null; then
        echo "Error: pm-mock-runtime failed to start. Log output:" >&2
        cat "$LOG_FILE" >&2
        rm -f "$PID_FILE"
        exit 1
    fi

    echo "Mock runtime started successfully (PID: $PID)."
    grep -E "Base \(PM_MCP_BASE\)|Unix Socket|Loopback Web Port|Scenario|Initial State" "$LOG_FILE" || true
    echo ""
    echo "Logs: $LOG_FILE"
    echo "To stop: $SCRIPT_DIR/stop.sh"
fi
