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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.NarrativeLine;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * Verifies revision-window closure and narrative fidelity (PLAN-194 D3): {@code scope.ended} closes
 * the revision window in {@code planActivities}, {@code plan.paused} in {@code pausing} is narrated
 * without deduplicating the later {@code paused}, {@code planFor} never binds an epic or entry
 * decision to a plan by prefix, and an {@code infeasible} task outcome is never narrated as
 * {@code done}. Note: {@code pm-api} and {@code pm-mock-runtime} declare no {@code sanity-check}
 * phase, so no phase constant is added here.
 */
@DisplayName("Revision window and narrative truth (D3)")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatWindowAndNarrativeTruthTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String PLAN = "session-log";
    private static final String BROADCAST = "/mock/broadcast";

    @Test
    @DisplayName("scope.ended closes the revision window so a plan decision's revision lapses (M1)")
    void scopeEndedClosesRevisionWindow() throws Exception {
        var questionId = "Q-" + Generators.integers(100, 999).next();
        var decisionRoutes = ChatRoutes.planDecision(PLAN, questionId);
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                     .initialScenario(withPlanDecisionRevision(PLAN, questionId)).timing(MockTiming.ORDERED).build())) {
            var chat = read(runtime, decisionRoutes);
            var proposal = Objects.requireNonNull(chat.proposal());

            broadcast(runtime, "scope.ended", Map.of("plan_id", PLAN, "outcome", "merged",
                    "scope", Map.of("kind", "plan", "id", PLAN, "project_id", "pm-mcp-core")));

            assertAll("scope.ended closes revision window and lapses decision chat revision",
                    () -> assertFalse(runtime.engine().revisionWindowOpen(PLAN),
                            "scope.ended closes the plan's revision window via planActivities"),
                    () -> assertEquals(ProposalState.LAPSED,
                            Objects.requireNonNull(read(runtime, decisionRoutes).proposal()).state()),
                    () -> assertEquals(409,
                            post(runtime, decisionRoutes.accept(),
                                    new ProposalAnswerRequest(proposal.turnId()).toJson()).status()));
        }
    }

    @Test
    @DisplayName("scope.ended with an active queued revision keeps activity ended and window closed")
    void scopeEndedWithQueuedRevisionKeepsWindowClosed() throws Exception {
        var questionId = "Q-" + Generators.integers(100, 999).next();
        var decisionRoutes = ChatRoutes.planDecision(PLAN, questionId);
        var planRoutes = ChatRoutes.plan(PLAN);
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                     .initialScenario(withPlanDecisionRevision(PLAN, questionId))
                     .timing(MockTiming.ORDERED).build())) {
            open(runtime, planRoutes);
            send(runtime, planRoutes);
            send(runtime, planRoutes);
            var planProposal = Objects.requireNonNull(read(runtime, planRoutes).proposal());
            assertEquals(200, post(runtime, planRoutes.accept(),
                    new ProposalAnswerRequest(planProposal.turnId()).toJson()).status());
            var queued = Objects.requireNonNull(runtime.engine().queuedRevision(PLAN),
                    "accepted revision must queue on the plan");
            assertEquals(planProposal.turnId(), queued.turnId());

            var decisionProposal = Objects.requireNonNull(read(runtime, decisionRoutes).proposal());
            broadcast(runtime, "scope.ended", Map.of("plan_id", PLAN, "outcome", "merged",
                    "scope", Map.of("kind", "plan", "id", PLAN, "project_id", "pm-mcp-core")));

            assertAll("queued revision at scope.ended never re-opens activity or revision window",
                    () -> assertEquals("ended", runtime.engine().activity(PLAN)),
                    () -> assertFalse(runtime.engine().revisionWindowOpen(PLAN),
                            "revisionWindowOpen must stay closed after activity.changed"),
                    () -> assertEquals(ProposalState.LAPSED,
                            Objects.requireNonNull(read(runtime, decisionRoutes).proposal()).state()),
                    () -> assertEquals(409,
                            post(runtime, decisionRoutes.accept(),
                                    new ProposalAnswerRequest(decisionProposal.turnId()).toJson()).status()));

            var pauseAfterEnd = post(runtime, "/api/v1/plans/" + PLAN + "/actions/pause", "{}");
            assertAll("ended plan stays ended and rejects control pause",
                    () -> assertEquals(409, pauseAfterEnd.status(), pauseAfterEnd.bodyText()),
                    () -> assertEquals("ended", runtime.engine().activity(PLAN)),
                    () -> assertFalse(runtime.engine().revisionWindowOpen(PLAN)));
        }
    }

    @Test
    @DisplayName("plan.paused in state pausing narrates pausing and does not deduplicate the later paused (m1)")
    void pausingThenPausedBothNarrated() throws Exception {
        var routes = ChatRoutes.plan(PLAN);
        try (var runtime = ordered(CHAT_STATE)) {
            assertEquals(201, post(runtime, routes.chat(), "").status());

            broadcast(runtime, "plan.paused", Map.of("plan_id", PLAN, "state", "pausing", "point", "task"));
            broadcast(runtime, "plan.paused",
                    Map.of("plan_id", PLAN, "state", "paused", "channel", "web",
                            "device", Generators.letterStrings(4, 10).next()));

            var lines = noticeLines(read(runtime, routes));
            assertAll("pausing and paused are both narrated in order and engine records paused",
                    () -> assertEquals("paused", runtime.engine().activity(PLAN)),
                    () -> assertEquals(List.of("chat.line.plan_pausing", "chat.line.plan_paused"),
                            lines.stream().map(NarrativeLine::key).toList()),
                    () -> assertEquals("task", String.valueOf(lines.get(0).args().get("point"))),
                    () -> assertEquals("web", String.valueOf(lines.get(1).args().get("channel"))));
        }
    }

    @Test
    @DisplayName("fixture validation rejects a decision chat whose subject prefix disagrees with the decision's plan")
    void mismatchedDecisionChatBindingRejected() {
        var scenario = withMismatchedDecisionChat(PLAN, "other-plan", "Q-" + Generators.integers(100, 999).next());
        var config = MockRuntimeConfig.builder().initialScenario(scenario).build();
        assertThrows(IllegalStateException.class, () -> MockRuntime.start(config));
    }

    @Test
    @DisplayName("planFor does not bind an epic or entry decision X/Q-9 to plan X by prefix (m2)")
    void planForIgnoresEpicAndEntryDecisionsMatchingPlanName() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                     .initialScenario(withNonPlanDecisionsSharingPlanId(PLAN)).timing(MockTiming.ORDERED).build())) {
            var epicSubject = new ChatSubject(ChatSubjectKind.DECISION, PLAN + "/Q-91");
            var entrySubject = new ChatSubject(ChatSubjectKind.DECISION, PLAN + "/Q-92");
            var epicDecisionRoutes = ChatRoutes.epicDecision(PLAN, "Q-91");
            var entryDecisionRoutes = ChatRoutes.entryDecision(PLAN, "Q-92");

            assertAll("planFor binds only plan decisions",
                    () -> assertNull(runtime.engine().planFor(epicSubject),
                            "epic decision must not bind to plan by prefix"),
                    () -> assertNull(runtime.engine().planFor(entrySubject),
                            "entry decision must not bind to plan by prefix"));

            var epicAccept = ChatResource.fromJson(post(runtime, epicDecisionRoutes.accept(),
                    new ProposalAnswerRequest("T-9213").toJson()).bodyText());
            var entryAccept = ChatResource.fromJson(post(runtime, entryDecisionRoutes.accept(),
                    new ProposalAnswerRequest("T-9223").toJson()).bodyText());

            assertAll("unplanned decisions refuse revisions and never queue on plan " + PLAN,
                    () -> assertEquals(ProposalState.REFUSED, Objects.requireNonNull(epicAccept.proposal()).state()),
                    () -> assertEquals(ProposalState.REFUSED, Objects.requireNonNull(entryAccept.proposal()).state()),
                    () -> assertNull(runtime.engine().queuedRevision(PLAN)));
        }
    }

    @Test
    @DisplayName("infeasible task outcome is narrated as infeasible, never done (m4)")
    void infeasibleTaskOutcomeNotNarratedAsDone() throws Exception {
        var routes = ChatRoutes.plan(PLAN);
        try (var runtime = ordered(CHAT_STATE)) {
            assertEquals(201, post(runtime, routes.chat(), "").status());

            broadcast(runtime, "task.ended", Map.of("plan_id", PLAN, "task_id", "TASK-3", "status", "infeasible"));
            broadcast(runtime, "task.ended", Map.of("plan_id", PLAN, "task_id", "TASK-4", "outcome", "infeasible"));

            var lines = noticeLines(read(runtime, routes));
            assertAll("infeasible outcome preserved",
                    () -> assertEquals(List.of("chat.line.task_ended", "chat.line.task_ended"),
                            lines.stream().map(NarrativeLine::key).toList()),
                    () -> assertEquals("infeasible", String.valueOf(lines.get(0).args().get("outcome"))),
                    () -> assertEquals("infeasible", String.valueOf(lines.get(1).args().get("outcome"))),
                    () -> assertNotEquals("done", String.valueOf(lines.get(0).args().get("outcome"))));
        }
    }

    @SuppressWarnings("unchecked")
    private static Scenario withMismatchedDecisionChat(String chatPlanId, String decisionPlanId, String questionId) {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copy(state.representation());
        var runtimeNode = (Map<String, Object>) ((Map<String, Object>) representation.get("facts")).get("runtime");
        var control = (Map<String, Object>) runtimeNode.get("control");
        ((List<Object>) runtimeNode.get("decisions")).add(scopedDecision(questionId, "plan", decisionPlanId));
        ((List<Object>) control.get("chats")).add(decisionRevisionChat("C-mismatch", chatPlanId + "/" + questionId,
                "Tm-"));
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    @SuppressWarnings("unchecked")
    private static Scenario withPlanDecisionRevision(String planId, String questionId) {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copy(state.representation());
        var runtime = (Map<String, Object>) ((Map<String, Object>) representation.get("facts")).get("runtime");
        var control = (Map<String, Object>) runtime.get("control");
        var chats = (List<Object>) control.get("chats");
        chats.add(decisionRevisionChat("C-9200", planId + "/" + questionId, "T-920"));
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    @SuppressWarnings("unchecked")
    private static Scenario withNonPlanDecisionsSharingPlanId(String sharedId) {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copy(state.representation());
        var runtime = (Map<String, Object>) ((Map<String, Object>) representation.get("facts")).get("runtime");
        var decisions = (List<Object>) runtime.get("decisions");
        decisions.add(scopedDecision("Q-91", "epic", sharedId));
        var start = new LinkedHashMap<String, Object>();
        start.put("project_id", "pm-mcp-clients");
        start.put("kind", "plan");
        start.put("state", "start.sources");
        start.put("entry_id", sharedId);
        start.put("sources", List.of());
        runtime.put("start", start);
        var control = (Map<String, Object>) runtime.get("control");
        var chats = (List<Object>) control.get("chats");
        chats.add(decisionRevisionChat("C-9210", sharedId + "/Q-91", "T-921"));
        chats.add(decisionRevisionChat("C-9220", sharedId + "/Q-92", "T-922"));
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    private static Map<String, Object> scopedDecision(String id, String scope, String subjectId) {
        var decision = new LinkedHashMap<String, Object>();
        decision.put("id", id);
        decision.put("revision", 1);
        decision.put("title", "Scope decision " + id);
        decision.put("subject", scope + " " + subjectId);
        decision.put("scope", scope);
        decision.put("asking_phase", "decompose");
        decision.put("asking_role", "decompose");
        decision.put("blocks", "none");
        decision.put("reversibility", "reversible");
        decision.put("description", "A decision whose scope is " + scope);
        decision.put("parts", List.of());
        return decision;
    }

    private static Map<String, Object> decisionRevisionChat(String chatId, String subjectId, String turnPrefix) {
        var chat = new LinkedHashMap<String, Object>();
        chat.put("id", chatId);
        chat.put("subject", Map.of("kind", "decision", "id", subjectId));
        chat.put("role", Map.of("name", "decompose", "model", "sonnet"));
        chat.put("scope", "decision");
        chat.put("state", "chat.read-back");
        chat.put("context", Map.of("knows", List.of(Map.of("key", "chat.knows.decision",
                        "args", Map.of("id", subjectId.substring(subjectId.indexOf('/') + 1)))),
                "as_of", "2026-10-14T10:00:00Z"));
        var proposal = new LinkedHashMap<String, Object>();
        proposal.put("turn_id", turnPrefix + "3");
        proposal.put("reply_turn_id", turnPrefix + "2");
        proposal.put("kind", "revision");
        proposal.put("summary", "a revision");
        proposal.put("applies", Map.of("key", "chat.applies.after_task", "args", Map.of("task_id", "TASK-4")));
        proposal.put("before", Generators.letterStrings(4, 16).next());
        proposal.put("after", Generators.letterStrings(4, 16).next());
        proposal.put("state", "open");
        chat.put("proposal", proposal);
        chat.put("turns", List.of(
                turn(turnPrefix + "1", "message", "operator", Generators.letterStrings(4, 24).next()),
                turn(turnPrefix + "2", "message", "decompose", Generators.letterStrings(4, 24).next()),
                turn(turnPrefix + "3", "proposal", "decompose", Generators.letterStrings(4, 24).next())));
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

    private static List<NarrativeLine> noticeLines(ChatResource chat) {
        return chat.turns().stream()
                .filter(turn -> turn.kind() == TurnKind.NOTICE)
                .flatMap(turn -> turn.lines().stream())
                .toList();
    }

    private static Object copy(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((key, member) -> copy.put(String.valueOf(key), copy(member)));
            return copy;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(ChatWindowAndNarrativeTruthTest::copy).toList());
        }
        return value;
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static void open(MockRuntime runtime, ChatRoutes routes) throws IOException {
        assertEquals(201, post(runtime, routes.chat(), "").status());
    }

    private static void send(MockRuntime runtime, ChatRoutes routes) throws IOException {
        var message = new ChatTurnRequest(Generators.letterStrings(4, 30).next(),
                Generators.letterStrings(8, 16).next());
        assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message.toJson()).status());
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

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
