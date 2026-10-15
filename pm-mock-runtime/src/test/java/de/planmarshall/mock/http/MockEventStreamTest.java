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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MockEventStream")
class MockEventStreamTest {

    @Test
    @DisplayName("formats single-line and multi-line SSE messages with chunked encoding")
    void sendTextEvents() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var stream = new MockEventStream(out)) {
            assertFalse(stream.isClosed());
            stream.send("tick", "payload-1");
            stream.send(null, "unnamed-event");
            stream.send("multi", "line1\nline2");
        }

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("event: tick\ndata: payload-1\n\n"));
        assertTrue(raw.contains("data: unnamed-event\n\n"));
        assertTrue(raw.contains("event: multi\ndata: line1\ndata: line2\n\n"));
        assertTrue(raw.endsWith("0\r\n\r\n"));
    }

    @Test
    @DisplayName("sends JSON serializable payloads and raw strings")
    void sendJsonEvents() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var stream = new MockEventStream(out)) {
            stream.sendJson("status", Map.of("running", true));
            stream.sendJson("raw", "{\"custom\":123}");
        }

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("event: status\ndata: {\"running\":true}\n\n"));
        assertTrue(raw.contains("event: raw\ndata: {\"custom\":123}\n\n"));
    }

    @Test
    @DisplayName("sends SSE comments and ping heartbeats")
    void sendComments() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var stream = new MockEventStream(out)) {
            stream.sendComment("ping heartbeat");
        }

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains(": ping heartbeat\n\n"));
    }

    @Test
    @DisplayName("rejects sends after stream is closed")
    void rejectAfterClose() throws Exception {
        var out = new ByteArrayOutputStream();
        var stream = new MockEventStream(out);
        stream.close();
        assertTrue(stream.isClosed());

        assertThrows(IOException.class, () -> stream.send("tick", "data"));
        assertThrows(IOException.class, () -> stream.sendComment("ping"));

        // Idempotent close
        stream.close();
        assertTrue(stream.isClosed());
    }
}
