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
    echo "Building pm-mock-runtime (full recompile of pm-mock-runtime and pm-api)..."
    TMP_CP="$CP_FILE.tmp.$$"
    trap 'rm -f "$TMP_CP"' EXIT INT TERM
    # Discard previously compiled classes instead of trusting them: the IDE's incremental
    # compiler can overwrite target/classes with bytecode whose Lombok @Value constructors miss
    # the Object super call, which fails at runtime with "Constructor must call super()".
    rm -rf "$CLASSES_DIR" "$REPO_ROOT/pm-api/target/classes"
    (cd "$REPO_ROOT" && ./mvnw compile dependency:build-classpath -Dmdep.outputFile="$TMP_CP" -pl pm-mock-runtime -am)
    mv "$TMP_CP" "$CP_FILE"
    touch "$BUILD_STAMP"
    trap - EXIT INT TERM
fi

# 3. Dynamic scenario selection
HAS_SCENARIO=false
HAS_PORT=false
HAS_BASE=false
HAS_DEVICE_SECRET=false
REQUESTED_PORT=7420
PORT_ASSIGNED=false
FOREGROUND=false
PASSED_ARGS=()

while [ $# -gt 0 ]; do
    case "$1" in
        -s|--scenario)
            HAS_SCENARIO=true
            PASSED_ARGS+=("$1" "$2")
            shift 2
            ;;
        -b|--base)
            if [ $# -lt 2 ]; then
                echo "Error: $1 requires a directory." >&2
                exit 1
            fi
            HAS_BASE=true
            PASSED_ARGS+=("$1" "$2")
            shift 2
            ;;
        -d|--device-secret)
            if [ $# -lt 2 ] || [ -z "$2" ]; then
                echo "Error: $1 requires a non-empty secret." >&2
                exit 1
            fi
            HAS_DEVICE_SECRET=true
            PASSED_ARGS+=("$1" "$2")
            shift 2
            ;;
        --device-secret=*)
            if [ -z "${1#--device-secret=}" ]; then
                echo "Error: --device-secret requires a secret." >&2
                exit 1
            fi
            HAS_DEVICE_SECRET=true
            PASSED_ARGS+=(--device-secret "${1#--device-secret=}")
            shift
            ;;
        --scenario=*)
            HAS_SCENARIO=true
            PASSED_ARGS+=("$1")
            shift
            ;;
        -p|--web-port)
            if [ $# -lt 2 ]; then
                echo "Error: $1 requires a port." >&2
                exit 1
            fi
            HAS_PORT=true
            PORT_ASSIGNED=true
            REQUESTED_PORT="$2"
            shift 2
            ;;
        --web-port=*)
            HAS_PORT=true
            PORT_ASSIGNED=true
            REQUESTED_PORT="${1#--web-port=}"
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

if [ "$HAS_PORT" = false ] && [ -n "${PM_MOCK_WEB_PORT:-}" ]; then
    PORT_ASSIGNED=true
    REQUESTED_PORT="$PM_MOCK_WEB_PORT"
fi

FINAL_ARGS=()

