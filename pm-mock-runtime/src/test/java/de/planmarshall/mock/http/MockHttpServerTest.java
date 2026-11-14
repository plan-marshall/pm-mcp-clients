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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;

@DisplayName("MockHttpServer")
@EnableGeneratorController
class MockHttpServerTest {

    @Test
    @DisplayName("starts loopback TCP server, handles requests, and captures arrivals")
    void tcpServerLifecycle() throws Exception {
        var echoPath = "/echo-" + Generators.letterStrings(4, 10).next();
        var errorPath = "/error-" + Generators.letterStrings(4, 10).next();
        var payload = Generators.letterStrings(4, 12).next();
        try (var server = MockHttpServer.startTcp(0, (req, res) -> {
                 if (echoPath.equals(req.path())) {
                     res.send(200, "text/plain", req.bodyText());
                 } else if (errorPath.equals(req.path())) {
                     throw new IllegalStateException("Intentional test failure");
                 } else {
                     res.send(404, "application/json", "{\"error\":\"not_found\"}");
                 }
             })) {
            assertTrue(server.port() > 0);
            assertNotNull(server.localAddress());

            var client = HttpClient.newHttpClient();

            // 1. GET /echo with body
            var echoUri = URI.create("http://127.0.0.1:" + server.port() + echoPath);
            var req1 = HttpRequest.newBuilder(echoUri)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            var res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, res1.statusCode());
            assertEquals(payload, res1.body());

            // 2. GET /error -> handler exception returns 500 internal_error
            var errorUri = URI.create("http://127.0.0.1:" + server.port() + errorPath);
            var req2 = HttpRequest.newBuilder(errorUri).GET().build();
            var res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
            assertEquals(500, res2.statusCode());
            assertEquals(ApiErrorCode.INTERNAL_FAULT, codeOf(res2.body()),
                    "a handler failure is the catalogue's internal_fault");

            // 3. GET /unknown-<payload> -> 404
            var unknownUri = URI.create("http://127.0.0.1:" + server.port() + "/unknown-" + payload);
            var req3 = HttpRequest.newBuilder(unknownUri).GET().build();
            var res3 = client.send(req3, HttpResponse.BodyHandlers.ofString());
            assertEquals(404, res3.statusCode());

            // 4. Verify request history and awaitRequest
            assertEquals(3, server.requests().size());
            var arrived = server.awaitRequest(Duration.ofMillis(100));
            assertNotNull(arrived);
        }
    }

    @Test
    @DisplayName("answers a handler failure as valid JSON that carries the failure's message unchanged")
    void failureMessageIsValidJson() throws Exception {
        var message = Generators.letterStrings(2, 6).next() + "\\path \"quoted\"\n"
                + Generators.letterStrings(2, 6).next();
        try (var server = MockHttpServer.startTcp(0, (req, res) -> {
                 throw new IllegalStateException(message);
             })) {
            var response = rawExchange(server.port(), "GET /fail HTTP/1.1\r\nHost: x\r\n\r\n");

            assertTrue(response.startsWith("HTTP/1.1 500"), response);
            var body = JsonTree.asObject(JsonTree.parse(response.substring(response.indexOf("\r\n\r\n") + 4)));
            assertNotNull(body, "the error body is a JSON object");
            assertEquals(ApiErrorCode.INTERNAL_FAULT.wire(), JsonTree.string(body, "error"));
            assertEquals(message, JsonTree.string(body, "message"));
        }
    }

    @Test
    @DisplayName("answers a request the handler leaves unanswered with internal_fault")
    void unansweredRequest() throws Exception {
        try (var server = MockHttpServer.startTcp(0, (req, res) -> {
                 // answers nothing
             })) {
            var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + server.port() + "/" + Generators.letterStrings(4, 10).next()))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(500, response.statusCode());
            assertEquals(ApiErrorCode.INTERNAL_FAULT, codeOf(response.body()));
        }
    }

    @Test
    @DisplayName("closes a connection whose request line names no target, recording no request")
    void malformedRequestLine() throws Exception {
        try (var server = MockHttpServer.startTcp(0, (req, res) -> res.send(200, "text/plain", "ok"))) {
            var response = rawExchange(server.port(), Generators.letterStrings(4, 10).next() + "\r\n\r\n");

            assertEquals("", response, "nothing is answered");
            assertTrue(server.requests().isEmpty());
        }
    }

    @Test
    @DisplayName("reads a request with bare line feeds, skips a header without a colon, "
            + "and reads a malformed Content-Length as no body")
    void lenientHeaders() throws Exception {
        var headerValue = Generators.letterStrings(4, 10).next();
        try (var server = MockHttpServer.startTcp(0, (req, res) -> res.send(200, "text/plain", req.bodyText()))) {
            var response = rawExchange(server.port(), "POST /echo HTTP/1.1\nX-Probe: " + headerValue
                    + "\nno colon here\nContent-Length: " + Generators.letterStrings(2, 4).next() + "\n\n");

            assertTrue(response.startsWith("HTTP/1.1 200"), response);
            var request = server.requests().getFirst();
            assertEquals(headerValue, request.header("x-probe"));
            assertEquals("", request.bodyText(), "a malformed Content-Length reads no body");
        }
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("serves a Unix domain socket and removes it on close")
    void unixSocket(@TempDir Path directory) throws Exception {
        var socket = directory.resolve("s.sock");
        var payload = Generators.letterStrings(4, 12).next();
        var server = MockHttpServer.startUnix(socket, (req, res) -> res.send(200, "text/plain", payload));
        try {
            assertEquals(-1, server.port(), "a Unix domain socket has no TCP port");
            var response = new UnixSocketHttpClient(socket)
                    .send(de.planmarshall.api.http.HttpRequest.get("/" + Generators.letterStrings(4, 10).next()));
            assertEquals(200, response.status());
            assertEquals(payload, response.bodyText());
        } finally {
            server.close();
        }

        assertFalse(Files.exists(socket), "closing the server removes its socket file");
    }

    private static ApiErrorCode codeOf(String body) throws IOException {
        return ApiErrorCode.fromWireOrNull(JsonTree.string(JsonTree.asObject(JsonTree.parse(body)), "error"));
    }

    /**
     * Writes raw bytes to the server and reads everything it answers until it closes the connection.
     */
    private static String rawExchange(int port, String request) throws IOException {
        try (var socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(5_000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.shutdownOutput();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
