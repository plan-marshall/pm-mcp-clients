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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import de.planmarshall.api.json.JsonTree;

/**
 * An active Server-Sent Events stream flushed per event using HTTP/1.1 chunked encoding.
 */
public final class MockEventStream implements AutoCloseable {

    private final OutputStream out;
    private volatile boolean closed;

    MockEventStream(OutputStream out) {
        this.out = out;
    }

    /**
     * Sends an event with optional name and text data.
     *
     * @param event the event type name, or {@code null}
     * @param data  the event payload text
     * @throws IOException on connection error
     */
    public synchronized void send(String event, String data) throws IOException {
        if (closed) {
            throw new IOException("Event stream is closed");
        }
        var sb = new StringBuilder();
        if (event != null && !event.isBlank()) {
            sb.append("event: ").append(event).append('\n');
        }
        var text = data == null ? "" : data;
        for (var line : text.split("\n", -1)) {
            sb.append("data: ").append(line).append('\n');
        }
        sb.append('\n');
        sendChunk(sb.toString());
    }

    /**
     * Sends an event with optional name and JSON serializable data.
     *
     * @param event the event type name, or {@code null}
     * @param data  the event payload object
     * @throws IOException on connection error
     */
    public synchronized void sendJson(String event, Object data) throws IOException {
        var json = data instanceof String s ? s : JsonTree.write(data);
        send(event, json);
    }

    /**
     * Sends an initial comment or ping.
     *
     * @param comment the comment line
     * @throws IOException on connection error
     */
    public synchronized void sendComment(String comment) throws IOException {
        if (closed) {
            throw new IOException("Event stream is closed");
        }
        sendChunk(": " + comment + "\n\n");
    }

    private void sendChunk(String text) throws IOException {
        var bytes = text.getBytes(StandardCharsets.UTF_8);
        out.write((Integer.toHexString(bytes.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                out.write("0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } catch (IOException e) {
                // Client might have already disconnected
            }
        }
    }

    /**
     * @return {@code true} if closed
     */
    public boolean isClosed() {
        return closed;
    }
}
