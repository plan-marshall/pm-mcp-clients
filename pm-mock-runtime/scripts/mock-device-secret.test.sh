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
# The machine-wide device secret: created once outside the checkout, reused by every later start,
# regenerated when the file is absent or empty, and never logged.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=mock-device-secret.sh
source "$SCRIPT_DIR/mock-device-secret.sh"

TEST_DIR="$(mktemp -d "${TMPDIR:-/tmp}/mock-device-secret-test.XXXXXX")"
trap 'rm -rf "$TEST_DIR"' EXIT

FAILURES=0
CURRENT_CASE=""

fail() {
    printf 'FAIL [%s]: %s\n' "$CURRENT_CASE" "$1" >&2
    FAILURES=$((FAILURES + 1))
}

assert_equal() {
    if [ "$1" != "$2" ]; then
        fail "expected '$2', got '$1'"
    fi
}

assert_not_equal() {
    if [ "$1" = "$2" ]; then
        fail "expected a different value, got '$1'"
    fi
}

assert_non_empty() {
    if [ -z "$1" ]; then
        fail 'expected a non-empty value'
    fi
}

assert_prefix() {
    case "$1" in
        pmw_mock_secret_*) ;;
        *) fail "expected the pmw_mock_secret_ prefix, got '$1'" ;;
    esac
}

# The file mode, portable across BSD and GNU stat.
file_mode() {
    if stat -f '%Lp' "$1" >/dev/null 2>&1; then
        stat -f '%Lp' "$1"
    else
        stat -c '%a' "$1"
    fi
}

CURRENT_CASE='the default path is under the user home'
EXPECTED_DEFAULT="$HOME/.plan-marshall-mcp/mock-web-device-secret"
assert_equal "$(unset PM_MOCK_DEVICE_SECRET_FILE; mock_secret_file)" "$EXPECTED_DEFAULT"

CURRENT_CASE='the override replaces the global path'
SECRET_FILE="$TEST_DIR/state/mock-web-device-secret"
assert_equal "$(PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" mock_secret_file)" "$SECRET_FILE"

CURRENT_CASE='the first call creates the file and its directory'
FIRST="$(PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret)"
assert_non_empty "$FIRST"
assert_prefix "$FIRST"
if [ ! -f "$SECRET_FILE" ]; then
    fail "expected the secret file at $SECRET_FILE"
fi
assert_equal "$(cat "$SECRET_FILE")" "$FIRST"

CURRENT_CASE='the file is readable only by its owner'
assert_equal "$(file_mode "$SECRET_FILE")" '600'

CURRENT_CASE='the helper prints nothing but the secret on stdout'
STDERR_FILE="$TEST_DIR/stderr"
SECOND="$(PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret 2>"$STDERR_FILE")"
assert_equal "$SECOND" "$FIRST"
assert_equal "$(cat "$STDERR_FILE")" ''

CURRENT_CASE='a later call in the same state reuses the secret'
THIRD="$(PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret)"
assert_equal "$THIRD" "$FIRST"

CURRENT_CASE='an empty file is regenerated'
: >"$SECRET_FILE"
FOURTH="$(PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret)"
assert_non_empty "$FOURTH"
assert_prefix "$FOURTH"
assert_not_equal "$FOURTH" "$FIRST"
assert_equal "$(cat "$SECRET_FILE")" "$FOURTH"
assert_equal "$(file_mode "$SECRET_FILE")" '600'

CURRENT_CASE='an existing world-readable file is tightened to 0600'
chmod 644 "$SECRET_FILE"
FIFTH="$(PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret)"
assert_equal "$FIFTH" "$FOURTH"
assert_equal "$(file_mode "$SECRET_FILE")" '600'

CURRENT_CASE='concurrent calls agree on one secret'
rm -f "$SECRET_FILE"
CONCURRENT_A="$TEST_DIR/concurrent-a"
CONCURRENT_B="$TEST_DIR/concurrent-b"
PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret >"$CONCURRENT_A" &
CONCURRENT_PID_A=$!
PM_MOCK_DEVICE_SECRET_FILE="$SECRET_FILE" ensure_mock_device_secret >"$CONCURRENT_B" &
CONCURRENT_PID_B=$!
wait "$CONCURRENT_PID_A"
wait "$CONCURRENT_PID_B"
assert_non_empty "$(cat "$CONCURRENT_A")"
assert_equal "$(cat "$CONCURRENT_A")" "$(cat "$CONCURRENT_B")"
assert_equal "$(cat "$CONCURRENT_A")" "$(cat "$SECRET_FILE")"
if [ -d "$SECRET_FILE.lock" ]; then
    fail 'the device-secret lock directory was left behind'
fi

if [ "$FAILURES" -ne 0 ]; then
    printf 'mock-device-secret: %s case(s) failed\n' "$FAILURES" >&2
    exit 1
fi

printf 'mock-device-secret: all cases passed\n'
