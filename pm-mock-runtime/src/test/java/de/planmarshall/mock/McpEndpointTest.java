/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

/**
 * The relay-facing MCP endpoint of the mock runtime: {@code POST /mcp} answers the JSON-RPC methods
 * the relay forwards, without a separate stub daemon.
 */
@DisplayName("MCP endpoint")
@EnabledOnOs({OS.MAC, OS.LINUX})
class McpEndpointTest {

    private static final String INITIALIZE = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":
             {"protocolVersion":"2025-11-25","clientInfo":{"name":"it","version":"1"},"capabilities":{}}}
            """;

    @Test
    @DisplayName("answers initialize with the negotiated protocol version")
    void initialize() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var res = client.send(HttpRequest.json("POST", "/mcp", INITIALIZE)
                    .withHeader("Authorization", "Bearer " + runtime.token())
                    .withHeader("Mcp-Method", "initialize"));

            assertEquals(200, res.status());
            var message = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals(1, JsonTree.integer(message, "id"));
            var result = JsonTree.object(message, "result");
            assertEquals("2025-11-25", JsonTree.string(result, "protocolVersion"));
        }
    }

    @Test
    @DisplayName("answers tools/list with a progress notification and a message frame on SSE")
    void toolsList() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var body = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}";

            try (var exchange = client.open(HttpRequest.json("POST", "/mcp", body)
                         .withHeader("Authorization", "Bearer " + runtime.token())
                         .withHeader("Mcp-Method", "tools/list"))) {
                assertTrue(exchange.isEventStream());
                var events = exchange.events();

                var progress = events.next();
                assertNotNull(progress);
                assertTrue(progress.data().contains("notifications/progress"));

                var message = events.next();
                assertNotNull(message);
                assertEquals("message", message.event());
                var frame = JsonTree.asObject(JsonTree.parse(message.data()));
                assertEquals(2, JsonTree.integer(frame, "id"));
                assertTrue(message.data().contains("pm_wait"));
            }
        }
    }

    @Test
    @DisplayName("answers tools/call and a notification, accepting the job token like the runtime")
    void toolsCallAndNotification() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());

            var call = client.send(HttpRequest.json("POST", "/mcp",
                    "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"pm_state\"}}")
                    .withHeader("PM-MCP-Job-Token", "job-token")
                    .withHeader("Mcp-Method", "tools/call"));
            assertEquals(200, call.status());
            var message = JsonTree.asObject(JsonTree.parse(call.bodyText()));
            assertEquals(3, JsonTree.integer(message, "id"));
            assertNotNull(JsonTree.object(message, "result"));

            var notification = client.send(HttpRequest.json("POST", "/mcp",
                    "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
                    .withHeader("Authorization", "Bearer " + runtime.token())
                    .withHeader("Mcp-Method", "notifications/initialized"));
            assertEquals(202, notification.status());
            assertEquals(0, notification.body().length);
        }
    }

    @Test
    @DisplayName("refuses /mcp without a runtime or job token")
    void requiresAuthentication() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var res = client.send(HttpRequest.json("POST", "/mcp", INITIALIZE)
                    .withHeader("Mcp-Method", "initialize"));
            assertEquals(401, res.status());
        }
    }
}
