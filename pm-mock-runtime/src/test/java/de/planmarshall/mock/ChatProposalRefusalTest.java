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
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * Verifies that a revision proposal refused for any cause (no plan, passed boundary, or failed check)
 * still accepts {@code reject}, refuses a second {@code accept}, and moves cleanly into
 * {@link de.planmarshall.api.chat.ProposalState#REJECTED}.
 */
@DisplayName("Refused proposals still accept reject")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatProposalRefusalTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String PLAN = "session-log";
    private static final String BROADCAST = "/mock/broadcast";

    @Test
    @DisplayName("a proposal refused for no plan still accepts reject")
    void refusedForNoPlanAcceptsReject() throws Exception {
        var routes = ChatRoutes.machineDecision("Q-999");
        try (var runtime = ordered(withMachineDecisionRevision("machine/Q-999"))) {
            var chat = read(runtime, routes);
            var proposal = Objects.requireNonNull(chat.proposal());

            var refused = answer(runtime, routes.accept(), proposal);
            assertRefusedStaysRejectable(runtime, routes, refused, proposal);
        }
    }

    @Test
    @DisplayName("a proposal refused for passed boundary still accepts reject")
    void refusedForPassedBoundaryAcceptsReject() throws Exception {
        var routes = ChatRoutes.plan(PLAN);
        try (var runtime = ordered(CHAT_STATE)) {
            open(runtime, routes);
            send(runtime, routes);
            send(runtime, routes);
            var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
            broadcast(runtime, "task.ended", Map.of("plan_id", PLAN, "task_id", "TASK-4", "status", "done"));

            var refused = answer(runtime, routes.accept(), proposal);
            assertRefusedStaysRejectable(runtime, routes, refused, proposal);
        }
    }

    @Test
    @DisplayName("a proposal refused at its boundary for a failed check still accepts reject")
    void refusedForFailedCheckAcceptsReject() throws Exception {
        var routes = ChatRoutes.plan(PLAN);
        try (var runtime = ordered(CHAT_STATE)) {
            open(runtime, routes);
            send(runtime, routes);
            send(runtime, routes);
            var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
            answer(runtime, routes.accept(), proposal);
            broadcast(runtime, "instance.changed", Map.of("plan_id", PLAN, "instance_version", 2, "cause", "replan"));

            broadcast(runtime, "task.ended", Map.of("plan_id", PLAN, "task_id", "TASK-4", "status", "done"));

            var refused = read(runtime, routes);
            assertRefusedStaysRejectable(runtime, routes, refused, proposal);
        }
    }

    private static void assertRefusedStaysRejectable(MockRuntime runtime, ChatRoutes routes, ChatResource refused,
            ChatProposal original) throws Exception {
        var proposal = Objects.requireNonNull(refused.proposal());
        var body = new ProposalAnswerRequest(original.turnId()).toJson();
        assertAll("refused proposal still allows reject and refuses accept",
                () -> assertEquals(ProposalState.REFUSED, proposal.state()),
                () -> assertNotNull(proposal.finding()),
                () -> assertEquals(409, post(runtime, routes.accept(), body).status()));

        var rejected = answer(runtime, routes.reject(), original);
        assertAll("rejected after refusal",
                () -> assertEquals(ProposalState.REJECTED,
                        Objects.requireNonNull(rejected.proposal()).state()),
                () -> assertNull(Objects.requireNonNull(rejected.proposal()).finding()),
                () -> assertEquals(ChatStatus.TURN, rejected.state()));
    }

    @SuppressWarnings("unchecked")
    private static Scenario withMachineDecisionRevision(String decisionSubjectId) {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copy(state.representation());
        var control = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) representation
                .get("facts")).get("runtime")).get("control");
        var chats = (List<Object>) control.get("chats");
        chats.add(machineDecisionRevisionChat(decisionSubjectId));
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    private static Map<String, Object> machineDecisionRevisionChat(String subjectId) {
        var chat = new LinkedHashMap<String, Object>();
        chat.put("id", "C-9100");
        chat.put("subject", Map.of("kind", "decision", "id", subjectId));
        chat.put("role", Map.of("name", "refine", "model", "sonnet"));
        chat.put("scope", "decision");
        chat.put("state", "chat.read-back");
        chat.put("context", Map.of("knows", List.of(Map.of("key", "chat.knows.decision",
                "args", Map.of("id", "Q-999"))), "as_of", "2026-10-14T10:00:00Z"));
        var proposal = new LinkedHashMap<String, Object>();
        proposal.put("turn_id", "T-9103");
        proposal.put("reply_turn_id", "T-9102");
        proposal.put("kind", "revision");
        proposal.put("summary", "cannot revise without a plan");
        proposal.put("applies", Map.of("key", "chat.applies.after_task", "args", Map.of("task_id", "TASK-4")));
        proposal.put("before", "1 task");
        proposal.put("after", "2 tasks");
        proposal.put("state", "open");
        chat.put("proposal", proposal);
        chat.put("turns", List.of(
                turn("T-9101", "message", "operator", "Can we revise?"),
                turn("T-9102", "message", "refine", "Proposed a revision."),
                turn("T-9103", "proposal", "refine", "cannot revise without a plan")));
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
            return new ArrayList<>(list.stream().map(ChatProposalRefusalTest::copy).toList());
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

    private static ChatResource open(MockRuntime runtime, ChatRoutes routes) throws Exception {
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

    private static ChatResource answer(MockRuntime runtime, String path, ChatProposal proposal) throws Exception {
        var response = post(runtime, path, new ProposalAnswerRequest(proposal.turnId()).toJson());
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
