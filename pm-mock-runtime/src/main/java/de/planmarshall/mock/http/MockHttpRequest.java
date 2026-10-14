/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.http;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * An HTTP request received by the mock server.
 *
 * @param method        HTTP method (GET, POST, etc.)
 * @param target        the raw request target (e.g. {@code /api/v1/status?param=val})
 * @param headers       headers normalized to lower-case names
 * @param body          raw request body bytes
 * @param receivedNanos timestamp of receipt in nanoseconds
 */
public record MockHttpRequest(
        String method,
        String target,
        Map<String, String> headers,
        byte[] body,
        long receivedNanos) {

    /**
     * Compact constructor normalizing fields.
     */
    public MockHttpRequest {
        method = Objects.requireNonNull(method, "method").toUpperCase(Locale.ROOT);
        Objects.requireNonNull(target, "target");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? new byte[0] : body;
    }

    /**
     * @return the URI path without query parameters
     */
    public String path() {
        var q = target.indexOf('?');
        return q >= 0 ? target.substring(0, q) : target;
    }

    /**
     * @return the query string, or {@code null} if none
     */
    public String query() {
        var q = target.indexOf('?');
        return q >= 0 ? target.substring(q + 1) : null;
    }

    /**
     * @param name header name
     * @return header value, or {@code null}
     */
    public String header(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the body decoded as UTF-8 text
     */
    public String bodyText() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
