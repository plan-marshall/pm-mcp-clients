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

@DisplayName("MockHttpRequest")
class MockHttpRequestTest {

    @Test
    @DisplayName("normalizes method to uppercase and parses path and query")
    void normalizeAndParse() {
        var req = new MockHttpRequest("get", "/api/v1/status?param=value&foo=bar",
                Map.of("Content-Type", "application/json"), "hello".getBytes(StandardCharsets.UTF_8), 1000L);

        assertEquals("GET", req.method());
        assertEquals("/api/v1/status?param=value&foo=bar", req.target());
        assertEquals("/api/v1/status", req.path());
        assertEquals("param=value&foo=bar", req.query());
        assertEquals("application/json", req.header("content-type"));
        assertEquals("application/json", req.header("CONTENT-TYPE"));
        assertNull(req.header("unknown"));
        assertEquals("hello", req.bodyText());
        assertEquals(1000L, req.receivedNanos());
    }

    @Test
    @DisplayName("handles target without query parameters")
    void targetWithoutQuery() {
        var req = new MockHttpRequest("POST", "/api/v1/check", null, null, 2000L);

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
        var req1 = new MockHttpRequest("GET", "/test", Map.of("a", "b"), new byte[]{1, 2, 3}, 10L);
        var req2 = new MockHttpRequest("GET", "/test", Map.of("a", "b"), new byte[]{1, 2, 3}, 10L);
        var req3 = new MockHttpRequest("GET", "/test", Map.of("a", "b"), new byte[]{1, 2, 4}, 10L);
        var req4 = new MockHttpRequest("POST", "/test", Map.of("a", "b"), new byte[]{1, 2, 3}, 10L);

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
