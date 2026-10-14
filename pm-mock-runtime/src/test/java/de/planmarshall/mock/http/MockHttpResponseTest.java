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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MockHttpResponse")
class MockHttpResponseTest {

    @Test
    @DisplayName("writes simple text response with status and custom headers")
    void sendTextResponse() throws Exception {
        var out = new ByteArrayOutputStream();
        var response = new MockHttpResponse(out);

        assertFalse(response.isCommitted());
        response.header("X-Custom", "test-val");
        response.send(200, "text/plain", "hello world");
        assertTrue(response.isCommitted());

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(raw.contains("Content-Type: text/plain\r\n"));
        assertTrue(raw.contains("Content-Length: 11\r\n"));
        assertTrue(raw.contains("X-Custom: test-val\r\n"));
        assertTrue(raw.endsWith("\r\n\r\nhello world"));
    }

    @Test
    @DisplayName("sends null body as empty bytes")
    void sendNullBody() throws Exception {
        var out = new ByteArrayOutputStream();
        var response = new MockHttpResponse(out);

        response.send(204, null, (String) null);
        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.startsWith("HTTP/1.1 204 No Content\r\n"));
        assertTrue(raw.contains("Content-Length: 0\r\n"));
        assertFalse(raw.contains("Content-Type:"));
    }

    @Test
    @DisplayName("writes JSON tree response")
    void sendJsonTree() throws Exception {
        var out = new ByteArrayOutputStream();
        var response = new MockHttpResponse(out);

        response.json(201, Map.of("created", true));
        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.startsWith("HTTP/1.1 201 Created\r\n"));
        assertTrue(raw.contains("Content-Type: application/json\r\n"));
        assertTrue(raw.contains("{\"created\":true}"));
    }

    @Test
    @DisplayName("throws IllegalStateException on double commit")
    void doubleCommitRejected() throws Exception {
        var out = new ByteArrayOutputStream();
        var response = new MockHttpResponse(out);

        response.send(200, "text/plain", "first");
        assertThrows(IllegalStateException.class, () -> response.send(200, "text/plain", "second"));
        assertThrows(IllegalStateException.class, () -> response.events(200));
    }

    @Test
    @DisplayName("starts SSE event stream with chunked encoding headers")
    void startEvents() throws Exception {
        var out = new ByteArrayOutputStream();
        var response = new MockHttpResponse(out);

        try (var stream = response.events(200)) {
            assertNotNull(stream);
            assertTrue(response.isCommitted());
            var raw = out.toString(StandardCharsets.UTF_8);
            assertTrue(raw.startsWith("HTTP/1.1 200 OK\r\n"));
            assertTrue(raw.contains("Content-Type: text/event-stream\r\n"));
            assertTrue(raw.contains("Transfer-Encoding: chunked\r\n"));
        }
    }

    @Test
    @DisplayName("maps standard and fallback status codes to reason phrases")
    void statusMessages() throws Exception {
        int[] statuses = {200, 201, 204, 400, 401, 403, 404, 409, 500, 418};
        for (var status : statuses) {
            var out = new ByteArrayOutputStream();
            var response = new MockHttpResponse(out);
            response.send(status, null, "");
            var raw = out.toString(StandardCharsets.UTF_8);
            assertTrue(raw.contains("HTTP/1.1 " + status + " "));
        }
    }
}
