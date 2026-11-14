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


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.PlanRevisionQueuedEvent;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.QueuedRevision;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnStatus;
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
 * The queued revision of a plan's chat ({@code state-catalogue.adoc#chat-proposal-states}): accepting a
 * revision while a task runs queues it as the plan's {@code queued_revision}, announced by
 * {@code plan.revision_queued}, carried by the plan's resource and its overview row; at the task
 * boundary it is applied — the plan's version moves on, the change names the chat and the turn it came
 * from as its origin — and it lapses where the plan leaves the revision window first. An accept made
 * against an older version of the plan, or while another revision of that plan is queued, is refused
 * {@code conflict}.
 */
@DisplayName("ChatRevisionQueue")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatRevisionQueueTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String PLAN = "session-log";
    private static final String PROJECT = "pm-mcp-core";
    private static final String PROCESS = "/api/v1/plans/" + PLAN + "/process";
    private static final String PROJECT_RESOURCE = "/api/v1/projects/" + PROJECT;
    private static final String BROADCAST = "/mock/broadcast";
    private static final String PLAN_ID = "plan_id";
    private static final String TASK_ID = "task_id";
    private static final String STATUS = "status";
    private static final String KEY_PHASE = "phase";
    private static final String QUEUED_REVISION = "queued_revision";
    private static final String INSTANCE_VERSION = "instance_version";
    private static final String CONFLICT = "conflict";
    private static final String DECISION = "Q-9";
    private static final String DECISION_SUBJECT = PLAN + "/" + DECISION;

    @Nested
    @DisplayName("Accepting a revision")
    class Accept {

        @Test
        @DisplayName("queues it as the plan's queued_revision, on the plan, its row and the events")
        void queues() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                var proposal = propose(runtime, routes);
                // The stream is read as the accept's events flow: a stream nobody reads fills and
                // stalls the next broadcast.
                try (var stream = openStream(runtime, "/api/v1/plans/" + PLAN + "/events")) {
                    var accepted = answer(runtime, routes.accept(), proposal);

                    assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(accepted.proposal()).state());
                    var queued = queuedRevision(runtime);
                    var row = overviewRow(runtime);
                    assertAll("the queued revision",
                            () -> assertNotNull(queued, "the plan carries its queued revision"),
                            () -> assertEquals(accepted.id(), queued.chatId()),
                            () -> assertEquals(proposal.turnId(), queued.turnId()),
                            () -> assertEquals(new ChatSubject(ChatSubjectKind.PLAN, PLAN), queued.subject()),
                            () -> assertTrue(queued.chatOpen(), "the chat that proposed it is open"),
                            () -> assertEquals("chat.applies.after_task", queued.applies().key()),
                            () -> assertEquals("TASK-4", queued.applies().args().get(TASK_ID)),
                            () -> assertNotNull(row.get(QUEUED_REVISION), "the overview's plan row carries it"));
                    var announced = planRevisionQueued(stream.events(), proposal.turnId());
                    assertEquals(proposal.turnId(), Objects.requireNonNull(announced.queuedRevision()).turnId());
                    assertActivityChanged(stream.events());
                }
            }
        }

        @Test
        @DisplayName("refuses it at the boundary where the plan moved on meanwhile")
        void refusesAtTheBoundary() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                var proposal = propose(runtime, routes);
                answer(runtime, routes.accept(), proposal);
                // The plan takes a version of its own before the boundary: the queued revision no
                // longer applies against the plan as it is then.
                broadcast(runtime, "instance.changed", Map.of(PLAN_ID, PLAN, INSTANCE_VERSION, 2));

                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-4", STATUS, "done"));

                assertAll("the refused revision",
                        () -> assertEquals(ProposalState.REFUSED,
                                Objects.requireNonNull(read(runtime, routes).proposal()).state()),
                        () -> assertEquals("chat.finding.check_failed",
                                Objects.requireNonNull(read(runtime, routes).proposal()).finding().key()),
                        () -> assertNull(process(runtime).get(QUEUED_REVISION), "the queue is empty"));
            }
        }

        @Test
        @DisplayName("applies it at the task boundary: the version moves and the change names its origin")
        void appliesAtTheBoundary() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                var proposal = propose(runtime, routes);
                var chat = answer(runtime, routes.accept(), proposal);
                assertEquals(1, version(runtime), "the version has not moved yet");

                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-4", STATUS, "done"));

                var applied = read(runtime, routes);
                var plan = process(runtime);
                assertAll("the applied revision",
                        () -> assertEquals(ProposalState.APPLIED,
                                Objects.requireNonNull(applied.proposal()).state()),
                        () -> assertEquals(2, JsonTree.integer(plan, INSTANCE_VERSION)),
                        () -> assertNull(plan.get(QUEUED_REVISION), "the queue is empty"),
                        () -> assertEquals("chat", JsonTree.string(JsonTree.object(plan, "origin"), "kind")),
                        () -> assertEquals(chat.id(), JsonTree.string(JsonTree.object(plan, "origin"), "chat_id")),
                        () -> assertEquals(proposal.turnId(),
                                JsonTree.string(JsonTree.object(plan, "origin"), "turn_id")));
            }
        }

        @Test
        @DisplayName("turns chat_open false when the chat that proposed it ends while it is queued")
        void chatEndingWhileQueued() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                var proposal = propose(runtime, routes);
                answer(runtime, routes.accept(), proposal);
                assertTrue(Objects.requireNonNull(queuedRevision(runtime)).chatOpen());

                assertEquals(200, post(runtime, routes.end(), "").status());

                var queued = Objects.requireNonNull(queuedRevision(runtime));
                assertAll("the chat's end is announced",
                        () -> assertFalse(queued.chatOpen(), "the proposing chat has ended"),
                        () -> assertEquals(ChatStatus.END, read(runtime, routes).state()));
            }
        }

        @Test
        @DisplayName("lapses a proposal whose plan left the revision window first")
        void lapsesWhenTheWindowCloses() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                open(runtime, routes);
                propose(runtime, routes);

                broadcast(runtime, "phase.changed", Map.of(PLAN_ID, PLAN, KEY_PHASE, "refine"));

                assertAll("the proposal lapsed",
                        () -> assertEquals(ProposalState.LAPSED,
                                Objects.requireNonNull(read(runtime, routes).proposal()).state()),
                        () -> assertNull(process(runtime).get(QUEUED_REVISION), "nothing was queued"));
            }
        }
    }

    @Nested
    @DisplayName("A revision from a decision's chat")
    class FromADecision {

        @Test
        @DisplayName("names the decision as its subject and turns chat_open false when the decision is answered")
        void decisionSubject() throws Exception {
            var routes = ChatRoutes.planDecision(PLAN, DECISION);
            try (var runtime = ordered(decisionScenario())) {
                var chat = read(runtime, routes);
                var proposal = Objects.requireNonNull(chat.proposal(), "the seeded proposal");
                assertEquals(ProposalKind.REVISION, proposal.kind());

                answer(runtime, routes.accept(), proposal);

                var queued = Objects.requireNonNull(queuedRevision(runtime));
                assertAll("the decision's revision",
                        () -> assertEquals(new ChatSubject(ChatSubjectKind.DECISION, DECISION_SUBJECT),
                                queued.subject()),
                        () -> assertTrue(queued.chatOpen()),
                        () -> assertEquals(ChatStatus.TURN, read(runtime, routes).state(),
                                "the accepted proposal leaves the read-back"));

                broadcast(runtime, "decision.answered", Map.of("id", DECISION, "channel", "web", "device", "dev-1"));

                assertAll("the answered decision ends its chat",
                        () -> assertEquals(ChatStatus.END, read(runtime, routes).state()),
                        () -> assertFalse(Objects.requireNonNull(queuedRevision(runtime)).chatOpen()));
            }
        }

        @Test
        @DisplayName("refuses the accept while another revision is queued, and the older version after it applies")
        void refusesAQueuedOneAndAnOlderVersion() throws Exception {
            var planRoutes = ChatRoutes.plan(PLAN);
            var decisionRoutes = ChatRoutes.planDecision(PLAN, DECISION);
            try (var runtime = ordered(decisionScenario())) {
                open(runtime, planRoutes);
                var planProposal = propose(runtime, planRoutes);
                var decisionProposal = Objects.requireNonNull(read(runtime, decisionRoutes).proposal());

                answer(runtime, planRoutes.accept(), planProposal);

                var whileQueued = post(runtime, decisionRoutes.accept(),
                        new ProposalAnswerRequest(decisionProposal.turnId()).toJson());
                assertEquals(409, whileQueued.status(), whileQueued.bodyText());
                assertEquals(CONFLICT,
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(whileQueued.body())), "error"));
                assertEquals("chat.finding.already_queued",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(whileQueued.body())), "message"));

                broadcast(runtime, "task.ended", Map.of(PLAN_ID, PLAN, TASK_ID, "TASK-4", STATUS, "done"));
                assertEquals(2, version(runtime), "the queued revision applied");

                var older = post(runtime, decisionRoutes.accept(),
                        new ProposalAnswerRequest(decisionProposal.turnId()).toJson());
                assertEquals(409, older.status(), older.bodyText());
                assertEquals("chat.finding.changed_since",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(older.body())), "message"));
                assertEquals(ProposalState.OPEN,
                        Objects.requireNonNull(read(runtime, decisionRoutes).proposal()).state(),
                        "a refused accept leaves the proposal open, so its reject is taken");

                var rejected = post(runtime, decisionRoutes.reject(),
                        new ProposalAnswerRequest(decisionProposal.turnId()).toJson());
                assertEquals(200, rejected.status(), rejected.bodyText());
                assertEquals(ProposalState.REJECTED,
                        Objects.requireNonNull(ChatResource.fromJson(rejected.bodyText()).proposal()).state());
            }
        }

        @Test
        @DisplayName("lapses the waiting proposal where the plan leaves the window, which wins over the conflict")
        void lapsedWinsWhereBothHold() throws Exception {
            var planRoutes = ChatRoutes.plan(PLAN);
            var decisionRoutes = ChatRoutes.planDecision(PLAN, DECISION);
            try (var runtime = ordered(decisionScenario())) {
                open(runtime, planRoutes);
                var planProposal = propose(runtime, planRoutes);
                answer(runtime, planRoutes.accept(), planProposal);

                broadcast(runtime, "phase.changed", Map.of(PLAN_ID, PLAN, KEY_PHASE, "refine"));

                assertAll("the window closed before the conflict",
                        () -> assertEquals(ProposalState.LAPSED,
                                Objects.requireNonNull(read(runtime, decisionRoutes).proposal()).state()),
                        () -> assertNull(process(runtime).get(QUEUED_REVISION), "the queue is empty"),
                        () -> assertEquals(ProposalState.LAPSED,
                                Objects.requireNonNull(read(runtime, planRoutes).proposal()).state()));
            }
        }
    }

    @Nested
    @DisplayName("A reply's act reference")
    class ActReference {

        @Test
        @DisplayName("is refused invalid_arguments where the subject offers no such act")
        void refusesAnActTheSubjectDoesNotOffer() throws Exception {
            var routes = ChatRoutes.plan(PLAN);
            try (var runtime = ordered(CHAT_STATE)) {
                var chat = open(runtime, routes);

                // `archive` is an epic's act: a running plan never offers it, `pause` it does.
                var refused = broadcast(runtime, completedEvent(chat.id(), "archive"));
                var taken = broadcast(runtime, completedEvent(chat.id(), "pause"));

                assertAll("the act check",
                        () -> assertEquals(400, refused.status(), refused.bodyText()),
                        () -> assertEquals("invalid_arguments",
                                JsonTree.string(JsonTree.asObject(JsonTree.parse(refused.body())), "error")),
                        () -> assertEquals(200, taken.status(), taken.bodyText()));
            }
        }

        private static String completedEvent(String chatId, String act) {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("chat_id", chatId);
            payload.put("turn_id", "T-" + Generators.letterStrings(4, 8).next());
            payload.put("kind", TurnKind.MESSAGE.wireValue());
            payload.put("status", TurnStatus.COMPLETE.wireValue());
            payload.put("untrusted", false);
            payload.put("text", "");
            payload.put("refs", List.of(Map.of("kind", "act", "act", act, "subject",
                    Map.of("kind", "plan", "id", PLAN))));
            return JsonTree.write(Map.of("event", ChatTurnCompletedEvent.EVENT_NAME, "data", payload));
        }
    }

    /**
     * The chat state with the plan's decision chat of {@code Q-9} added, its open proposal a revision
     * of the plan made at the version the plan is at.
     */
    @SuppressWarnings("unchecked")
    private static Scenario decisionScenario() {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copy(state.representation());
        var control = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) representation
                .get("facts")).get("runtime")).get("control");
        var chats = (List<Object>) control.get("chats");
        chats.add(decisionChat());
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    private static Map<String, Object> decisionChat() {
        var chat = new LinkedHashMap<String, Object>();
        chat.put("id", "C-9001");
        chat.put("subject", Map.of("kind", "decision", "id", DECISION_SUBJECT));
        chat.put("role", Map.of("name", "replan", "model", "sonnet"));
        chat.put("scope", "decision");
        chat.put("state", "chat.read-back");
        chat.put("context", Map.of("knows", List.of(Map.of("key", "chat.knows.decision",
                "args", Map.of("id", DECISION))), "as_of", "2026-10-14T10:00:00Z"));
        var proposal = new LinkedHashMap<String, Object>();
        proposal.put("turn_id", "T-9003");
        proposal.put("reply_turn_id", "T-9002");
        proposal.put("kind", "revision");
        proposal.put("summary", "a fifth task");
        proposal.put("applies", Map.of("key", "chat.applies.after_task", "args", Map.of(TASK_ID, "TASK-4")));
        proposal.put("before", "3 tasks");
        proposal.put("after", "4 tasks");
        proposal.put("state", "open");
        chat.put("proposal", proposal);
        chat.put("turns", List.of(
                turn("T-9001", "message", "operator", "complete", "plain", null, "Can the plan take one more task?"),
                turn("T-9002", "message", "replan", "complete", "markdown", "sonnet", "Yes, one more task."),
                turn("T-9003", "proposal", "replan", "complete", "markdown", "sonnet", "a fifth task")));
        return chat;
    }

    private static Map<String, Object> turn(String turnId, String kind, String author, String status,
            String format, String model, String text) {
        var turn = new LinkedHashMap<String, Object>();
        turn.put("turn_id", turnId);
        turn.put("kind", kind);
        turn.put("author", author);
        turn.put("status", status);
        turn.put("format", format);
        turn.put("untrusted", false);
        turn.put("text", text);
        turn.put("steps", List.of());
        turn.put("refs", List.of());
        turn.put("at", "2026-10-14T10:00:00Z");
        if (model != null) {
            turn.put("model", model);
        }
        return turn;
    }

    private static Object copy(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((key, member) -> copy.put(String.valueOf(key), copy(member)));
            return copy;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(ChatRevisionQueueTest::copy).toList());
        }
        return value;
    }

    private static ChatProposal propose(MockRuntime runtime, ChatRoutes routes) throws Exception {
        for (var index = 0; index < 2; index++) {
            send(runtime, routes);
        }
        var chat = read(runtime, routes);
        var proposal = Objects.requireNonNull(chat.proposal(), "the reply proposes");
        assertEquals(ProposalKind.REVISION, proposal.kind());
        assertEquals(ProposalState.OPEN, proposal.state());
        return proposal;
    }

    private static QueuedRevision queuedRevision(MockRuntime runtime) throws Exception {
        var plan = process(runtime);
        var queued = JsonTree.objectOrNull(plan, QUEUED_REVISION, "plan");
        return queued == null ? null : QueuedRevision.fromMap(queued);
    }

    private static int version(MockRuntime runtime) throws Exception {
        var version = JsonTree.integer(process(runtime), INSTANCE_VERSION);
        assertNotNull(version, "the plan names its instance version");
        return version;
    }

    private static Map<String, Object> process(MockRuntime runtime) throws Exception {
        var response = get(runtime, PROCESS);
        assertEquals(200, response.status(), response.bodyText());
        return JsonTree.asObject(JsonTree.parse(response.body()));
    }

    private static Map<String, Object> overviewRow(MockRuntime runtime) throws Exception {
        var response = get(runtime, PROJECT_RESOURCE);
        assertEquals(200, response.status(), response.bodyText());
        var project = JsonTree.asObject(JsonTree.parse(response.body()));
        assertNotNull(project, response.bodyText());
        for (var entry : project.entrySet()) {
            if (entry.getValue() instanceof List<?> rows && "plans".equals(entry.getKey())) {
                for (var row : rows) {
                    var map = JsonTree.asObject(row);
                    if (map == null) {
                        continue;
                    }
                    var id = JsonTree.string(map, "plan_id");
                    if (id == null) {
                        id = JsonTree.string(map, "id");
                    }
                    if (PLAN.equals(id)) {
                        return map;
                    }
                }
            }
        }
        throw new AssertionError("no plan row for " + PLAN + " in " + response.bodyText());
    }

    private static PlanRevisionQueuedEvent planRevisionQueued(SseReader events, String turnId)
            throws IOException {
        while (true) {
            var seen = events.next();
            assertNotNull(seen, "the stream ended");
            if (!PlanRevisionQueuedEvent.EVENT_NAME.equals(seen.event())) {
                continue;
            }
            var payload = payloadOf(seen.data());
            var event = PlanRevisionQueuedEvent.fromMap(payload);
            if (PLAN.equals(event.planId()) && event.queuedRevision() != null
                    && turnId.equals(event.queuedRevision().turnId())) {
                return event;
            }
        }
    }

    /**
     * Reads the stream until the plan's {@code activity.changed}, which the queued revision announces.
     */
    private static void assertActivityChanged(SseReader events) throws IOException {
        while (true) {
            var seen = events.next();
            assertNotNull(seen, "the stream ended");
            if ("activity.changed".equals(seen.event())
                    && PLAN.equals(String.valueOf(payloadOf(seen.data()).get(PLAN_ID)))) {
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

    private static HttpResponse broadcast(MockRuntime runtime, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath()).send(HttpRequest.json("POST", BROADCAST,
                body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static void broadcast(MockRuntime runtime, String event, Map<String, Object> data) throws IOException {
        var response = broadcast(runtime, JsonTree.write(Map.of("event", event, "data", data)));
        assertEquals(200, response.status(), response.bodyText());
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static MockRuntime ordered(Scenario scenario) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().initialScenario(scenario)
                .timing(MockTiming.ORDERED).build());
    }

    private static ChatResource answer(MockRuntime runtime, String path, ChatProposal proposal) throws Exception {
        var answered = post(runtime, path, new ProposalAnswerRequest(proposal.turnId()).toJson());
        assertEquals(200, answered.status(), answered.bodyText());
        return ChatResource.fromJson(answered.bodyText());
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
        assertEquals(ChatTurnAccepted.HTTP_STATUS, accepted.status(), accepted.bodyText());
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
