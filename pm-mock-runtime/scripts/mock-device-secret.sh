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
# The one machine-wide web device secret of the development loop, sourced by `start.sh`.
#
# The secret lives outside the checkout on purpose: a base under `target/` or a short temp path
# derived from the checkout disappears on `mvn clean`, is different in a second worktree, and
# changes when the temp directory is cleaned. A paired browser would then receive `401` and lose
# its session. One file under the user's home survives all three.
#
# The value is printed on stdout for the caller only; this helper never logs it.

# The file that holds the machine-wide secret; `PM_MOCK_DEVICE_SECRET_FILE` overrides it.
mock_secret_file() {
    printf '%s' "${PM_MOCK_DEVICE_SECRET_FILE:-$HOME/.plan-marshall-mcp/mock-web-device-secret}"
}

# A fresh development secret. This is a local dev credential, not a production key; 24 random
# bytes are far beyond what a loopback mock needs and keep the value non-guessable.
new_mock_device_secret() {
    printf 'pmw_mock_secret_%s' "$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')"
}

# Prints the machine-wide device secret, creating it on first use.
#
# The caller receives the value on stdout only. A same-directory temporary file published by an
# atomic move means a concurrent reader sees either no file or a complete one, never a partial
# secret. After the move the file itself is read back, so two simultaneous starts agree on the
# secret that won instead of each using the value it generated.
ensure_mock_device_secret() {
    local secret_file
    secret_file="$(mock_secret_file)"

    if [ ! -s "$secret_file" ]; then
        local secret_dir
        secret_dir="$(dirname "$secret_file")"
        mkdir -p "$secret_dir"
        local tmp_file="$secret_file.tmp.$$"
        if ! (umask 077 && new_mock_device_secret >"$tmp_file") || ! chmod 600 "$tmp_file"; then
            rm -f "$tmp_file"
            echo "Error: cannot write the device secret at $secret_file" >&2
            return 1
        fi
        if ! mv "$tmp_file" "$secret_file"; then
            rm -f "$tmp_file"
            echo "Error: cannot publish the device secret at $secret_file" >&2
            return 1
        fi
    fi

    # An operator-created file keeps the same guarantee: the secret is never group/world readable.
    if ! chmod 600 "$secret_file" 2>/dev/null; then
        echo "Error: cannot restrict the device secret at $secret_file to mode 0600" >&2
        return 1
    fi

    cat "$secret_file"
}
