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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MockHttpServer")
class MockHttpServerTest {

    @Test
    @DisplayName("starts loopback TCP server, handles requests, and captures arrivals")
    void tcpServerLifecycle() throws Exception {
        try (var server = MockHttpServer.startTcp(0, (req, res) -> {
                 if ("/echo".equals(req.path())) {
                     res.send(200, "text/plain", req.bodyText());
                 } else if ("/error".equals(req.path())) {
                     throw new IllegalStateException("Intentional test failure");
                 } else {
                     res.send(404, "application/json", "{\"error\":\"not_found\"}");
                 }
             })) {
            assertTrue(server.port() > 0);
            assertNotNull(server.localAddress());

            var client = HttpClient.newHttpClient();

            // 1. GET /echo with body
            var echoUri = URI.create("http://127.0.0.1:" + server.port() + "/echo");
            var req1 = HttpRequest.newBuilder(echoUri)
                    .POST(HttpRequest.BodyPublishers.ofString("echo-payload"))
                    .build();
            var res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, res1.statusCode());
            assertEquals("echo-payload", res1.body());

            // 2. GET /error -> handler exception returns 500 internal_error
            var errorUri = URI.create("http://127.0.0.1:" + server.port() + "/error");
            var req2 = HttpRequest.newBuilder(errorUri).GET().build();
            var res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
            assertEquals(500, res2.statusCode());
            assertTrue(res2.body().contains("internal_error"));

            // 3. GET /unknown -> 404
            var unknownUri = URI.create("http://127.0.0.1:" + server.port() + "/unknown");
            var req3 = HttpRequest.newBuilder(unknownUri).GET().build();
            var res3 = client.send(req3, HttpResponse.BodyHandlers.ofString());
            assertEquals(404, res3.statusCode());

            // 4. Verify request history and awaitRequest
            assertEquals(3, server.requests().size());
            var arrived = server.awaitRequest(Duration.ofMillis(100));
            assertNotNull(arrived);
        }
    }
}
