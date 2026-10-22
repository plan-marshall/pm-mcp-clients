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
# The loopback/name/IP/no-code matrix of the development coordinates. Sources the renderer and
# stubs the two address resolvers, so no real network is read.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=connection-urls.sh
source "$SCRIPT_DIR/connection-urls.sh"

STUB_HOST=""
STUB_IP=""
OUTPUT=""
CURRENT_CASE=""
FAILURES=0

# The resolvers the renderer calls, stubbed per case.
reachable_host() {
    printf '%s' "$STUB_HOST"
}

primary_ipv4() {
    printf '%s' "$STUB_IP"
}

fail() {
    printf 'FAIL [%s]: %s\n' "$CURRENT_CASE" "$1" >&2
    FAILURES=$((FAILURES + 1))
}

assert_contains() {
    case "$OUTPUT" in
        *"$1"*) ;;
        *) fail "expected to contain: $1" ;;
    esac
}

assert_not_contains() {
    case "$OUTPUT" in
        *"$1"*) fail "expected not to contain: $1" ;;
        *) ;;
    esac
}

assert_count() {
    local needle="$1"
    local expected="$2"
    local actual
    actual="$(printf '%s\n' "$OUTPUT" | grep -cF -- "$needle" || true)"
    if [ "$actual" -ne "$expected" ]; then
        fail "expected $expected line(s) with '$needle', found $actual"
    fi
}

# $1 case name, $2 resolved host, $3 primary IPv4, $4 pairing code
run_case() {
    CURRENT_CASE="$1"
    STUB_HOST="$2"
    STUB_IP="$3"
    OUTPUT="$(render_connection_urls 5173 "$4")"
}

# 1. A loopback override prints only local coordinates, even when an IPv4 exists.
run_case 'loopback 127.0.0.1' '127.0.0.1' '192.168.1.20' 'KXQB-MRTN'
assert_contains 'Local:       http://localhost:5173'
assert_not_contains 'Network'
assert_contains 'Paired:      http://127.0.0.1:5173/pair#code=KXQB-MRTN'

# 2. The whole 127.0.0.0/8 block is loopback, not only 127.0.0.1.
run_case 'loopback 127.0.0.2' '127.0.0.2' '192.168.1.20' 'KXQB-MRTN'
assert_not_contains 'Network'
assert_contains 'Paired:      http://127.0.0.2:5173/pair#code=KXQB-MRTN'

# 3. The localhost name is loopback too.
run_case 'loopback localhost' 'localhost' '192.168.1.20' 'KXQB-MRTN'
assert_contains 'Local:       http://localhost:5173'
assert_not_contains 'Network'
assert_contains 'Paired:      http://localhost:5173/pair#code=KXQB-MRTN'

# 4. The IPv6 loopback spellings are loopback as well, and the paired URL brackets the host.
run_case 'loopback ::1' '::1' '192.168.1.20' 'KXQB-MRTN'
assert_not_contains 'Network'
assert_contains 'Paired:      http://[::1]:5173/pair#code=KXQB-MRTN'

run_case 'loopback [::1]' '[::1]' '192.168.1.20' 'KXQB-MRTN'
assert_not_contains 'Network'
assert_contains 'Paired:      http://[::1]:5173/pair#code=KXQB-MRTN'

# 5. A real name prints the name, the differing IP, and the paired URL on the name.
run_case 'name and IP' 'Mac-Mini.local' '192.168.1.20' 'KXQB-MRTN'
assert_contains 'Network:     http://Mac-Mini.local:5173'
assert_contains 'Network (IP): http://192.168.1.20:5173'
assert_contains 'Paired:      http://Mac-Mini.local:5173/pair#code=KXQB-MRTN'

# 6. A non-loopback IPv6 host is bracketed in every URL.
run_case 'IPv6 network host' 'fe80::1234' '' 'KXQB-MRTN'
assert_contains 'Network:     http://[fe80::1234]:5173'
assert_contains 'Paired:      http://[fe80::1234]:5173/pair#code=KXQB-MRTN'

# 7. A host equal to the IP prints one network line, with no duplicate IP line.
run_case 'host equals IP' '192.168.1.20' '192.168.1.20' 'KXQB-MRTN'
assert_contains 'Network:     http://192.168.1.20:5173'
assert_count 'Network:' 1
assert_not_contains 'Network (IP)'

# 8. No pairing code prints no Paired line.
run_case 'no pairing code' 'Mac-Mini.local' '192.168.1.20' ''
assert_contains 'Network:     http://Mac-Mini.local:5173'
assert_not_contains 'Paired'

# 9. No IPv4 prints the name without an IP line.
run_case 'no IPv4' 'Mac-Mini.local' '' 'KXQB-MRTN'
assert_contains 'Network:     http://Mac-Mini.local:5173'
assert_not_contains 'Network (IP)'
assert_contains 'Paired:      http://Mac-Mini.local:5173/pair#code=KXQB-MRTN'

if [ "$FAILURES" -ne 0 ]; then
    printf 'connection-urls: %s case(s) failed\n' "$FAILURES" >&2
    exit 1
fi

printf 'connection-urls: all cases passed\n'
