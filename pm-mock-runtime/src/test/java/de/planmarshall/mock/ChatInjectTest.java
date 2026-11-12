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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnDeltaEvent;
import de.planmarshall.api.chat.ChatTurnProgressEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;
import de.planmarshall.mock.scenario.StateDefinition.ChatFailure;

/**
 * Chat failures on demand through {@code POST /mock/inject} and a state's {@code chat_failures}: each
 * failure is played once by the next chat request or reply it matches, and the request or reply after it
 * is clean again. A reply ending {@code failed} names its reason {@code chat.reason.<name>}, a stopped
 * reply keeps its text so far, a replaced worker restarts the reply, an {@code http_error} answers with
 * its status and code, a {@code slow} request answers late; an unknown failure is refused, and a reset or
 * a scenario switch disarms what was not played.
 */
@DisplayName("ChatInject")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatInjectTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String CHAT_EVENT_PREFIX = "chat.";
    private static final String INJECT = "/mock/inject";
    private static final String KEY_ARMED = "armed";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_ERROR = "error";
    private static final String KEY_CHAT_ID = "chat_id";

    /**
     * One chat event as the plan's stream delivered it.
     *
     * @param name    the event's name
     * @param payload the payload, without the envelope's members
     */
    record Seen(String name, Map<String, Object> payload) {
    }

    @Nested
    @DisplayName("Reply failures")
    class ReplyFailures {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = ChatFailure.Kind.class, names = {"INFEASIBLE", "LOST", "TOO_LONG"})
        @DisplayName("ends the next reply failed with its reason after the first piece, then the chat is clean")
        void failsTheNextReply(ChatFailure.Kind kind) throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start();
                 var stream = open(runtime, planId)) {
                var chat = openChat(runtime, routes);
                var events = stream.events();

                var armed = inject(runtime, "{\"failure\":\"" + kind.wireValue() + "\"}");
                assertEquals(200, armed.status(), armed.bodyText());
                assertEquals(kind.wireValue(), JsonTree.string(body(armed), KEY_ARMED));
                assertEquals(List.of(kind.wireValue()), pending(armed));

                var sequence = sendAndPlay(runtime, routes, events, chat.id());
                var completed = ChatTurnCompletedEvent.fromMap(sequence.getLast().payload());
                assertAll("the failed reply",
                        () -> assertEquals(TurnStatus.FAILED, completed.status()),
                        () -> assertEquals("chat.reason." + kind.wireValue(),
                                Objects.requireNonNull(completed.reason()).key()),
                        () -> assertEquals(1, deltas(sequence).size(), "it fails after its first piece"),
                        () -> assertEquals(joinedDeltas(sequence), completed.text()));
                var afterFailure = read(runtime, routes);
                assertEquals(ChatStatus.TURN, afterFailure.state());
                assertEquals(TurnStatus.FAILED, afterFailure.turns().getLast().status());

                assertCleanReply(runtime, routes, events, chat.id());
            }
        }

        @Test
        @DisplayName("stops the next reply after its first piece, keeping its text so far, then the chat is clean")
        void stopsTheNextReply() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start();
                 var stream = open(runtime, planId)) {
                var chat = openChat(runtime, routes);
                var events = stream.events();
                assertEquals(200, inject(runtime, "{\"failure\":\"stop\"}").status());

                var sequence = sendAndPlay(runtime, routes, events, chat.id());
                var completed = ChatTurnCompletedEvent.fromMap(sequence.getLast().payload());
                assertAll("the stopped reply",
                        () -> assertEquals(TurnStatus.STOPPED, completed.status()),
                        () -> assertNull(completed.reason()),
                        () -> assertEquals(1, deltas(sequence).size()),
                        () -> assertFalse(completed.text().isEmpty()),
                        () -> assertEquals(joinedDeltas(sequence), completed.text()));
                assertEquals(TurnStatus.STOPPED, read(runtime, routes).turns().getLast().status());

                assertCleanReply(runtime, routes, events, chat.id());
            }
        }

        @Test
        @DisplayName("replaces the next reply's worker after its first piece: the reply restarts and completes")
        void replacesTheNextRepliesWorker() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start();
                 var stream = open(runtime, planId)) {
                var chat = openChat(runtime, routes);
                var events = stream.events();
                assertEquals(200, inject(runtime, "{\"failure\":\"worker_replaced\"}").status());

                var sequence = sendAndPlay(runtime, routes, events, chat.id());
                var replyStarts = new ArrayList<ChatTurnStartedEvent>();
                for (var seen : sequence) {
                    if (ChatTurnStartedEvent.EVENT_NAME.equals(seen.name())) {
                        var started = ChatTurnStartedEvent.fromMap(seen.payload());
                        if (!started.isOperator()) {
                            replyStarts.add(started);
                        }
                    }
                }
                assertEquals(2, replyStarts.size(), "the reply is announced again");
                assertEquals(replyStarts.get(0).turnId(), replyStarts.get(1).turnId());

                var restartAt = lastReplyStart(sequence);
                var rest = sequence.subList(restartAt + 1, sequence.size());
                assertEquals(ChatTurnProgressEvent.EVENT_NAME, rest.getFirst().name(), "from the working line");
                var completed = ChatTurnCompletedEvent.fromMap(sequence.getLast().payload());
                assertEquals(TurnStatus.COMPLETE, completed.status());
                assertEquals(joinedDeltas(rest), completed.text());
                assertEquals(2, read(runtime, routes).turns().size(), "a restart adds no turn");

                assertCleanReply(runtime, routes, events, chat.id());
            }
        }
    }

    @Nested
    @DisplayName("Request failures")
    class RequestFailures {

        @Test
        @DisplayName("answers the next chat request with the injected status and code, then the chat is clean")
        void answersWithTheInjectedError() throws Exception {
            var code = Generators.enumValues(ApiErrorCode.class).next();
            var status = Generators.integers(400, 599).next();
            var routes = ChatRoutes.plan(id());
            try (var runtime = start()) {
                openChat(runtime, routes);
                var armed = inject(runtime, "{\"failure\":\"http_error\",\"status\":" + status
                        + ",\"code\":\"" + code.wire() + "\"}");
                assertEquals(200, armed.status(), armed.bodyText());

                var failed = get(runtime, routes.chat());
                assertEquals(status, failed.status());
                assertEquals(code.wire(), JsonTree.string(body(failed), KEY_ERROR));

                var clean = get(runtime, routes.chat());
                assertEquals(200, clean.status(), clean.bodyText());
            }
        }

        @Test
        @DisplayName("answers the next chat request after the injected delay, then the chat is clean")
        void answersLate() throws Exception {
            var delayMs = Generators.integers(300, 600).next();
            var routes = ChatRoutes.plan(id());
            try (var runtime = start()) {
                openChat(runtime, routes);
                assertEquals(200, inject(runtime, "{\"failure\":\"slow\",\"delay_ms\":" + delayMs + "}").status());

                var before = System.nanoTime();
                var late = get(runtime, routes.chat());
                var elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
                assertEquals(200, late.status(), late.bodyText());
                assertTrue(elapsed >= delayMs, "answered after " + elapsed + " ms, injected " + delayMs + " ms");

                assertEquals(200, get(runtime, routes.chat()).status());
                assertEquals(List.of("stop"), pending(inject(runtime, "{\"failure\":\"stop\"}")),
                        "the slow request was played once");
            }
        }
    }

    @Nested
    @DisplayName("Arming")
    class Arming {

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"failure\":\"no-%s\"}",
                "{\"failure\":\"http_error\",\"status\":503}",
                "{\"failure\":\"http_error\",\"status\":200,\"code\":\"internal_fault\"}",
                "{\"failure\":\"stop\",\"delay_ms\":100}",
                "{\"failure\":\"lost\",\"%s\":1}"})
        @DisplayName("refuses an unknown failure or a member its failure does not take, arming nothing")
        void refusesInvalid(String template) throws Exception {
            try (var runtime = start()) {
                var refused = inject(runtime, template.formatted(Generators.letterStrings(4, 10).next()));

                assertEquals(400, refused.status(), refused.bodyText());
                assertEquals(ApiErrorCode.INVALID_ARGUMENTS.wire(), JsonTree.string(body(refused), KEY_ERROR));
                assertEquals(List.of("lost"), pending(inject(runtime, "{\"failure\":\"lost\"}")));
            }
        }

        @Test
        @DisplayName("lists every failure still armed, oldest first")
        void listsPending() throws Exception {
            try (var runtime = start()) {
                inject(runtime, "{\"failure\":\"slow\"}");
                inject(runtime, "{\"failure\":\"lost\"}");

                assertEquals(List.of("slow", "lost", "stop"), pending(inject(runtime, "{\"failure\":\"stop\"}")));
            }
        }

        @Test
        @DisplayName("a reset disarms what was not played")
        void resetDisarms() throws Exception {
            try (var runtime = start()) {
                inject(runtime, "{\"failure\":\"lost\"}");
                inject(runtime, "{\"failure\":\"slow\"}");

                runtime.reset();

                assertEquals(List.of("stop"), pending(inject(runtime, "{\"failure\":\"stop\"}")));
            }
        }

        @Test
        @DisplayName("a scenario switch disarms what was not played")
        void switchDisarms() throws Exception {
            try (var runtime = start()) {
                inject(runtime, "{\"failure\":\"infeasible\"}");

                runtime.switchScenario(CHAT_STATE);

                assertEquals(List.of("stop"), pending(inject(runtime, "{\"failure\":\"stop\"}")));
            }
        }

        @Test
        @DisplayName("a state's chat_failures are armed when the state is entered and played once")
        void stateArms() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            var scenario = withChatFailures(List.of(ChatFailure.of(ChatFailure.Kind.LOST)));
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .initialScenario(scenario).timing(MockTiming.ORDERED).build());
                 var stream = open(runtime, planId)) {
                var chat = openChat(runtime, routes);
                var events = stream.events();

                var sequence = sendAndPlay(runtime, routes, events, chat.id());
                var completed = ChatTurnCompletedEvent.fromMap(sequence.getLast().payload());
                assertEquals(TurnStatus.FAILED, completed.status());
                assertEquals("chat.reason.lost", Objects.requireNonNull(completed.reason()).key());

                assertCleanReply(runtime, routes, events, chat.id());
            }
        }
    }

    /**
     * The state {@code control.chat} arming the given failures when it is entered.
     */
    private static Scenario withChatFailures(List<ChatFailure> failures) {
        var base = new ScenarioRegistry().resolve(CHAT_STATE);
        var state = Objects.requireNonNull(base.state(base.start()));
        var states = new LinkedHashMap<>(base.states());
        states.put(base.start(), new StateDefinition(state.representation(), state.links(), state.events(),
                state.auto(), failures));
        return new Scenario(base.formatVersion(), base.name(), base.description(), base.start(), base.pairingCode(),
                base.routes(), states);
    }

    private static MockRuntime start() throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(CHAT_STATE).timing(MockTiming.ORDERED).build());
    }

    private static String id() {
        return Generators.letterStrings(4, 10).next().toLowerCase();
    }

    /**
     * Sends a message and reads the chat's events until the reply's {@code chat.turn.completed}.
     */
    private static List<Seen> sendAndPlay(MockRuntime runtime, ChatRoutes routes, SseReader events, String chatId)
            throws Exception {
        var message = new ChatTurnRequest(Generators.letterStrings(4, 40).next(),
                Generators.letterStrings(12, 16).next());
        var accepted = post(runtime, routes.turns(), message.toJson());
        assertEquals(ChatTurnAccepted.HTTP_STATUS, accepted.status(), accepted.bodyText());
        var seen = new ArrayList<Seen>();
        Seen next;
        do {
            next = nextOf(events, chatId);
            seen.add(next);
        } while (!ChatTurnCompletedEvent.EVENT_NAME.equals(next.name()));
        return seen;
    }

    /**
     * Asserts that the next reply plays clean: it completes with its whole text.
     */
    private static void assertCleanReply(MockRuntime runtime, ChatRoutes routes, SseReader events, String chatId)
            throws Exception {
        var sequence = sendAndPlay(runtime, routes, events, chatId);
        var completed = ChatTurnCompletedEvent.fromMap(sequence.getLast().payload());
        assertEquals(TurnStatus.COMPLETE, completed.status(), "the reply after the failure is clean");
        assertNull(completed.reason());
        assertTrue(deltas(sequence).size() > 1, "the clean reply comes in all its pieces");
        assertEquals(joinedDeltas(sequence), completed.text());
    }

    private static int lastReplyStart(List<Seen> sequence) {
        for (var i = sequence.size() - 1; i >= 0; i--) {
            if (ChatTurnStartedEvent.EVENT_NAME.equals(sequence.get(i).name())) {
                return i;
            }
        }
        throw new AssertionError("no reply was announced: " + sequence);
    }

    private static List<Seen> deltas(List<Seen> sequence) {
        return sequence.stream().filter(seen -> ChatTurnDeltaEvent.EVENT_NAME.equals(seen.name())).toList();
    }

    private static String joinedDeltas(List<Seen> sequence) {
        return deltas(sequence).stream()
                .map(seen -> JsonTree.string(seen.payload(), ChatTurnDeltaEvent.FIELD_TEXT))
                .collect(Collectors.joining());
    }

    private static Seen nextOf(SseReader events, String chatId) throws IOException {
        while (true) {
            var event = events.next();
            assertNotNull(event, "the stream ended");
            if (!event.event().startsWith(CHAT_EVENT_PREFIX)) {
                continue;
            }
            var data = new LinkedHashMap<>(JsonTree.asObject(JsonTree.parse(event.data())));
            data.remove("event");
            data.remove("at");
            data.remove("scope");
            if (chatId.equals(JsonTree.string(data, KEY_CHAT_ID))) {
                return new Seen(event.event(), data);
            }
        }
    }

    private static Map<String, Object> body(HttpResponse response) throws IOException {
        return JsonTree.asObject(JsonTree.parse(response.bodyText()));
    }

    private static List<String> pending(HttpResponse armed) throws IOException {
        var list = JsonTree.asArray(body(armed).get(KEY_PENDING));
        assertNotNull(list, armed.bodyText());
        return list.stream().map(String::valueOf).toList();
    }

    private static HttpResponse inject(MockRuntime runtime, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath()).send(HttpRequest.json("POST", INJECT, body));
    }

    private static ChatResource openChat(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertEquals(201, opened.status(), opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
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
