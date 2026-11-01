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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("MockHttpRequest")
@EnableGeneratorController
class MockHttpRequestTest {

    @Test
    @DisplayName("normalizes method to uppercase and parses path and query")
    void normalizeAndParse() {
        var headerName = Generators.letterStrings(4, 10).next();
        var headerValue = Generators.letterStrings(4, 10).next();
        var body = Generators.letterStrings(4, 12).next();
        var receivedNanos = Generators.integers(1, 1_000_000).next().longValue();
        var req = new MockHttpRequest("get", "/api/v1/status?param=value&foo=bar",
                Map.of(headerName, headerValue), body.getBytes(StandardCharsets.UTF_8), receivedNanos);

        assertEquals("GET", req.method());
        assertEquals("/api/v1/status?param=value&foo=bar", req.target());
        assertEquals("/api/v1/status", req.path());
        assertEquals("param=value&foo=bar", req.query());
        assertEquals(headerValue, req.header(headerName.toLowerCase()));
        assertEquals(headerValue, req.header(headerName.toUpperCase()));
        assertNull(req.header(headerName + "-absent"));
        assertEquals(body, req.bodyText());
        assertEquals(receivedNanos, req.receivedNanos());
    }

    @Test
    @DisplayName("handles target without query parameters")
    void targetWithoutQuery() {
        var receivedNanos = Generators.integers(1, 1_000_000).next().longValue();
        var req = new MockHttpRequest("POST", "/api/v1/check", null, null, receivedNanos);

        assertEquals("POST", req.method());
        assertEquals("/api/v1/check", req.path());
        assertNull(req.query());
        assertEquals(0, req.body().length);
        assertEquals("", req.bodyText());
        assertTrue(req.headers().isEmpty());
    }

    @Test
    @DisplayName("implements equals, hashCode, and toString considering byte array contents")
    void equalsAndHashCode() {
        var headerName = Generators.letterStrings(4, 10).next();
        var headerValue = Generators.letterStrings(4, 10).next();
        var bytes = new byte[]{1, 2, 3};
        var headers = Map.of(headerName, headerValue);
        var receivedNanos = Generators.integers(1, 1_000_000).next().longValue();
        var req1 = new MockHttpRequest("GET", "/test", headers, bytes, receivedNanos);
        var req2 = new MockHttpRequest("GET", "/test", headers, bytes.clone(), receivedNanos);
        var req3 = new MockHttpRequest("GET", "/test", headers, new byte[]{1, 2, 4}, receivedNanos);
        var req4 = new MockHttpRequest("POST", "/test", headers, bytes.clone(), receivedNanos);

        assertEquals(req1, req1);
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());

        assertNotEquals(req1, req3);
        assertNotEquals(req1, req4);
        assertNotNull(req1);
        assertNotEquals("other", req1);

        assertTrue(req1.toString().contains("MockHttpRequest[GET /test"));
    }
}
