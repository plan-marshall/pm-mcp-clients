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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("MockEventStream")
@EnableGeneratorController
class MockEventStreamTest {

    @Test
    @DisplayName("formats single-line and multi-line SSE messages with chunked encoding")
    void sendTextEvents() throws Exception {
        var out = new ByteArrayOutputStream();
        var eventName = Generators.letterStrings(4, 10).next();
        var payload = Generators.letterStrings(4, 12).next();
        var unnamedPayload = Generators.letterStrings(4, 12).next();
        var firstLine = Generators.letterStrings(4, 10).next();
        var secondLine = Generators.letterStrings(4, 10).next();
        try (var stream = new MockEventStream(out)) {
            assertFalse(stream.isClosed());
            stream.send(eventName, payload);
            stream.send(null, unnamedPayload);
            stream.send(eventName, firstLine + "\n" + secondLine);
        }

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("event: " + eventName + "\ndata: " + payload + "\n\n"));
        assertTrue(raw.contains("data: " + unnamedPayload + "\n\n"));
        assertTrue(raw.contains("event: " + eventName + "\ndata: " + firstLine + "\ndata: " + secondLine + "\n\n"));
        assertTrue(raw.endsWith("0\r\n\r\n"));
    }

    @Test
    @DisplayName("sends JSON serializable payloads and raw strings")
    void sendJsonEvents() throws Exception {
        var out = new ByteArrayOutputStream();
        var statusEvent = Generators.letterStrings(4, 10).next();
        var member = Generators.letterStrings(4, 10).next();
        var rawEvent = Generators.letterStrings(4, 10).next();
        var rawMember = Generators.letterStrings(4, 10).next();
        var number = Generators.integers(1, 999).next();
        try (var stream = new MockEventStream(out)) {
            stream.sendJson(statusEvent, Map.of(member, true));
            stream.sendJson(rawEvent, "{\"" + rawMember + "\":" + number + "}");
        }

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("event: " + statusEvent + "\ndata: {\"" + member + "\":true}\n\n"));
        assertTrue(raw.contains("event: " + rawEvent + "\ndata: {\"" + rawMember + "\":" + number + "}\n\n"));
    }

    @Test
    @DisplayName("sends SSE comments and ping heartbeats")
    void sendComments() throws Exception {
        var out = new ByteArrayOutputStream();
        var comment = Generators.letterStrings(4, 12).next();
        try (var stream = new MockEventStream(out)) {
            stream.sendComment(comment);
        }

        var raw = out.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains(": " + comment + "\n\n"));
    }

    @Test
    @DisplayName("rejects sends after stream is closed")
    void rejectAfterClose() throws Exception {
        var out = new ByteArrayOutputStream();
        var stream = new MockEventStream(out);
        var eventName = Generators.letterStrings(4, 10).next();
        var payload = Generators.letterStrings(4, 12).next();
        stream.close();
        assertTrue(stream.isClosed());

        assertThrows(IOException.class, () -> stream.send(eventName, payload));
        assertThrows(IOException.class, () -> stream.sendComment(payload));

        // Idempotent close
        stream.close();
        assertTrue(stream.isClosed());
    }
}
