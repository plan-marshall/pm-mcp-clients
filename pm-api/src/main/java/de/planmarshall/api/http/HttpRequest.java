/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One HTTP/1.1 request to the runtime's socket.
 *
 * @param method  the request method, for example {@code GET}
 * @param target  the request target (path and query), starting with {@code /}
 * @param headers the request headers in sending order; the client adds {@code Host},
 *                {@code Content-Length} and {@code Connection} itself
 * @param body    the request body, empty for none
 */
public record HttpRequest(String method, String target, Map<String, String> headers, byte[] body) {

    /** Media type of JSON bodies. */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * Validates and copies the components.
     *
     * @param method  the request method
     * @param target  the request target
     * @param headers the request headers
     * @param body    the request body
     */
    public HttpRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(target, "target");
        if (!target.startsWith("/")) {
            throw new IllegalArgumentException("Request target must start with '/': " + target);
        }
        var copy = new LinkedHashMap<String, String>();
        Objects.requireNonNull(headers, "headers").forEach((name, value) -> copy.put(checked(name), checked(value)));
        headers = Collections.unmodifiableMap(copy);
        body = body == null ? new byte[0] : body.clone();
    }

    /**
     * @param target the request target
     * @return a {@code GET} request without headers
     */
    public static HttpRequest get(String target) {
        return new HttpRequest("GET", target, Map.of(), null);
    }

    /**
     * @param method the request method
     * @param target the request target
     * @param json   the JSON body
     * @return a request with a JSON body
     */
    public static HttpRequest json(String method, String target, String json) {
        return new HttpRequest(method, target, Map.of("Content-Type", APPLICATION_JSON),
                json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param target the request target
     * @return a {@code POST} request without body
     */
    public static HttpRequest post(String target) {
        return new HttpRequest("POST", target, Map.of(), null);
    }

    /**
     * @param target the request target
     * @return a {@code DELETE} request without body
     */
    public static HttpRequest delete(String target) {
        return new HttpRequest("DELETE", target, Map.of(), null);
    }

    /**
     * @param target the request target
     * @return a {@code PUT} request without body
     */
    public static HttpRequest put(String target) {
        return new HttpRequest("PUT", target, Map.of(), null);
    }

    /**
     * @param target the request target
     * @param json   the JSON body
     * @return a {@code PUT} request with JSON body
     */
    public static HttpRequest put(String target, String json) {
        return json("PUT", target, json);
    }

    /**
     * @param name  the header name
     * @param value the header value
     * @return a copy of this request with the header added or replaced
     */
    public HttpRequest withHeader(String name, String value) {
        var copy = new LinkedHashMap<>(headers);
        copy.put(name, value);
        return new HttpRequest(method, target, copy, body);
    }

    /**
     * @param json the JSON body
     * @return a copy of this request with the JSON body and application/json Content-Type
     */
    public HttpRequest withBody(String json) {
        var copy = new LinkedHashMap<>(headers);
        copy.put("Content-Type", APPLICATION_JSON);
        return new HttpRequest(method, target, copy, json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    int bodyLength() {
        return body.length;
    }

    byte[] bodyUnsafe() {
        return body;
    }

    private static String checked(String token) {
        Objects.requireNonNull(token, "header");
        if (token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Header contains a line break");
        }
        return token;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HttpRequest that && method.equals(that.method) && target.equals(that.target)
                && headers.equals(that.headers) && Arrays.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, target, headers, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return method + " " + target;
    }
}
