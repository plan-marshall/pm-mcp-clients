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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatProposalEvent;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * Verifies D4 revision-lifecycle invariants across all five outcomes ({@code decision}, {@code apply},
 * {@code refuse}, {@code lapse}, {@code chat-end}): exact SSE event sequence and relative ordering
 * ({@code plan.revision_queued}, {@code activity.changed}, {@code chat.proposal}, {@code chat.ended}),
 * overview plan rows matching at every step, {@code reject} remaining open after every refusal, and
 * non-plan subjects refusing revision proposals.
 */
@DisplayName("Revision event order, overview parity, and refusal handling")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatRevisionOrderTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String BROADCAST = "/mock/broadcast";
    private static final String PLAN_SESSION = "session-log";
    private static final String PROJECT_CORE = "/api/v1/projects/pm-mcp-core";
    private static final String PLAN_RELAY = "fix-relay";
    private static final String PROJECT_CLIENTS = "/api/v1/projects/pm-mcp-clients";
    private static final String EPIC_RELAY = "relay-hardening";

    @Nested
    @DisplayName("All five revision outcomes emit ordered SSE events and match the overview")
    class OutcomesAndOrder {

        @Test
        @DisplayName("decision accept queues revision, emits ordered events, closes chat, and updates overview")
        void decisionAcceptOrderAndOverview() throws Exception {
            var routes = ChatRoutes.planDecision(PLAN_RELAY, "Q-007");
            try (var runtime = ordered(CHAT_STATE);
                 var stream = openStream(runtime, planEventsPath(PLAN_RELAY))) {
                var reader = stream.events();
                var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
                var accepted = answer(runtime, routes.accept(), proposal);
                assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(accepted.proposal()).state(),
                        () -> "unexpected proposal state: " + accepted.proposal());

                var acceptEvents = collectUntilEventCount(reader, "activity.changed", 1);
                assertOrderedNames(acceptEvents,
                        event -> "chat.proposal".equals(event.name())
                                && "accepted".equals(JsonTree.string(event.data(), "state")),
                        event -> "plan.revision_queued".equals(event.name())
                                && event.data().get("queued_revision") instanceof Map<?, ?> rev
                                && Boolean.TRUE.equals(rev.get("chat_open")),
                        event -> "activity.changed".equals(event.name())
                                && PLAN_RELAY.equals(JsonTree.string(event.data(), "plan_id")));

                var openRow = epicPlanRow(runtime, PROJECT_CLIENTS, EPIC_RELAY, PLAN_RELAY);
                var queuedOpen = JsonTree.asObject(openRow.get("queued_revision"));
                assertAll("decision overview row after accept",
                        () -> assertEquals(ProposalState.ACCEPTED,
                                Objects.requireNonNull(accepted.proposal()).state()),
                        () -> assertNotNull(queuedOpen),
                        () -> assertEquals(Boolean.TRUE, Objects.requireNonNull(queuedOpen).get("chat_open")));

                broadcast(runtime, "decision.answered",
                        Map.of("id", "Q-007", "channel", "web", "device", "dev-1"));
                var closeEvents = collectUntilEvent(reader,
                        e -> "activity.changed".equals(e.name())
                                && PLAN_RELAY.equals(JsonTree.string(e.data(), "plan_id")));
                assertOrderedNames(closeEvents,
                        event -> "chat.ended".equals(event.name())
                                && "answered_elsewhere".equals(JsonTree.string(event.data(), "ended_by")),
                        event -> "plan.revision_queued".equals(event.name())
                                && event.data().get("queued_revision") instanceof Map<?, ?> rev
                                && Boolean.FALSE.equals(rev.get("chat_open")),
                        event -> "activity.changed".equals(event.name())
                                && PLAN_RELAY.equals(JsonTree.string(event.data(), "plan_id")));

                var closedRow = epicPlanRow(runtime, PROJECT_CLIENTS, EPIC_RELAY, PLAN_RELAY);
                var queuedClosed = JsonTree.asObject(closedRow.get("queued_revision"));
                assertAll("decision overview row after decision answered",
                        () -> assertNotNull(queuedClosed),
                        () -> assertEquals(Boolean.FALSE, Objects.requireNonNull(queuedClosed).get("chat_open")));
            }
        }

        @Test
        @DisplayName("apply at task boundary clears revision_queued, emits activity.changed, then chat.proposal applied")
        void applyOrderAndOverview() throws Exception {
            var routes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(CHAT_STATE)) {
                var proposal = producePlanProposal(runtime, routes);
                try (var stream = openStream(runtime, planEventsPath(PLAN_SESSION))) {
                    var reader = stream.events();
                    answer(runtime, routes.accept(), proposal);
                    var queuedEvents = collectUntilEventCount(reader, "activity.changed", 1);
                    assertOrderedNames(queuedEvents,
                            e -> "chat.proposal".equals(e.name())
                                    && "accepted".equals(JsonTree.string(e.data(), "state")),
                            e -> "plan.revision_queued".equals(e.name()) && e.data().get("queued_revision") != null,
                            e -> "activity.changed".equals(e.name())
                                    && PLAN_SESSION.equals(JsonTree.string(e.data(), "plan_id")));
                    var queuedRow = planRow(runtime, PROJECT_CORE, PLAN_SESSION);
                    assertNotNull(queuedRow.get("queued_revision"), "overview row carries queued_revision while open");

                    broadcast(runtime, "task.ended",
                            Map.of("plan_id", PLAN_SESSION, "task_id", "TASK-4", "status", "done"));
                    var applyEvents = collectUntilEvent(reader,
                            e -> "chat.proposal".equals(e.name())
                                    && "applied".equals(JsonTree.string(e.data(), "state")));
                    assertOrderedNames(applyEvents,
                            e -> "plan.revision_queued".equals(e.name()) && e.data().get("queued_revision") == null,
                            e -> "activity.changed".equals(e.name())
                                    && PLAN_SESSION.equals(JsonTree.string(e.data(), "plan_id")),
                            e -> "chat.proposal".equals(e.name())
                                    && "applied".equals(JsonTree.string(e.data(), "state")));

                    assertNull(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"),
                            "overview row clears queued_revision after apply");
                }
            }
        }

        @Test
        @DisplayName("refuse at boundary clears revision_queued, emits activity.changed, refuses proposal, and keeps reject")
        void refuseOrderOverviewAndReject() throws Exception {
            var routes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(CHAT_STATE)) {
                var proposal = producePlanProposal(runtime, routes);
                try (var stream = openStream(runtime, planEventsPath(PLAN_SESSION))) {
                    var reader = stream.events();
                    answer(runtime, routes.accept(), proposal);
                    collectUntilEventCount(reader, "activity.changed", 1);

                    broadcast(runtime, "instance.changed",
                            Map.of("plan_id", PLAN_SESSION, "instance_version", 99, "cause", "replan"));
                    broadcast(runtime, "task.ended",
                            Map.of("plan_id", PLAN_SESSION, "task_id", "TASK-4", "status", "done"));

                    var refuseEvents = collectUntilEvent(reader,
                            e -> "chat.proposal".equals(e.name())
                                    && "refused".equals(JsonTree.string(e.data(), "state")));
                    assertOrderedNames(refuseEvents,
                            e -> "plan.revision_queued".equals(e.name()) && e.data().get("queued_revision") == null,
                            e -> "activity.changed".equals(e.name())
                                    && PLAN_SESSION.equals(JsonTree.string(e.data(), "plan_id")),
                            e -> "chat.proposal".equals(e.name())
                                    && "refused".equals(JsonTree.string(e.data(), "state")));

                    assertNull(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"),
                            "overview row clears queued_revision on refuse");

                    var rejected = answer(runtime, routes.reject(), proposal);
                    assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
                }
            }
        }

        @Test
        @DisplayName("lapse on scope.ended clears revision_queued, emits activity.changed, lapses proposal, and updates overview")
        void lapseOrderAndOverview() throws Exception {
            var routes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(CHAT_STATE)) {
                var proposal = producePlanProposal(runtime, routes);
                try (var stream = openStream(runtime, planEventsPath(PLAN_SESSION))) {
                    var reader = stream.events();
                    answer(runtime, routes.accept(), proposal);
                    collectUntilEventCount(reader, "activity.changed", 1);
                    assertNotNull(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"));

                    broadcast(runtime, "scope.ended",
                            Map.of("scope", Map.of("kind", "plan", "id", PLAN_SESSION), "outcome", "done"));
                    var lapseEvents = collectUntilEvent(reader,
                            e -> "chat.proposal".equals(e.name())
                                    && "lapsed".equals(JsonTree.string(e.data(), "state")));
                    assertOrderedNames(lapseEvents,
                            e -> "chat.ended".equals(e.name())
                                    && "subject_ended".equals(JsonTree.string(e.data(), "ended_by")),
                            e -> "plan.revision_queued".equals(e.name()) && e.data().get("queued_revision") == null,
                            e -> "activity.changed".equals(e.name())
                                    && PLAN_SESSION.equals(JsonTree.string(e.data(), "plan_id")),
                            e -> "chat.proposal".equals(e.name())
                                    && "lapsed".equals(JsonTree.string(e.data(), "state")));

                    assertNull(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"),
                            "overview row clears queued_revision on lapse");
                }
            }
        }

        @Test
        @DisplayName("chat-end clears chat_open on revision_queued, emits activity.changed, and updates overview")
        void chatEndOrderAndOverview() throws Exception {
            var routes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(CHAT_STATE)) {
                var proposal = producePlanProposal(runtime, routes);
                try (var stream = openStream(runtime, planEventsPath(PLAN_SESSION))) {
                    var reader = stream.events();
                    answer(runtime, routes.accept(), proposal);
                    collectUntilEventCount(reader, "activity.changed", 1);

                    assertEquals(200, post(runtime, routes.end(), "{}").status());
                    var endEvents = collectUntilEvent(reader,
                            e -> "activity.changed".equals(e.name())
                                    && PLAN_SESSION.equals(JsonTree.string(e.data(), "plan_id")));
                    assertOrderedNames(endEvents,
                            e -> "chat.ended".equals(e.name())
                                    && "operator".equals(JsonTree.string(e.data(), "ended_by")),
                            e -> "plan.revision_queued".equals(e.name())
                                    && e.data().get("queued_revision") instanceof Map<?, ?> rev
                                    && Boolean.FALSE.equals(rev.get("chat_open")),
                            e -> "activity.changed".equals(e.name())
                                    && PLAN_SESSION.equals(JsonTree.string(e.data(), "plan_id")));

                    var queued = JsonTree.asObject(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"));
                    assertAll("overview reflects chat_open=false after chat-end",
                            () -> assertNotNull(queued),
                            () -> assertEquals(Boolean.FALSE, Objects.requireNonNull(queued).get("chat_open")));
                }
            }
        }

        @Test
        @DisplayName("scoped task.ended records the ended task and refuses a late revision for that boundary")
        void scopedTaskEndedRefusesLateRevision() throws Exception {
            var routes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(CHAT_STATE)) {
                var proposal = producePlanProposal(runtime, routes);
                broadcast(runtime, "task.ended", Map.of(
                        "scope", Map.of("kind", "plan", "id", PLAN_SESSION),
                        "task_id", "TASK-4",
                        "status", "done"));

                var refused = answer(runtime, routes.accept(), proposal);

                assertEquals(ProposalState.REFUSED, Objects.requireNonNull(refused.proposal()).state());
                assertNull(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"));
                var rejected = answer(runtime, routes.reject(), proposal);
                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
            }
        }
    }

    @Nested
    @DisplayName("Reject remains open after every refusal path")
    class RejectStillTaken {

        @Test
        @DisplayName("already_queued conflict keeps proposal open so reject still succeeds")
        void rejectStillTakenAfterAlreadyQueuedConflict() throws Exception {
            var decisionRoutes = ChatRoutes.planDecision(PLAN_SESSION, "Q-902");
            var planRoutes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(withSeededRevisionChat("decision", PLAN_SESSION + "/Q-902", "decision",
                         "replan"))) {
                var planProposal = producePlanProposal(runtime, planRoutes);
                answer(runtime, planRoutes.accept(), planProposal);

                var decisionProposal = Objects.requireNonNull(read(runtime, decisionRoutes).proposal());
                var conflict = post(runtime, decisionRoutes.accept(),
                        new ProposalAnswerRequest(decisionProposal.turnId()).toJson());
                assertEquals(409, conflict.status(), conflict.bodyText());

                var rejected = answer(runtime, decisionRoutes.reject(), decisionProposal);
                assertAll("reject taken after already_queued refusal",
                        () -> assertEquals(ProposalState.REJECTED,
                                Objects.requireNonNull(rejected.proposal()).state()),
                        () -> assertEquals(ChatStatus.TURN, rejected.state()));
            }
        }

        @Test
        @DisplayName("changed_since conflict keeps proposal open so reject still succeeds")
        void rejectStillTakenAfterChangedSinceConflict() throws Exception {
            var routes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(CHAT_STATE)) {
                var proposal = producePlanProposal(runtime, routes);
                broadcast(runtime, "instance.changed",
                        Map.of("plan_id", PLAN_SESSION, "instance_version", 2, "cause", "replan"));

                var conflict = post(runtime, routes.accept(),
                        new ProposalAnswerRequest(proposal.turnId()).toJson());
                assertEquals(409, conflict.status(), conflict.bodyText());
                assertNull(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"));

                var rejected = answer(runtime, routes.reject(), proposal);
                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
            }
        }

        @Test
        @DisplayName("broadcast conflict keeps existing queued revision untouched and reject still succeeds")
        void rejectStillTakenAfterBroadcastConflict() throws Exception {
            var decisionRoutes = ChatRoutes.planDecision(PLAN_SESSION, "Q-903");
            var planRoutes = ChatRoutes.plan(PLAN_SESSION);
            try (var runtime = ordered(withSeededRevisionChat("decision", PLAN_SESSION + "/Q-903", "decision",
                         "replan"))) {
                var planProposal = producePlanProposal(runtime, planRoutes);
                answer(runtime, planRoutes.accept(), planProposal);

                var decisionChat = read(runtime, decisionRoutes);
                var decisionProposal = Objects.requireNonNull(decisionChat.proposal());
                var conflict = post(runtime, BROADCAST, JsonTree.write(Map.of("event", ChatProposalEvent.EVENT_NAME,
                        "data", new ChatProposalEvent(decisionChat.id(), decisionProposal.turnId(),
                        decisionProposal.replyTurnId(), ProposalKind.REVISION, ProposalState.ACCEPTED, null)
                        .toMap())));
                assertEquals(409, conflict.status(), conflict.bodyText());

                var afterBroadcast = read(runtime, decisionRoutes);
                var queued = JsonTree.asObject(planRow(runtime, PROJECT_CORE, PLAN_SESSION).get("queued_revision"));
                assertAll("broadcast conflict keeps proposal open and existing revision queued",
                        () -> assertEquals(ProposalState.OPEN,
                                Objects.requireNonNull(afterBroadcast.proposal()).state()),
                        () -> assertEquals(planProposal.turnId(),
                                JsonTree.string(Objects.requireNonNull(queued), "turn_id")));

                var rejected = answer(runtime, decisionRoutes.reject(), decisionProposal);
                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
            }
        }
    }

    @Nested
    @DisplayName("A revision proposal on a non-plan subject is refused and still accepts reject")
    class AnotherSubject {

        @Test
        @DisplayName("epic, entry, and epic decision revision proposals are refused without queuing and accept reject")
        void nonPlanSubjectsRefuseRevisionAndKeepReject() throws Exception {
            var scenario = withSeededRevisions(List.of(
                    new SeededTarget("C-9201", "epic", "epic-order-test", "epic", "decompose"),
                    new SeededTarget("C-9202", "entry", "ent-order-test", "entry", "refine"),
                    new SeededTarget("C-9203", "decision", "epic-order-test/Q-991", "decision", "decompose")));
            var targets = List.of(
                    ChatRoutes.epic("epic-order-test"),
                    ChatRoutes.entry("ent-order-test"),
                    ChatRoutes.epicDecision("epic-order-test", "Q-991"));

            try (var runtime = ordered(scenario)) {
                for (var routes : targets) {
                    var initial = read(runtime, routes);
                    var proposal = Objects.requireNonNull(initial.proposal());
                    var refused = answer(runtime, routes.accept(), proposal);
                    var refusedProposal = Objects.requireNonNull(refused.proposal());
                    assertAll(routes.chat(),
                            () -> assertEquals(ProposalState.REFUSED, refusedProposal.state()),
                            () -> assertNotNull(refusedProposal.finding()),
                            () -> assertEquals(409, post(runtime, routes.accept(),
                                    new ProposalAnswerRequest(proposal.turnId()).toJson()).status()));

                    var rejected = answer(runtime, routes.reject(), proposal);
                    assertAll(routes.reject(),
                            () -> assertEquals(ProposalState.REJECTED,
                                    Objects.requireNonNull(rejected.proposal()).state()),
                            () -> assertNull(Objects.requireNonNull(rejected.proposal()).finding()),
                            () -> assertEquals(ChatStatus.TURN, rejected.state()));
                }
            }
        }
    }

    // ---- helpers ----

    private static ChatProposal producePlanProposal(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertTrue(opened.status() == 200 || opened.status() == 201, opened.bodyText());
        for (var i = 0; i < 2; i++) {
            var req = new ChatTurnRequest(Generators.letterStrings(4, 30).next(),
                    Generators.letterStrings(10, 14).next()).toJson();
            assertEquals(202, post(runtime, routes.turns(), req).status());
        }
        return Objects.requireNonNull(read(runtime, routes).proposal());
    }

    private static List<RecordedEvent> collectUntilEvent(SseReader reader, Predicate<RecordedEvent> stop)
            throws Exception {
        var collected = Collections.synchronizedList(new ArrayList<RecordedEvent>());
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        var next = reader.next();
                        assertNotNull(next, "SSE stream ended unexpectedly; collected=" + collected);
                        var parsed = JsonTree.asObject(JsonTree.parse(next.data()));
                        var recorded = new RecordedEvent(Objects.requireNonNull(next.event()),
                                parsed == null ? Map.of() : parsed);
                        collected.add(recorded);
                        if (stop.test(recorded)) {
                            return;
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out collecting events; seen="
                    + collected.stream().map(RecordedEvent::name).toList(), e);
        }
        return List.copyOf(collected);
    }

    private static List<RecordedEvent> collectUntilEventCount(SseReader reader, String name, int targetCount)
            throws Exception {
        var collected = Collections.synchronizedList(new ArrayList<RecordedEvent>());
        var count = new int[1];
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    while (count[0] < targetCount) {
                        var next = reader.next();
                        assertNotNull(next, "SSE stream ended before " + name);
                        var parsed = JsonTree.asObject(JsonTree.parse(next.data()));
                        var recorded = new RecordedEvent(Objects.requireNonNull(next.event()),
                                parsed == null ? Map.of() : parsed);
                        collected.add(recorded);
                        if (name.equals(recorded.name())) {
                            count[0]++;
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out waiting for " + targetCount + "x " + name + "; seen="
                    + collected.stream().map(RecordedEvent::name).toList(), e);
        }
        return List.copyOf(collected);
    }

    private static final Set<String> LIFECYCLE_EVENTS = Set.of("chat.proposal", "chat.ended",
            "plan.revision_queued", "activity.changed");

    @SafeVarargs
    private static void assertOrderedNames(List<RecordedEvent> events, Predicate<RecordedEvent>... matchers) {
        var lifecycle = events.stream().filter(event -> LIFECYCLE_EVENTS.contains(event.name())).toList();
        assertEquals(matchers.length, lifecycle.size(),
                () -> "unexpected lifecycle event sequence: "
                        + lifecycle.stream().map(RecordedEvent::name).toList());
        for (var i = 0; i < matchers.length; i++) {
            final var index = i;
            var candidate = lifecycle.get(index);
            assertTrue(matchers[index].test(candidate),
                    () -> "lifecycle event #" + index + " mismatch in "
                            + lifecycle.stream().map(RecordedEvent::name).toList());
        }
    }

    private static Map<String, Object> planRow(MockRuntime runtime, String projectPath, String planId)
            throws Exception {
        var response = get(runtime, projectPath);
        assertEquals(200, response.status(), response.bodyText());
        var body = Objects.requireNonNull(JsonTree.asObject(JsonTree.parse(response.body())));
        if (body.get("plans") instanceof List<?> plans) {
            for (var item : plans) {
                var row = JsonTree.asObject(item);
                if (row != null && (planId.equals(JsonTree.string(row, "plan_id"))
                        || planId.equals(JsonTree.string(row, "id")))) {
                    return row;
                }
            }
        }
        throw new AssertionError("plan row not found: " + planId + " in " + projectPath);
    }

    private static Map<String, Object> epicPlanRow(MockRuntime runtime, String projectPath, String epicId,
            String planId) throws Exception {
        var response = get(runtime, projectPath);
        assertEquals(200, response.status(), response.bodyText());
        var body = Objects.requireNonNull(JsonTree.asObject(JsonTree.parse(response.body())));
        if (body.get("epics") instanceof List<?> epics) {
            for (var epicObj : epics) {
                var epic = JsonTree.asObject(epicObj);
                if (epic != null && (epicId.equals(JsonTree.string(epic, "epic_id"))
                        || epicId.equals(JsonTree.string(epic, "id")))
                        && epic.get("plans") instanceof List<?> plans) {
                    for (var planObj : plans) {
                        var plan = JsonTree.asObject(planObj);
                        if (plan != null && (planId.equals(JsonTree.string(plan, "plan_id"))
                                || planId.equals(JsonTree.string(plan, "id")))) {
                            return plan;
                        }
                    }
                }
            }
        }
        throw new AssertionError("epic plan row not found: " + epicId + "/" + planId);
    }

    private static Scenario withSeededRevisionChat(String kind, String subjectId, String scope, String role) {
        return withSeededRevisions(List.of(new SeededTarget("C-9200", kind, subjectId, scope, role)));
    }

    @SuppressWarnings("unchecked")
    private static Scenario withSeededRevisions(List<SeededTarget> targets) {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copy(state.representation());
        var control = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) representation
                .get("facts")).get("runtime")).get("control");
        var chats = (List<Object>) control.get("chats");
        var index = 1;
        for (var target : targets) {
            chats.add(revisionChat(target, index++));
        }
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    private static Map<String, Object> revisionChat(SeededTarget target, int offset) {
        var turnPrefix = "T-92" + offset;
        var chat = new LinkedHashMap<String, Object>();
        chat.put("id", target.chatId());
        chat.put("subject", Map.of("kind", target.kind(), "id", target.subjectId()));
        chat.put("role", Map.of("name", target.role(), "model", "sonnet"));
        chat.put("scope", target.scope());
        chat.put("state", "chat.read-back");
        chat.put("context", Map.of("knows", List.of(), "as_of", "2026-10-14T10:00:00Z"));
        var proposal = new LinkedHashMap<String, Object>();
        proposal.put("turn_id", turnPrefix + "3");
        proposal.put("reply_turn_id", turnPrefix + "2");
        proposal.put("kind", "revision");
        proposal.put("summary", "seeded revision proposal");
        proposal.put("applies", Map.of("key", "chat.applies.after_task", "args", Map.of("task_id", "TASK-4")));
        proposal.put("before", "1 task");
        proposal.put("after", "2 tasks");
        proposal.put("state", "open");
        chat.put("proposal", proposal);
        chat.put("turns", List.of(
                turn(turnPrefix + "1", "message", "operator", "Please propose a revision"),
                turn(turnPrefix + "2", "message", target.role(), "Proposed a revision."),
                turn(turnPrefix + "3", "proposal", target.role(), "seeded revision proposal")));
        return chat;
    }

    private static Map<String, Object> turn(String turnId, String kind, String author, String text) {
        var turn = new LinkedHashMap<String, Object>();
        turn.put("turn_id", turnId);
        turn.put("kind", kind);
        turn.put("author", author);
        turn.put("status", "complete");
        turn.put("format", "plain");
        turn.put("untrusted", false);
        turn.put("text", text);
        turn.put("steps", List.of());
        turn.put("refs", List.of());
        turn.put("at", "2026-10-14T10:00:00Z");
        return turn;
    }

    private static Object copy(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((key, member) -> copy.put(String.valueOf(key), copy(member)));
            return copy;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(ChatRevisionOrderTest::copy).toList());
        }
        return value;
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static MockRuntime ordered(Scenario scenario) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().initialScenario(scenario).timing(MockTiming.ORDERED)
                .build());
    }

    private static String planEventsPath(String planId) {
        return "/api/v1/plans/" + planId + "/events";
    }

    private static HttpExchange openStream(MockRuntime runtime, String path) throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream(), path);
        return exchange;
    }

    private static ChatResource answer(MockRuntime runtime, String path, ChatProposal proposal) throws Exception {
        var response = post(runtime, path, new ProposalAnswerRequest(proposal.turnId()).toJson());
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = get(runtime, routes.chat());
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static void broadcast(MockRuntime runtime, String event, Map<String, Object> data) throws IOException {
        var response = new UnixSocketHttpClient(runtime.socketPath()).send(HttpRequest.json("POST", BROADCAST,
                JsonTree.write(Map.of("event", event, "data", data)))
                .withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private record RecordedEvent(String name, Map<String, Object> data) {
    }

    private record SeededTarget(String chatId, String kind, String subjectId, String scope, String role) {
    }
}