if [ "$HAS_SCENARIO" = false ]; then
    # The canonical scenarios of the consolidated catalog come from the same resource the Java
    # catalog reads (StandardScenarios.PRIMARY_SCENARIOS), so the menu cannot drift from it. The
    # fine-grained case files still exist during migration but are deliberately not offered here;
    # -s <name> reaches any of them.
    PRIMARY_FILE="$SCENARIOS_DIR/primary.txt"
    scenarios=()
    if [ -f "$PRIMARY_FILE" ]; then
        while IFS= read -r line; do
            case "$line" in
                ''|'#'*) continue ;;
            esac
            scenarios+=("$line")
        done < "$PRIMARY_FILE"
    fi
    if [ ${#scenarios[@]} -eq 0 ]; then
        echo "Error: no primary scenarios in $PRIMARY_FILE" >&2
        exit 1
    fi

    # Reads the one-line description from a scenario file, without requiring jq or python.
    scenario_description() {
        sed -n 's/^[[:space:]]*"description"[[:space:]]*:[[:space:]]*"\(.*\)",\{0,1\}[[:space:]]*$/\1/p' "$1" \
            | head -n1
    }

    echo ""
    echo "No scenario specified. Available scenarios:"
    default_idx=1
    for i in "${!scenarios[@]}"; do
        idx=$((i + 1))
        name="${scenarios[$i]}"
        description=""
        if [ -f "$SCENARIOS_DIR/$name.json" ]; then
            description="$(scenario_description "$SCENARIOS_DIR/$name.json")"
        fi
        echo "  $idx) $name"
        if [ -n "$description" ]; then
            echo "     $description"
        fi
        if [ "$name" = "running-work" ]; then
            default_idx=$idx
        fi
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

port_pids() {
    lsof -ti ":$1" 2>/dev/null || true
}

WEB_PORT="$REQUESTED_PORT"
if [ -z "$WEB_PORT" ]; then
    echo "Error: empty web port." >&2
    exit 1
fi

if [[ "$WEB_PORT" =~ ^[0-9]+$ ]] && [ "$WEB_PORT" -gt 0 ]; then
    PIDS="$(port_pids "$WEB_PORT")"
    if [ -n "$PIDS" ]; then
        if [ "$PORT_ASSIGNED" = true ]; then
            echo "Error: assigned port $WEB_PORT is in use by process (PID: $PIDS)." >&2
            exit 1
        fi
        REQUESTED="$WEB_PORT"
        WEB_PORT=""
        for ((candidate = REQUESTED + 1; candidate <= REQUESTED + 100; candidate++)); do
            if [ -z "$(port_pids "$candidate")" ]; then
                WEB_PORT="$candidate"
                break
            fi
        done
        if [ -z "$WEB_PORT" ]; then
            echo "Error: port $REQUESTED is in use and no free port was found." >&2
            exit 1
        fi
        echo "Port $REQUESTED is in use (PID: $PIDS); falling back to free port $WEB_PORT."
    fi
fi

FINAL_ARGS+=(--web-port "$WEB_PORT")

# A stable base by default, so the scenario state survives a restart. The base carries the
# scenario state, the runtime token, and the socket only; the web device secret is machine-wide
# (see `scripts/mock-device-secret.sh`), so deleting this base does not unpair a browser.
# `rm -rf "$SCRIPT_DIR/target/machine"` is the explicit "fresh scenario state" action; an
# operator-supplied --base keeps its own directory.
if [ "$HAS_BASE" = false ]; then
    STABLE_BASE="$SCRIPT_DIR/target/machine"
    # A Unix socket path is limited (104 bytes on macOS and Linux). A deep worktree checkout can
    # exceed that with the module's own target directory, so fall back to a short, stable path
    # under the temp directory; the CLI prints the base it uses.
    if [ "${#STABLE_BASE}" -ge 80 ]; then
        STABLE_BASE="${TMPDIR:-/tmp}/pm-mock-$(printf '%s' "$REPO_ROOT" | cksum | awk '{print $1}')"
    fi
    mkdir -p "$STABLE_BASE"
    FINAL_ARGS+=(--base "$STABLE_BASE")
fi

# One machine-wide web device secret, so `mvn clean`, a second worktree, or a temp cleanup does
# not change the secret under an open browser. An explicit -d/--device-secret still wins; it is
# already in PASSED_ARGS and is appended after this block.
if [ "$HAS_DEVICE_SECRET" = false ]; then
    # shellcheck source=scripts/mock-device-secret.sh
    source "$SCRIPT_DIR/scripts/mock-device-secret.sh"
    DEVICE_SECRET="$(ensure_mock_device_secret)"
    FINAL_ARGS+=(--device-secret "$DEVICE_SECRET")
fi

FINAL_ARGS+=(${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"})

# 4. Launch runtime
CP="$(cat "$CP_FILE"):$CLASSES_DIR:$REPO_ROOT/pm-api/target/classes"

if [ "$FOREGROUND" = true ]; then
    echo "Starting pm-mock-runtime in foreground..."
    exec java -cp "$CP" de.planmarshall.mock.cli.MockRuntimeCli ${FINAL_ARGS[@]+"${FINAL_ARGS[@]}"}
else
    echo "Starting pm-mock-runtime in background..."
    nohup java -cp "$CP" de.planmarshall.mock.cli.MockRuntimeCli ${FINAL_ARGS[@]+"${FINAL_ARGS[@]}"} < /dev/null > "$LOG_FILE" 2>&1 &
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
    grep -E "Base \(PM_MCP_BASE\)|Unix Socket|Loopback Web Port|Scenario|Initial State|Pairing Code|Web App URL" "$LOG_FILE" || true
    echo ""
    echo "Logs: $LOG_FILE"
    echo "To stop: $SCRIPT_DIR/stop.sh"
fi
