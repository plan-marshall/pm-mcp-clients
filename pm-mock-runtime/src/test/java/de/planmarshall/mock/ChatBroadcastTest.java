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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatEndedEvent;
import de.planmarshall.api.chat.ChatProposalEvent;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;

/**
 * A second device played through {@code POST /mock/broadcast}: a chat event names its chat by
 * {@code chat_id}, is applied to the chat and reaches the stream of the chat's subject — an operator's
 * message, an accepted proposal and the chat's end.
 */
@DisplayName("ChatBroadcast")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatBroadcastTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String BROADCAST = "/mock/broadcast";
    private static final String CHAT_PREFIX = "chat.";
    private static final String ERROR = "error";

    /**
     * One chat event as the plan's stream delivered it.
     *
     * @param name    the event's name
     * @param scope   the envelope's scope
     * @param payload the payload, without the envelope's members
     */
    record Seen(String name, Map<String, Object> scope, Map<String, Object> payload) {
    }

    @Test
    @DisplayName("a message of another device reaches the plan's stream and joins the transcript")
    void message() throws Exception {
        var planId = id();
        var routes = ChatRoutes.plan(planId);
        try (var runtime = ordered();
             var stream = open(runtime, planId)) {
            var chat = openChat(runtime, routes);
            var turnId = "T-" + Generators.letterStrings(6, 10).next();
            var text = Generators.letterStrings(4, 40).next();
            var started = new ChatTurnStartedEvent(chat.id(), turnId, TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                    TurnFormat.PLAIN, false, null, null, text);

            var broadcast = broadcast(runtime, ChatTurnStartedEvent.EVENT_NAME, started.toMap());

            assertEquals(200, broadcast.status(), broadcast.bodyText());
            var seen = next(stream.events(), chat.id());
            assertOnPlanStream(seen, planId, ChatTurnStartedEvent.EVENT_NAME);
            assertEquals(started, ChatTurnStartedEvent.fromMap(seen.payload()));
            var turn = read(runtime, routes).turns().getLast();
            assertAll("the message of the other device",
                    () -> assertEquals(turnId, turn.turnId()),
                    () -> assertTrue(turn.isOperator()),
                    () -> assertEquals(text, turn.text()));
        }
    }

    @Test
    @DisplayName("a proposal accepted on another device reaches the plan's stream and answers the chat's proposal")
    void acceptedProposal() throws Exception {
        var planId = id();
        var routes = ChatRoutes.plan(planId);
        try (var runtime = ordered()) {
            var chat = openChat(runtime, routes);
            send(runtime, routes);
            send(runtime, routes);
            var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
            assertEquals(ProposalKind.REVISION, proposal.kind());
            var accepted = new ChatProposalEvent(chat.id(), proposal.turnId(), proposal.replyTurnId(),
                    proposal.kind(), ProposalState.ACCEPTED, null);
            // The stream is opened once the replies are written, so the test does not have to drain them.
            try (var stream = open(runtime, planId)) {

                var broadcast = broadcast(runtime, ChatProposalEvent.EVENT_NAME, accepted.toMap());

                assertEquals(200, broadcast.status(), broadcast.bodyText());
                var seen = untilProposal(stream.events(), chat.id(), ProposalState.ACCEPTED);
                assertOnPlanStream(seen, planId, ChatProposalEvent.EVENT_NAME);
                assertEquals(accepted, ChatProposalEvent.fromMap(seen.payload()));
            }
            var answered = read(runtime, routes);
            assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(answered.proposal()).state());
            assertEquals(ChatStatus.TURN, answered.state());
            var again = post(runtime, routes.accept(), new ProposalAnswerRequest(proposal.turnId()).toJson());
            assertEquals(409, again.status(), "the proposal is answered for good");
        }
    }

    @Test
    @DisplayName("the end on another device reaches the plan's stream and ends the chat")
    void end() throws Exception {
        var planId = id();
        var routes = ChatRoutes.plan(planId);
        try (var runtime = ordered();
             var stream = open(runtime, planId)) {
            var chat = openChat(runtime, routes);
            var ended = new ChatEndedEvent(chat.id(), ChatEndedEvent.EndedBy.OPERATOR, "web",
                    Generators.letterStrings(4, 12).next());

            var broadcast = broadcast(runtime, ChatEndedEvent.EVENT_NAME, ended.toMap());

            assertEquals(200, broadcast.status(), broadcast.bodyText());
            var seen = next(stream.events(), chat.id());
            assertOnPlanStream(seen, planId, ChatEndedEvent.EVENT_NAME);
            assertEquals(ended, ChatEndedEvent.fromMap(seen.payload()));
            assertEquals(ChatStatus.END, read(runtime, routes).state());
            assertEquals(409, post(runtime, routes.end(), "").status(), "the chat has ended");
        }
    }

    @Test
    @DisplayName("refuses a chat nobody opened and a payload its event's record refuses")
    void refuses() throws Exception {
        try (var runtime = ordered()) {
            var chat = openChat(runtime, ChatRoutes.plan(id()));
            var unknown = new ChatEndedEvent("C-" + Generators.letterStrings(6, 10).next(),
                    ChatEndedEvent.EndedBy.OPERATOR, null, null);

            assertRefused(broadcast(runtime, ChatEndedEvent.EVENT_NAME, unknown.toMap()), 404, "target_not_found");
            assertRefused(broadcast(runtime, ChatEndedEvent.EVENT_NAME,
                    Map.of(ChatEndedEvent.FIELD_CHAT_ID, chat.id(), ChatEndedEvent.FIELD_ENDED_BY,
                            Generators.letterStrings(6, 10).next())), 400, "invalid_arguments");
        }
    }

    private static void assertOnPlanStream(Seen seen, String planId, String name) {
        assertAll("an event of the plan's chat",
                () -> assertEquals(name, seen.name()),
                () -> assertEquals("plan", JsonTree.string(seen.scope(), "kind")),
                () -> assertEquals(planId, JsonTree.string(seen.scope(), "id")));
    }

    private static void assertRefused(HttpResponse response, int status, String error) throws Exception {
        assertEquals(status, response.status(), response.bodyText());
        assertEquals(error, JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), ERROR));
    }

    /**
     * Reads the chat's events, the reply's among them, until {@code chat.proposal} names the state.
     */
    private static Seen untilProposal(SseReader events, String chatId, ProposalState state) throws IOException {
        Seen seen;
        do {
            seen = next(events, chatId);
        } while (!(ChatProposalEvent.EVENT_NAME.equals(seen.name())
                && state.wireValue().equals(JsonTree.string(seen.payload(), ChatProposalEvent.FIELD_STATE))));
        return seen;
    }

    private static Seen next(SseReader events, String chatId) throws IOException {
        while (true) {
            var event = events.next();
            assertNotNull(event, "the stream ended");
            if (!event.event().startsWith(CHAT_PREFIX)) {
                continue;
            }
            var data = new LinkedHashMap<>(JsonTree.asObject(JsonTree.parse(event.data())));
            var scope = JsonTree.object(data, "scope");
            data.remove("event");
            data.remove("at");
            data.remove("scope");
            if (chatId.equals(JsonTree.string(data, ChatEndedEvent.FIELD_CHAT_ID))) {
                return new Seen(event.event(), scope, data);
            }
        }
    }

    private static HttpResponse broadcast(MockRuntime runtime, String event, Map<String, Object> data)
            throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", BROADCAST, JsonTree.write(Map.of("event", event, "data", data))));
    }

    private static MockRuntime ordered() throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(CHAT_STATE).timing(MockTiming.ORDERED).build());
    }

    private static String id() {
        return Generators.letterStrings(4, 10).next().toLowerCase();
    }

    private static ChatResource openChat(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertEquals(201, opened.status(), opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
    }

    private static void send(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var message = new ChatTurnRequest(Generators.letterStrings(4, 40).next(),
                Generators.letterStrings(12, 16).next());
        assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message.toJson()).status());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(routes.chat()).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpExchange open(MockRuntime runtime, String planId) throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get("/api/v1/plans/" + planId + "/events")
                        .withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream());
        return exchange;
    }
}
