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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


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
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.NarrativeLine;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.RefKind;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.decision.AnswerType;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionOpenedEvent;
import de.planmarshall.api.decision.DecisionOption;
import de.planmarshall.api.decision.DecisionPart;
import de.planmarshall.api.decision.DecisionScope;
import de.planmarshall.api.decision.Reversibility;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.mock.runtime.MockTiming;

/**
 * Verifies narrative completeness, epic/entry knows items, native {@code decision.opened}, pausing
 * pins, removal of hard-coded {@code H-1} hold refs, {@code instance.changed} emission on applied
 * revisions, and single-turn notice extension (PLAN-194 D5).
 */
@DisplayName("Narrative completeness, knows items, and D5 pins")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatNarrativeCompletenessTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String CONTROL_EPIC_STATE = StandardScenarios.RUNNING_WORK + "#control.epic";
    private static final String START_CHAT_STATE = StandardScenarios.PROJECTS_AND_START + "#start.chat";
    private static final String BROADCAST = "/mock/broadcast";
    private static final String PLAN = "session-log";
    private static final String EPIC = "relay-hardening";

    @Nested
    @DisplayName("Plan pull-request, native decision, pausing, instance, and reply pins")
    class PlanAndCrossCuttingPins {

        @Test
        @DisplayName("merged plan emits pr_merged line before ending the chat")
        void prMergedLineBeforeChatEnds() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                broadcast(runtime, "scope.ended", Map.of(
                        "scope", Map.of("kind", "plan", "id", PLAN),
                        "outcome", "merged",
                        "pull_request_number", 198));

                var lines = allNoticeLines(read(runtime, routes));
                assertEquals(List.of("chat.line.pr_merged"), lines.stream().map(NarrativeLine::key).toList());
                assertEquals(198, intArg(lines.getFirst().args().get("number")));
            }
        }

        @Test
        @DisplayName("merged plan emits pr_merged line even when scope.ended interrupts a streaming reply")
        void prMergedEmittedWhenScopeEndsDuringReply() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            var prNumber = Generators.integers(100, 999).next();
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .scenario(CHAT_STATE).timing(MockTiming.REALISTIC).build())) {
                open(runtime, routes);
                var req = new ChatTurnRequest(Generators.letterStrings(4, 24).next(),
                        Generators.letterStrings(8, 14).next());
                assertEquals(202, post(runtime, routes.turns(), req.toJson()).status());
                assertEquals(ChatStatus.REPLY, read(runtime, routes).state());

                broadcast(runtime, "scope.ended", Map.of(
                        "scope", Map.of("kind", "plan", "id", PLAN),
                        "outcome", "merged",
                        "pull_request_number", prNumber));

                var lines = allNoticeLines(read(runtime, routes));
                assertEquals(List.of("chat.line.pr_merged"), lines.stream().map(NarrativeLine::key).toList());
                assertEquals(prNumber, intArg(lines.getFirst().args().get("number")));
            }
        }

        @Test
        @DisplayName("native DecisionOpenedEvent writes decision_opened into plan chat")
        void nativeDecisionOpenedNarratesPlanChat() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                var envelope = new DecisionEnvelope("Q-" + Generators.integers(10, 99).next(), 1,
                        Generators.letterStrings(6, 24).next(), null, "plan " + PLAN,
                        DecisionScope.PLAN, "execute", "implement", "plan", Reversibility.REVERSIBLE,
                        Generators.letterStrings(6, 24).next(), null,
                        List.of(new DecisionPart("1", Generators.letterStrings(4, 16).next(), AnswerType.ONE_OPTION,
                                List.of(new DecisionOption("1", "Proceed", null, null, null, null)),
                                null, null, null, null, false, false)),
                        Instant.now(), false, null, null, null, "pm-mcp-core", PLAN);
                broadcast(runtime, DecisionOpenedEvent.EVENT_NAME, new DecisionOpenedEvent(envelope).toMap());

                var lines = allNoticeLines(read(runtime, routes));
                assertAll("native decision.opened narrative line",
                        () -> assertEquals(List.of("chat.line.decision_opened"),
                                lines.stream().map(NarrativeLine::key).toList()),
                        () -> assertEquals(envelope.id(), lines.getFirst().args().get("id")),
                        () -> assertEquals("yes", lines.getFirst().args().get("blocks")));
            }
        }

        @Test
        @DisplayName("actions/pause and plan.paused narrate pausing with point and then paused")
        void pausingAndPausedPins() throws Exception {
            var pausePlan = "fix-relay";
            var routes = ChatRoutes.plan(pausePlan);
            try (var runtime = ordered(CHAT_STATE)) {
                var pausedResp = post(runtime, "/api/v1/plans/" + pausePlan + "/actions/pause",
                        "{\"point\":\"phase\"}");
                assertEquals(200, pausedResp.status(), pausedResp.bodyText());

                broadcast(runtime, "plan.paused", Map.of("plan_id", pausePlan, "state", "paused", "channel", "cli"));
                var lines = allNoticeLines(read(runtime, routes));
                assertAll("pausing then paused lines",
                        () -> assertEquals(List.of("chat.line.plan_pausing", "chat.line.plan_paused"),
                                lines.stream().map(NarrativeLine::key).toList()),
                        () -> assertEquals("phase", lines.getFirst().args().get("point")));
            }
        }

        @Test
        @DisplayName("later plan replies carry pause act ref and no hard-coded H-1 hold ref")
        void laterReplyDropsHardCodedH1() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                for (var i = 0; i < 4; i++) {
                    send(runtime, routes);
                }
                var reply = read(runtime, routes).turns().stream()
                        .filter(turn -> turn.kind() == TurnKind.MESSAGE && !turn.isOperator()).toList().getLast();
                assertAll("later plan reply refs",
                        () -> assertFalse(reply.refs().stream().anyMatch(ref -> ref.kind() == RefKind.HOLD),
                                "hard-coded H-1 hold ref must be absent"),
                        () -> assertTrue(reply.refs().stream().anyMatch(ref -> ref.kind() == RefKind.ACT
                                && "pause".equals(ref.act()))));
            }
        }

        @Test
        @DisplayName("applied revision emits instance.changed with cause chat and single version_changed line")
        void appliedRevisionEmitsInstanceChanged() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                send(runtime, routes);
                send(runtime, routes);
                var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
                assertEquals(200, post(runtime, routes.accept(),
                        new ProposalAnswerRequest(proposal.turnId()).toJson()).status());

                try (var stream = openStream(runtime, "/api/v1/plans/" + PLAN + "/events")) {
                    var reader = stream.events();
                    broadcast(runtime, "task.ended", Map.of("plan_id", PLAN, "task_id", "TASK-4", "status", "done"));

                    var seenInstance = readUntilInstanceChanged(reader);
                    var lines = allNoticeLines(read(runtime, routes));
                    var versionLines = lines.stream()
                            .filter(line -> "chat.line.version_changed".equals(line.key())).toList();
                    assertAll("applied revision instance.changed and single narrative line",
                            () -> assertEquals(2, Objects.requireNonNull(JsonTree.integer(seenInstance,
                                    "instance_version")).intValue()),
                            () -> assertEquals("chat", seenInstance.get("cause")),
                            () -> assertInstanceOf(Map.class, seenInstance.get("origin")),
                            () -> assertEquals(1, versionLines.size()));
                }
            }
        }

        @Test
        @DisplayName("extending a notice turn between replies re-sends chat.turn.completed with same turn_id and accumulated lines")
        void noticeExtensionResendsTurnCompletedWithSameTurnId() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE);
                 var stream = openStream(runtime, "/api/v1/plans/" + PLAN + "/events")) {
                var reader = stream.events();
                open(runtime, routes);

                broadcast(runtime, "phase.changed", Map.of("plan_id", PLAN, "new_phase", "verify"));
                var firstNotice = readNextNoticeCompleted(reader);
                broadcast(runtime, "hold.opened", Map.of("plan_id", PLAN, "hold_id", "H-77", "reason", "ci"));
                var extendedNotice = readNextNoticeCompleted(reader);

                assertAll("extended notice reuses turn_id and accumulates lines",
                        () -> assertEquals(firstNotice.turnId(), extendedNotice.turnId()),
                        () -> assertEquals(1, firstNotice.lines().size()),
                        () -> assertEquals(List.of("chat.line.phase_changed", "chat.line.hold_opened"),
                                extendedNotice.lines().stream().map(NarrativeLine::key).toList()));
            }
        }
    }

    @Nested
    @DisplayName("Epic and entry narrative lines and knows context")
    class EpicAndEntryCompleteness {

        @Test
        @DisplayName("epic.row_changed and epic.auto_changed write row_* and auto_changed lines")
        void epicNarrativeLines() throws Exception {
            var routes = ChatRoutes.epic(EPIC);
            try (var runtime = ordered(CONTROL_EPIC_STATE)) {
                open(runtime, routes);

                broadcast(runtime, "epic.row_changed",
                        Map.of("epic_id", EPIC, "plan_id", "p-staged", "status", "staged"));
                broadcast(runtime, "epic.row_changed",
                        Map.of("epic_id", EPIC, "plan_id", "p-run", "status", "running"));
                broadcast(runtime, "epic.row_changed",
                        Map.of("epic_id", EPIC, "plan_id", "p-land", "status", "landed"));
                broadcast(runtime, "epic.row_changed",
                        Map.of("epic_id", EPIC, "plan_id", "p-fail", "status", "failed", "outcome", "aborted"));
                broadcast(runtime, "epic.row_changed",
                        Map.of("epic_id", EPIC, "plan_id", "p-cancel", "status", "cancelled"));
                broadcast(runtime, "epic.auto_changed",
                        Map.of("epic_id", EPIC, "auto_start", false));

                var lines = allNoticeLines(read(runtime, routes));
                assertTrue(lines.size() >= 6, "expected 6 appended epic lines, got " + lines);
                var appended = lines.subList(lines.size() - 6, lines.size());
                assertAll("epic narrative lines",
                        () -> assertEquals(List.of(
                                        "chat.line.row_staged",
                                        "chat.line.row_started",
                                        "chat.line.row_landed",
                                        "chat.line.row_failed",
                                        "chat.line.row_cancelled",
                                        "chat.line.auto_changed"),
                                appended.stream().map(NarrativeLine::key).toList()),
                        () -> assertEquals("aborted", appended.get(3).args().get("outcome")),
                        () -> assertEquals("off", appended.get(5).args().get("state")));
            }
        }

        @Test
        @DisplayName("source.added, source.removed, and source.flagged write source_* lines into entry chat")
        void entrySourceNarrativeLines() throws Exception {
            var entryId = "ent-running";
            var routes = ChatRoutes.entry(entryId);
            try (var runtime = ordered(CHAT_STATE)) {
                broadcast(runtime, "source.added",
                        Map.of("scope", Map.of("kind", "entry", "id", entryId), "kind", "issue"));
                broadcast(runtime, "source.removed",
                        Map.of("scope", Map.of("kind", "entry", "id", entryId), "source", "issue"));
                broadcast(runtime, "source.flagged",
                        Map.of("scope", Map.of("kind", "entry", "id", entryId), "source", "pull_request",
                                "reason", "untrusted_instructions"));

                var notices = noticesOf(read(runtime, routes));
                var lastLines = notices.getLast().lines();
                assertTrue(lastLines.size() >= 3, "expected 3 appended source lines, got " + lastLines);
                assertEquals(List.of("chat.line.source_added", "chat.line.source_removed", "chat.line.source_flagged"),
                        lastLines.subList(lastLines.size() - 3, lastLines.size()).stream()
                                .map(NarrativeLine::key).toList());
            }
        }

        @Test
        @DisplayName("opening an epic or entry chat computes knows beyond description")
        void knowsForEpicAndEntry() throws Exception {
            try (var runtime = ordered(CONTROL_EPIC_STATE)) {
                var epicChat = open(runtime, ChatRoutes.epic(EPIC));
                var knows = epicChat.context().knows();
                assertEquals(List.of("chat.knows.description", "chat.knows.rows", "chat.knows.items"),
                        knows.stream().map(MessageText::key).toList());
                assertAll("epic knows counts",
                        () -> assertTrue(intArg(knows.get(1).args().get("count")) > 0),
                        () -> assertTrue(intArg(knows.get(2).args().get("count")) >= 0));
            }
            try (var runtime = ordered(START_CHAT_STATE)) {
                var openedEntry = post(runtime, "/api/v1/projects/pm-mcp-clients/entries",
                        new EntryOpenRequest(EntryKind.PLAN, "Fresh entry for knows", null, null, null).toJson());
                assertEquals(201, openedEntry.status(), openedEntry.bodyText());
                var entryId = StartStateResource.fromJson(openedEntry.bodyText()).entry().entryId();
                assertEquals(200, post(runtime, "/api/v1/entries/" + entryId + "/links/add_issue", "{}").status());
                var entryChat = open(runtime, ChatRoutes.entry(entryId));
                var knows = entryChat.context().knows();
                assertEquals(List.of("chat.knows.description", "chat.knows.sources"),
                        knows.stream().map(MessageText::key).toList());
                assertEquals(1, intArg(knows.get(1).args().get("count")));
            }
        }
    }

    // ---- helpers ----

    private static Map<String, Object> readUntilInstanceChanged(SseReader reader) throws Exception {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    while (true) {
                        var seen = reader.next();
                        assertNotNull(seen, "SSE stream ended before instance.changed");
                        if ("instance.changed".equals(seen.event())) {
                            return Objects.requireNonNull(JsonTree.asObject(JsonTree.parse(seen.data())));
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out waiting for instance.changed event", e);
        }
    }

    private static ChatTurnCompletedEvent readNextNoticeCompleted(SseReader reader) throws Exception {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    while (true) {
                        var seen = reader.next();
                        assertNotNull(seen, "SSE stream ended before chat.turn.completed notice");
                        if (!ChatTurnCompletedEvent.EVENT_NAME.equals(seen.event())) {
                            continue;
                        }
                        var data = JsonTree.asObject(JsonTree.parse(seen.data()));
                        if (data != null && "notice".equals(JsonTree.string(data, "kind"))) {
                            var payload = new LinkedHashMap<>(data);
                            payload.remove("event");
                            payload.remove("at");
                            payload.remove("scope");
                            return ChatTurnCompletedEvent.fromMap(payload);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out waiting for notice chat.turn.completed", e);
        }
    }

    private static int intArg(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }

    private static List<NarrativeLine> allNoticeLines(ChatResource chat) {
        var result = new ArrayList<NarrativeLine>();
        for (var turn : noticesOf(chat)) {
            result.addAll(turn.lines());
        }
        return result;
    }

    private static List<ChatTurn> noticesOf(ChatResource chat) {
        return chat.turns().stream().filter(turn -> turn.kind() == TurnKind.NOTICE).toList();
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static ChatResource open(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertTrue(opened.status() == 200 || opened.status() == 201, opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
    }

    private static void send(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var req = new ChatTurnRequest(Generators.letterStrings(4, 30).next(),
                Generators.letterStrings(10, 14).next()).toJson();
        assertEquals(202, post(runtime, routes.turns(), req).status());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(routes.chat()).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static void broadcast(MockRuntime runtime, String event, Map<String, Object> data) throws IOException {
        var response = post(runtime, BROADCAST, JsonTree.write(Map.of("event", event, "data", data)));
        assertEquals(200, response.status(), response.bodyText());
    }

    private static HttpExchange openStream(MockRuntime runtime, String path) throws IOException {
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
