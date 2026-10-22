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
# The coordinates a browser can use to reach the development server. Sourced by `start.sh`, so
# the renderer and its address resolution are testable without starting Vite.

# The primary IPv4 address of this machine, or empty when none is reachable.
primary_ipv4() {
    local ip=""
    case "$(uname -s)" in
        Darwin)
            ip="$(ipconfig getifaddr en0 2>/dev/null || true)"
            if [ -z "$ip" ]; then
                ip="$(ipconfig getifaddr en1 2>/dev/null || true)"
            fi
            ;;
        *)
            ip="$(hostname -I 2>/dev/null | awk '{print $1}' || true)"
            ;;
    esac
    printf '%s' "$ip"
}

# The reachable name of this machine: the name is preferred over the address, because it
# survives DHCP changes and resolves through mDNS/Bonjour. The IP is the fallback.
reachable_host() {
    if [ -n "${PM_WEB_HOST:-}" ]; then
        printf '%s' "$PM_WEB_HOST"
        return 0
    fi

    local local_name=""
    local_name="$(scutil --get LocalHostName 2>/dev/null || true)"
    if [ -n "$local_name" ]; then
        printf '%s.local' "$local_name"
        return 0
    fi

    local fqdn=""
    fqdn="$(hostname -f 2>/dev/null || true)"
    if [ -n "$fqdn" ] && [ "$fqdn" != "localhost" ]; then
        printf '%s' "$fqdn"
        return 0
    fi

    local short=""
    short="$(hostname 2>/dev/null || true)"
    if [ -n "$short" ] && [ "$short" != "localhost" ]; then
        printf '%s' "$short"
        return 0
    fi

    local ip
    ip="$(primary_ipv4)"
    if [ -n "$ip" ]; then
        printf '%s' "$ip"
        return 0
    fi

    printf 'localhost'
}

# Whether a host reaches the machine without the network (so no phone can use it).
is_loopback_host() {
    case "$1" in
        localhost|::1|'[::1]') return 0 ;;
    esac
    # 127.0.0.0/8 is the IPv4 loopback block, not only 127.0.0.1.
    [[ "$1" =~ ^127\.[0-9]+\.[0-9]+\.[0-9]+$ ]]
}

# A host as the authority of a URL: a bare IPv6 address needs its brackets.
#
# $1 the resolved host
url_host() {
    case "$1" in
        *:*)
            case "$1" in
                \[*\]) printf '%s' "$1" ;;
                *) printf '[%s]' "$1" ;;
            esac
            ;;
        *) printf '%s' "$1" ;;
    esac
}

# Prints the coordinates a phone can use: the local URL always, the machine's name and the IP
# only for a reachable host, and the paired URL on the resolved host.
#
# A loopback host is the operator's explicit no-exposure choice: it prints no network
# coordinate, even when a primary IPv4 exists, and the paired URL never swaps in that IP.
#
# $1 the web port
# $2 the pairing code, or empty when none is available
render_connection_urls() {
    local web_port="$1"
    local pairing_code="${2:-}"
    local host
    host="$(reachable_host)"
    local authority
    authority="$(url_host "$host")"
    local ip
    ip="$(primary_ipv4)"

    printf 'Local:       http://localhost:%s\n' "$web_port"
    if ! is_loopback_host "$host"; then
        printf 'Network:     http://%s:%s\n' "$authority" "$web_port"
        if [ -n "$ip" ] && [ "$ip" != "$host" ]; then
            printf 'Network (IP): http://%s:%s\n' "$ip" "$web_port"
        fi
    fi
    if [ -n "$pairing_code" ]; then
        printf 'Paired:      http://%s:%s/pair#code=%s\n' "$authority" "$web_port" "$pairing_code"
    fi
}
