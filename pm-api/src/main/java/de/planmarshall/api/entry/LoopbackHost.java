/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The {@code Host} of a request on the loopback listener
 * (03-web-server.adoc#loopback-host): {@code 127.0.0.1}, {@code localhost}, or {@code [::1]}, with
 * any port or none. The check exists against foreign host names, so the port adds nothing to it,
 * while an SSH forward to another local port sends that port. An IPv6 literal with a zone, an
 * IPv4-mapped address, and a malformed host are refused.
 *
 * @param host the normalized loopback host, lower case and without a port or brackets
 */
public record LoopbackHost(String host) {

    /** The bare IPv4 loopback address. */
    public static final String IPV4 = "127.0.0.1";

    /** The loopback name. */
    public static final String LOCALHOST = "localhost";

    /** The bare IPv6 loopback address. */
    public static final String IPV6 = "::1";

    public LoopbackHost {
        Objects.requireNonNull(host, "host");
    }

    /**
     * Parses a {@code Host} header value of the loopback listener.
     *
     * @param hostHeader the header value, may be {@code null}
     * @return the loopback host when the value names the loopback listener's own origin, otherwise
     *         an empty optional
     */
    public static Optional<LoopbackHost> parse(@Nullable String hostHeader) {
        if (hostHeader == null) {
            return Optional.empty();
        }
        var value = hostHeader.strip();
        if (value.isEmpty() || value.indexOf('%') >= 0) {
            return Optional.empty();
        }
        String candidate;
        if (value.startsWith("[")) {
            candidate = stripBracketedPort(value);
        } else {
            candidate = stripPort(value);
        }
        if (candidate == null) {
            return Optional.empty();
        }
        var normalized = candidate.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case IPV4, LOCALHOST, "[" + IPV6 + "]" -> Optional.of(new LoopbackHost(normalized));
            default -> Optional.empty();
        };
    }

    /**
     * @param hostHeader the header value
     * @return {@code true} when the value names the loopback listener's own origin
     */
    public static boolean isLoopback(@Nullable String hostHeader) {
        return parse(hostHeader).isPresent();
    }

    private static @Nullable String stripBracketedPort(String value) {
        var closing = value.indexOf(']');
        if (closing < 0) {
            return null;
        }
        var address = value.substring(0, closing + 1);
        var rest = value.substring(closing + 1);
        if (rest.isEmpty()) {
            return address;
        }
        if (rest.charAt(0) != ':') {
            return null;
        }
        var port = rest.substring(1);
        return !port.isEmpty() && port.chars().allMatch(Character::isDigit) ? address : null;
    }

    private static @Nullable String stripPort(String value) {
        var separator = value.lastIndexOf(':');
        if (separator < 0) {
            return value;
        }
        var port = value.substring(separator + 1);
        if (port.isEmpty() || !port.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return value.substring(0, separator);
    }
}
