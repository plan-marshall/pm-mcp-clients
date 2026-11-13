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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.NarrativeLine;
import de.planmarshall.api.chat.RefKind;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;

/**
 * What a chat knows of its subject ({@code state-catalogue.adoc#chat-context}) and what happened to
 * the subject while the chat stays ({@code state-catalogue.adoc#chat-narrative}): the context is
 * rendered from the plan's detail before the first turn and refreshed with every role turn, and the
 * subject's events become notice turns between two replies — one notice for the lines that come at
 * once, held while a reply is being written, and none for ended work.
 */
@DisplayName("ChatContextAndNarrative")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatContextNarrativeTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String FOLLOW_UP_STATE = StandardScenarios.PROJECTS_AND_START + "#start.follow-up";
    private static final String PLAN = "session-log";
    private static final String BROADCAST = "/mock/broadcast";
    private static final String KEY_COUNT = "count";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_VERSION = "version";
    private static final String PLAN_ID = "plan_id";
    private static final String TASK_ID = "task_id";
    private static final String STATUS = "status";
    private static final String KEY_ID = "id";
    private static final String KEY_KIND = "kind";
    private static final String KEY_BLOCKS = "blocks";
    private static final String HOLD_ID = "hold_id";
    private static final String KEY_REASON = "reason";

    @Nested
    @DisplayName("What the role knows")
    class Knows {

        @Test
        @DisplayName("is rendered from the plan's detail before the first turn")
        void beforeTheFirstTurn() throws Exception {
            try (var runtime = ordered(CHAT_STATE)) {
                var chat = open(runtime, ChatRoutes.plan(PLAN));

                var knows = chat.context().knows();
                assertAll("what the role is given of session-log",
                        () -> assertEquals(List.of("chat.knows.description", "chat.knows.tasks",
                                        "chat.knows.phase", "chat.knows.version"),
                                knows.stream().map(MessageText::key).toList()),
                        () -> assertEquals("3", String.valueOf(knows.get(1).args().get(KEY_COUNT))),
                        () -> assertEquals("execute", knows.get(2).args().get(KEY_PHASE)),
                        () -> assertEquals("1", String.valueOf(knows.get(3).args().get(KEY_VERSION))),
                        () -> assertFalse(chat.context().asOf().isBlank()));
            }
        }

        @Test
        @DisplayName("is refreshed by every role turn, so a turn on a changed subject names the new phase")
        void refreshedByEveryTurn() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE);
                 var stream = openStream(runtime, "/api/v1/plans/" + PLAN + "/events")) {
                var chat = open(runtime, routes);
                send(runtime, routes);

                var first = replyStarted(stream.events(), chat.id());
                broadcast(runtime, "phase.changed", Map.of(PLAN_ID, PLAN, KEY_PHASE, "finalize"));
                send(runtime, routes);
                var second = replyStarted(stream.events(), chat.id());

                assertAll("the context of the role's turns",
                        () -> assertNotNull(first.context()),
                        () -> assertEquals("execute", phaseOf(Objects.requireNonNull(first.context()).knows())),
                        () -> assertNotNull(second.context()),
                        () -> assertEquals("finalize", phaseOf(Objects.requireNonNull(second.context()).knows())));
            }
        }
    }

    @Nested
    @DisplayName("What happened to the subject")
    class Narrative {

        @Test
        @DisplayName("five events that come at once are one notice with five lines, refs on the lines")
        void fiveEventsAreOneNotice() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);

                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-3", STATUS, "done"));
                broadcast(runtime, "phase.changed", Map.of(PLAN_ID, PLAN, KEY_PHASE, "finalize"));
                broadcast(runtime, "decision.opened", Map.of(PLAN_ID, PLAN, KEY_ID, "Q-9", KEY_BLOCKS, "plan"));
                broadcast(runtime, "hold.opened", Map.of(PLAN_ID, PLAN, HOLD_ID, "H-1", KEY_REASON, "review"));
                broadcast(runtime, "hold.cleared", Map.of(PLAN_ID, PLAN, HOLD_ID, "H-1", KEY_REASON, "done"));

                var notices = noticesOf(read(runtime, routes));
                assertAll("one notice of five lines",
                        () -> assertEquals(1, notices.size(), String.valueOf(notices)),
                        () -> assertEquals(List.of("chat.line.task_ended", "chat.line.phase_changed",
                                        "chat.line.decision_opened", "chat.line.hold_opened", "chat.line.hold_cleared"),
                                notices.getFirst().lines().stream().map(NarrativeLine::key).toList()),
                        () -> assertTrue(notices.getFirst().lines().get(0).refs().stream()
                                .anyMatch(ref -> ref.kind() == RefKind.TASK), "the task is referenced"),
                        () -> assertTrue(notices.getFirst().lines().get(3).refs().stream()
                                .anyMatch(ref -> ref.kind() == RefKind.HOLD), "the hold is referenced"),
                        () -> assertTrue(notices.getFirst().lines().get(2).refs().stream()
                                .anyMatch(ref -> ref.kind() == RefKind.DECISION), "the decision is referenced"));
            }
        }

        @Test
        @DisplayName("waits for the end of a reply being written and follows it as one notice")
        void waitsForTheReply() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = realistic(CHAT_STATE);
                 var stream = openStream(runtime, "/api/v1/plans/" + PLAN + "/events")) {
                var chat = open(runtime, routes);
                send(runtime, routes);
                var events = stream.events();
                awaitFirstDelta(events, chat.id());

                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-3", STATUS, "done"));

                assertTrue(noticesOf(read(runtime, routes)).isEmpty(),
                        "a line that arises while a reply is written is held");
                awaitCompletion(events, chat.id());
                var written = read(runtime, routes);
                assertAll("the notice follows the reply",
                        () -> assertEquals(TurnKind.NOTICE, written.turns().getLast().kind()),
                        () -> assertEquals("chat.line.task_ended",
                                written.turns().getLast().lines().getFirst().key()));
            }
        }

        @Test
        @DisplayName("is none for ended work, whose archive never changes")
        void noneForEndedWork() throws Exception {
            var routes = ChatRoutes.endedPlan("codeberg-issues");
            try (var runtime = ordered(FOLLOW_UP_STATE)) {
                assertEquals(ChatScopeKind.ARCHIVE, open(runtime, routes).scope());

                broadcast(runtime, "task.ended", Map.of(PLAN_ID, "codeberg-issues", TASK_ID, "TASK-1",
                        STATUS, "done"));

                assertTrue(read(runtime, routes).turns().isEmpty(), "ended work has no narrative");
            }
        }

        @Test
        @DisplayName("a line after a reply is a new notice, not the one before the reply")
        void aNewNoticeAfterAReply() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-1", STATUS, "done"));
                send(runtime, routes);
                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-2", STATUS, "done"));

                var notices = noticesOf(read(runtime, routes));
                assertAll("two notices around a reply",
                        () -> assertEquals(2, notices.size(), String.valueOf(notices)),
                        () -> assertEquals(2, notices.stream().mapToInt(notice -> notice.lines().size()).sum()));
            }
        }

        @Test
        @DisplayName("reads the subject of a catalogue-shaped event from the scope the event declares")
        void catalogueShapedEvent() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                // The catalogue's decision.answered names the decision and the envelope's scope, not a plan_id.
                broadcast(runtime, "decision.answered", Map.of(KEY_ID, "Q-7", "channel", "web", "device", "dev-1",
                        "scope", Map.of(KEY_KIND, "plan", KEY_ID, PLAN)));

                var notices = noticesOf(read(runtime, routes));
                assertAll("the line of a catalogue-shaped event",
                        () -> assertEquals(1, notices.size(), String.valueOf(notices)),
                        () -> assertEquals("chat.line.decision_answered",
                                notices.getFirst().lines().getFirst().key()));
            }
        }

        @Test
        @DisplayName("a role's reply names a hold and the act the operator may take, where scripted")
        void replyNamesAHoldAndAnAct() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                for (var index = 0; index < 4; index++) {
                    send(runtime, routes);
                }

                var reply = read(runtime, routes).turns().stream()
                        .filter(turn -> turn.kind() == TurnKind.MESSAGE && !turn.isOperator()).toList().getLast();
                assertAll("the later reply names a hold and an act",
                        () -> assertTrue(reply.refs().stream().anyMatch(ref -> ref.kind() == RefKind.HOLD)),
                        () -> assertTrue(reply.refs().stream().anyMatch(ref -> ref.kind() == RefKind.ACT
                                && "pause".equals(ref.act())
                                && new ChatSubject(Objects.requireNonNull(ref.subject()).kind(), PLAN)
                                .equals(ref.subject()))));
            }
        }
    }

    private static String phaseOf(List<MessageText> knows) {
        return knows.stream().filter(known -> "chat.knows.phase".equals(known.key())).findFirst()
                .map(known -> String.valueOf(known.args().get(KEY_PHASE))).orElse("");
    }

    private static List<ChatTurn> noticesOf(ChatResource chat) {
        return chat.turns().stream().filter(turn -> turn.kind() == TurnKind.NOTICE).toList();
    }

    private static ChatTurnStartedEvent replyStarted(SseReader events, String chatId) throws IOException {
        while (true) {
            var seen = events.next();
            assertNotNull(seen, "the stream ended");
            if (!ChatTurnStartedEvent.EVENT_NAME.equals(seen.event())) {
                continue;
            }
            var payload = payloadOf(seen.data());
            var started = ChatTurnStartedEvent.fromMap(payload);
            if (chatId.equals(started.chatId()) && !started.isOperator()) {
                return started;
            }
        }
    }

    private static void awaitFirstDelta(SseReader events, String chatId) throws IOException {
        while (true) {
            var seen = events.next();
            assertNotNull(seen, "the stream ended");
            if ("chat.turn.delta".equals(seen.event())
                    && chatId.equals(String.valueOf(payloadOf(seen.data()).get("chat_id")))) {
                return;
            }
        }
    }

    private static void awaitCompletion(SseReader events, String chatId) throws IOException {
        while (true) {
            var seen = events.next();
            assertNotNull(seen, "the stream ended");
            if (!ChatTurnCompletedEvent.EVENT_NAME.equals(seen.event())) {
                continue;
            }
            var payload = payloadOf(seen.data());
            var completed = ChatTurnCompletedEvent.fromMap(payload);
            if (chatId.equals(completed.chatId()) && completed.kind() == TurnKind.MESSAGE) {
                return;
            }
        }
    }

    private static Map<String, Object> payloadOf(String data) throws IOException {
        var payload = new LinkedHashMap<>(JsonTree.asObject(JsonTree.parse(data)));
        payload.remove("event");
        payload.remove("at");
        payload.remove("scope");
        return payload;
    }

    private static void broadcast(MockRuntime runtime, String event, Map<String, Object> data) throws IOException {
        var response = new UnixSocketHttpClient(runtime.socketPath()).send(HttpRequest.json("POST", BROADCAST,
                JsonTree.write(Map.of("event", event, "data", data))).withHeader(AUTH_HEADER,
                "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static MockRuntime realistic(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.REALISTIC).build());
    }

    private static ChatResource open(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertTrue(opened.status() == 200 || opened.status() == 201, opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
    }

    private static void send(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var message = new ChatTurnRequest(Generators.letterStrings(4, 40).next(),
                Generators.letterStrings(12, 16).next());
        var accepted = post(runtime, routes.turns(), message.toJson());
        assertEquals(202, accepted.status(), accepted.bodyText());
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

    private static HttpExchange openStream(MockRuntime runtime, String path) throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream(), path);
        return exchange;
    }
}
