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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonTree;

/**
 * An HTTP response builder and writer for a mock exchange.
 */
public final class MockHttpResponse {

    private final OutputStream out;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private boolean committed;

    /**
     * @param out output stream to write HTTP response to
     */
    public MockHttpResponse(OutputStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    /**
     * Adds an HTTP header.
     *
     * @param name  header name
     * @param value header value
     * @return this response
     */
    public MockHttpResponse header(String name, String value) {
        headers.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * Sends a complete response with the given status, content-type and body text.
     *
     * @param status      HTTP status code
     * @param contentType MIME type, or {@code null}
     * @param body        response body text
     * @throws IOException on write error
     */
    public void send(int status, String contentType, String body) throws IOException {
        var bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        send(status, contentType, bytes);
    }

    /**
     * Sends a complete response with raw bytes.
     *
     * @param status      HTTP status code
     * @param contentType MIME type, or {@code null}
     * @param bytes       response body bytes
     * @throws IOException on write error
     */
    public void send(int status, String contentType, byte[] bytes) throws IOException {
        if (committed) {
            throw new IllegalStateException("Response already committed");
        }
        committed = true;
        var sb = new StringBuilder("HTTP/1.1 ").append(status).append(" ")
                .append(statusMessage(status)).append("\r\n");
        if (contentType != null) {
            sb.append("Content-Type: ").append(contentType).append("\r\n");
        }
        sb.append("Content-Length: ").append(bytes.length).append("\r\n");
        sb.append("Connection: close\r\n");
        for (var entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    /**
     * Sends a JSON response with status and string payload.
     *
     * @param status HTTP status code
     * @param json   JSON string
     * @throws IOException on write error
     */
    public void json(int status, String json) throws IOException {
        send(status, "application/json", json);
    }

    /**
     * Serializes a JSON object tree and sends it as application/json.
     *
     * @param status HTTP status code
     * @param tree   the JSON tree / object
     * @throws IOException on write error
     */
    public void json(int status, Object tree) throws IOException {
        json(status, JsonTree.write(tree));
    }

    /**
     * Starts a Server-Sent Events stream using chunked transfer encoding.
     *
     * @param status HTTP status code (typically 200)
     * @return an open {@link MockEventStream}
     * @throws IOException on write error
     */
    public MockEventStream events(int status) throws IOException {
        if (committed) {
            throw new IllegalStateException("Response already committed");
        }
        committed = true;
        var sb = new StringBuilder("HTTP/1.1 ").append(status).append(" ")
                .append(statusMessage(status)).append("\r\n");
        sb.append("Content-Type: text/event-stream\r\n");
        sb.append("Transfer-Encoding: chunked\r\n");
        sb.append("Cache-Control: no-cache\r\n");
        for (var entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return new MockEventStream(out);
    }

    /**
     * @return {@code true} if the response head has already been written
     */
    public boolean isCommitted() {
        return committed;
    }

    private static String statusMessage(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "Status " + status;
        };
    }
}
