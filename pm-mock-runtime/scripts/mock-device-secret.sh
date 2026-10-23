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
# The caller receives the value on stdout only. Creation runs under an exclusive `mkdir` lock, so
# two simultaneous starts cannot each publish a different secret: the first creates it, and the
# other reads the file once it is complete (or takes the lock after it has gone and finds the file
# already in place). The value is written to a same-directory temporary file with mode 0600 and
# moved into place atomically, so a reader never sees a partial secret.
ensure_mock_device_secret() {
    local secret_file
    secret_file="$(mock_secret_file)"
    local secret_dir
    secret_dir="$(dirname "$secret_file")"
    mkdir -p "$secret_dir"

    local lock_dir="$secret_file.lock"
    local lock_held=false
    local attempts=0
    local recovered=false
    while [ "$lock_held" = false ]; do
        if mkdir "$lock_dir" 2>/dev/null; then
            lock_held=true
            break
        fi
        # A complete secret published by another start is authoritative; reading needs no lock.
        # The lock serializes only the absent/empty regeneration.
        if [ -s "$secret_file" ]; then
            break
        fi
        attempts=$((attempts + 1))
        if [ "$attempts" -ge 100 ]; then
            if [ "$recovered" = true ]; then
                echo "Error: timed out waiting for the device-secret lock at $lock_dir" >&2
                return 1
            fi
            # The critical section is a few file operations; a lock this old has no live holder.
            rmdir "$lock_dir" 2>/dev/null || true
            recovered=true
            attempts=0
            continue
        fi
        sleep 0.1
    done

    if [ "$lock_held" = true ]; then
        if [ ! -s "$secret_file" ]; then
            local tmp_file="$secret_file.tmp.$$"
            if ! (umask 077 && new_mock_device_secret >"$tmp_file") || ! chmod 600 "$tmp_file"; then
                rm -f "$tmp_file"
                rmdir "$lock_dir" 2>/dev/null || true
                echo "Error: cannot write the device secret at $secret_file" >&2
                return 1
            fi
            if ! mv "$tmp_file" "$secret_file"; then
                rm -f "$tmp_file"
                rmdir "$lock_dir" 2>/dev/null || true
                echo "Error: cannot publish the device secret at $secret_file" >&2
                return 1
            fi
        fi
        rmdir "$lock_dir" 2>/dev/null || true
    fi

    # An operator-created file keeps the same guarantee: the secret is never group/world readable.
    if ! chmod 600 "$secret_file" 2>/dev/null; then
        echo "Error: cannot restrict the device secret at $secret_file to mode 0600" >&2
        return 1
    fi

    cat "$secret_file"
}
