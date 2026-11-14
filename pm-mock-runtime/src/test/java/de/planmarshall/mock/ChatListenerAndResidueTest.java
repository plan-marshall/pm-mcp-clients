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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;

/**
 * The channel an operator act is recorded with — the listener's, never a request's default
 * ({@code 01-processes-and-transport.adoc}, Operator endpoints) — the transcript an abandoned entry
 * takes with it ({@code starting-work.adoc}, The transcript), and the chat routes of a subject whose id
 * reads {@code chat}.
 */
@DisplayName("ChatListenerAndResidue")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatListenerAndResidueTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String START_CHAT_STATE = StandardScenarios.PROJECTS_AND_START + "#start.chat";
    private static final String PLAN = "session-log";
    private static final String ENTRY_ID = "ent-3f9a2c41";
    private static final String BROADCAST = "/mock/broadcast";
    private static final String ACTS = "/mock/acts";
    private static final String CHANNEL = "channel";
    private static final String CHANNEL_CLI = "cli";
    private static final String CHANNEL_WEB = "web";
    private static final String DEVICE_ID = "device_id";
    private static final String KEY_ERROR = "error";
    private static final String TARGET_NOT_FOUND = "target_not_found";

    @Nested
    @DisplayName("The channel of an operator act")
    class Channel {

        @Test
        @DisplayName("is cli on the Unix socket and web with the device on the web listener")
        void fromTheListener() throws Exception {
            try (var runtime = webRuntime()) {
                assertEquals(201, post(runtime, ChatRoutes.plan(PLAN).chat(), "").status());

                var web = webPost(runtime, ChatRoutes.plan("relay-docs").chat(), "");

                assertAll("the recorded listeners",
                        () -> assertTrue(web.statusCode() == 200 || web.statusCode() == 201, web.body()),
                        () -> assertEquals(CHANNEL_CLI,
                                actFor(runtime, "/api/v1/plans/" + PLAN + "/chat").get(CHANNEL)),
                        () -> assertNull(actFor(runtime, "/api/v1/plans/" + PLAN + "/chat").get(DEVICE_ID),
                                "the socket names no device"),
                        () -> assertEquals(CHANNEL_WEB, actFor(runtime, "/api/v1/plans/relay-docs/chat").get(CHANNEL)),
                        () -> assertNotNull(actFor(runtime, "/api/v1/plans/relay-docs/chat").get(DEVICE_ID),
                                "a web act names the device"));
            }
        }

        @Test
        @DisplayName("is the listener's for a decision answered after a chat, not the request's default")
        void ofADecisionAnswered() throws Exception {
            try (var runtime = webRuntime();
                 var stream = openStream(runtime, "/api/v1/events")) {
                var answered = webPost(runtime, "/api/v1/decisions/Q-007/answer", "{\"option\":\"1\"}");
                assertEquals(200, answered.statusCode(), answered.body());

                var act = actFor(runtime, "/api/v1/decisions/Q-007/answer");
                var published = decisionAnswered(stream.events());
                assertAll("the answer's channel",
                        () -> assertEquals(CHANNEL_WEB, act.get(CHANNEL)),
                        () -> assertNotNull(act.get(DEVICE_ID), "the act names the device"),
                        () -> assertEquals(CHANNEL_WEB, published.get(CHANNEL)));
            }
        }

        /**
         * @return the payload of the first {@code decision.answered} on the stream
         */
        private static Map<String, Object> decisionAnswered(SseReader events)
                throws Exception {
            while (true) {
                var seen = events.next();
                assertNotNull(seen, "the stream ended");
                if ("decision.answered".equals(seen.event())) {
                    var payload = new LinkedHashMap<>(
                            JsonTree.asObject(JsonTree.parse(seen.data())));
                    payload.remove("event");
                    payload.remove("at");
                    payload.remove("scope");
                    if ("Q-007".equals(payload.get("id"))) {
                        return payload;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("An abandoned entry's transcript")
    class Abandoned {

        @Test
        @DisplayName("is deleted with the entry, not kept as an ended chat")
        void deletedWithTheEntry() throws Exception {
            var routes = ChatRoutes.entry(ENTRY_ID);
            try (var runtime = ordered(START_CHAT_STATE)) {
                assertEquals(200, post(runtime, routes.chat(), "").status());
                assertNotNull(read(runtime, routes), "the entry's chat exists");

                broadcast(runtime, "entry.abandoned", Map.of("entry_id", ENTRY_ID));

                var after = get(runtime, routes.chat());
                assertAll("the transcript went with the entry",
                        () -> assertEquals(404, after.status(), after.bodyText()),
                        () -> assertEquals(TARGET_NOT_FOUND,
                                JsonTree.string(JsonTree.asObject(JsonTree.parse(after.body())), KEY_ERROR)));
            }
        }
    }

    @Nested
    @DisplayName("A subject whose id reads chat")
    class IdNamedChat {

        @Test
        @DisplayName("opens, reads and takes a message on the routes ChatRoutes builds")
        void routable() throws Exception {
            var id = "chat";
            var routes = ChatRoutes.plan(id);
            try (var runtime = ordered(CHAT_STATE)) {
                var opened = post(runtime, routes.chat(), "");

                assertEquals(201, opened.status(), opened.bodyText());
                var chat = ChatResource.fromJson(opened.bodyText());
                assertEquals(id, chat.subject().id());
                assertEquals(chat.id(), read(runtime, routes).id());

                var message = new ChatTurnRequest(Generators.letterStrings(4, 40).next(),
                        Generators.letterStrings(12, 16).next());
                var accepted = post(runtime, routes.turns(), message.toJson());
                assertEquals(ChatTurnAccepted.HTTP_STATUS, accepted.status(), accepted.bodyText());
            }
        }
    }

    private Map<String, Object> actFor(MockRuntime runtime, String path) throws Exception {
        for (var act : acts(runtime)) {
            var map = JsonTree.asObject(act);
            if (map != null && path.equals(JsonTree.string(map, "path"))) {
                return map;
            }
        }
        throw new AssertionError("no act for " + path);
    }

    private List<Object> acts(MockRuntime runtime) throws Exception {
        var response = get(runtime, ACTS);
        assertEquals(200, response.status(), response.bodyText());
        var body = JsonTree.asObject(JsonTree.parse(response.body()));
        assertNotNull(body, response.bodyText());
        return body.get("acts") instanceof List<?> list ? List.copyOf(list) : List.of();
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static MockRuntime webRuntime() throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(CHAT_STATE).webPort(0)
                .timing(MockTiming.ORDERED).build());
    }

    private java.net.http.HttpResponse<String> webPost(MockRuntime runtime, String path, String body)
            throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + runtime.webPort() + path))
                .header("Content-Type", "application/json")
                .header(AUTH_HEADER, "Bearer " + runtime.deviceSecret())
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private static void broadcast(MockRuntime runtime, String event, Map<String, Object> data) throws IOException {
        var response = new UnixSocketHttpClient(runtime.socketPath()).send(HttpRequest.json("POST", BROADCAST,
                JsonTree.write(Map.of("event", event, "data", data))).withHeader(AUTH_HEADER,
                "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = get(runtime, routes.chat());
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpExchange openStream(MockRuntime runtime, String path)
            throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream(), path);
        return exchange;
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
