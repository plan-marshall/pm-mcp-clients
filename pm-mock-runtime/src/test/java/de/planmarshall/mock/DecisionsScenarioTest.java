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

import java.time.Instant;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.decision.DecisionAnswerRequest;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.DecisionScope;
import de.planmarshall.api.decision.InterpretationType;
import de.planmarshall.api.decision.QuorumBand;
import de.planmarshall.api.decision.Reversibility;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.ScenarioLoader;

@DisplayName("DecisionsScenario (UC-PLN-2)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class DecisionsScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String STATE = StandardScenarios.RUNNING_WORK + "#decisions.open";

    @Nested
    @DisplayName("The open decisions")
    class OpenDecisions {

        @Test
        @DisplayName("lists the open decisions and filters them by plan and project")
        void listsAndFilters() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(StandardScenarios.RUNNING_WORK, runtime.currentScenarioName());

                var list = (List<?>) JsonTree.parse(get(runtime, "/api/v1/decisions").body());
                assertNotNull(list);
                assertEquals(8, list.size());

                var byPlan = (List<?>) JsonTree.parse(
                        get(runtime, "/api/v1/decisions?plan=fix-relay").body());
                assertFalse(byPlan.isEmpty(), "the plan filter names the plan's decisions");
                for (var item : byPlan) {
                    assertEquals("fix-relay", ((Map<?, ?>) item).get("plan"));
                }

                var byProject = (List<?>) JsonTree.parse(
                        get(runtime, "/api/v1/decisions?project=pm-mcp-core").body());
                assertEquals(1, byProject.size());
                assertEquals("WR-3", ((Map<?, ?>) byProject.getFirst()).get("id"));

                var none = (List<?>) JsonTree.parse(
                        get(runtime, "/api/v1/decisions?project=no-such-project").body());
                assertTrue(none.isEmpty());
            }
        }

        @Test
        @DisplayName("serves a blocking decision with its quorum support and reversibility")
        void detail() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var response = get(runtime, "/api/v1/decisions/Q-007");
                assertEquals(200, response.status());
                var decision = DecisionEnvelope.fromJson(response.bodyText());

                assertAll(
                        () -> assertEquals("Q-007", decision.id()),
                        () -> assertEquals("Resolve rebase conflict", decision.title()),
                        () -> assertEquals("plan fix-relay", decision.subject()),
                        () -> assertEquals("execute", decision.askingPhase()),
                        () -> assertEquals("implement", decision.askingRole()),
                        () -> assertEquals("plan", decision.blocks()),
                        () -> assertEquals(Reversibility.REVERSIBLE, decision.reversibility()),
                        () -> assertEquals(1, decision.parts().size()));

                var part = decision.parts().getFirst();
                assertEquals("2", part.recommendation());
                var option = part.options().stream().filter(o -> "2".equals(o.key()))
                        .findFirst().orElseThrow();
                assertNotNull(option.quorumSupport());
                assertEquals(QuorumBand.CLEAR_MAJORITY, option.quorumSupport().band());
                assertEquals(2, option.quorumSupport().votesCount());
                assertEquals(3, option.quorumSupport().totalVotes());
            }
        }

        @Test
        @DisplayName("covers every reversibility, subject scope, and a multi-part question")
        void coversTheClosedLists() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(Reversibility.IRREVERSIBLE,
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-009").bodyText())
                                .reversibility());
                assertEquals(Reversibility.COSTLY,
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/WR-2").bodyText())
                                .reversibility());
                assertEquals(DecisionScope.PROJECT_SETUP,
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-031").bodyText())
                                .scope());
                assertEquals(DecisionScope.EPIC,
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-015").bodyText())
                                .scope());
                assertEquals(DecisionScope.MACHINE,
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-016").bodyText())
                                .scope());
                assertEquals("setup",
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-031").bodyText())
                                .blocks());

                // A non-blocking question names no scope it holds.
                assertEquals("none",
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-016").bodyText())
                                .blocks());
                assertNull(DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-016").bodyText())
                        .parts().getFirst().recommendation());
                assertEquals("epic",
                        DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-015").bodyText())
                                .blocks());

                var multi = DecisionEnvelope.fromJson(get(runtime, "/api/v1/decisions/Q-012").bodyText());
                assertEquals(3, multi.parts().size());
                assertEquals("1", multi.parts().getFirst().recommendation());
                assertNotNull(multi.parts().get(1).dependency());
            }
        }

        @Test
        @DisplayName("a waiver requires its reason and records the waiver")
        void waiver() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var noReason = new DecisionAnswerRequest(Map.of(), null, null, null, "waive", "",
                        "cli", null);
                assertEquals(400, post(runtime, "/api/v1/decisions/WR-2/answer", noReason.toJson()).status());

                var withReason = new DecisionAnswerRequest(Map.of(), null, null, null, "waive",
                        "Covered by build box run", "cli", null);
                var response = post(runtime, "/api/v1/decisions/WR-2/answer", withReason.toJson());
                assertEquals(200, response.status());
                assertEquals("waived", JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())),
                        "status"));
            }
        }
    }

    @Nested
    @DisplayName("Answers in the operator's own words")
    class OwnWords {

        @Test
        @DisplayName("reads an option with a condition")
        void optionWithCondition() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var interpretation = interpret(runtime, "rebase, but leave release/0.9 alone, it is frozen");
                assertEquals(InterpretationType.OPTION_WITH_CONDITION, interpretation.interpretationType());
                assertEquals("2", interpretation.mappedOptionKey());
                assertNotNull(interpretation.condition());
            }
        }

        @Test
        @DisplayName("reads a plain option and an own option")
        void optionAndOwnOption() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(InterpretationType.OPTION,
                        interpret(runtime, "rebase onto main").interpretationType());
                assertEquals(InterpretationType.OWN_OPTION,
                        interpret(runtime, "neither, please squash").interpretationType());
            }
        }

        @Test
        @DisplayName("leaves an unclear reading open with its clarification question")
        void unclear() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var interpretation = interpret(runtime, "do whatever the reviewer would do");
                assertEquals(InterpretationType.UNCLEAR, interpretation.interpretationType());
                assertNotNull(interpretation.clarificationQuestion());
            }
        }

        @Test
        @DisplayName("reads a delegation")
        void delegation() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(InterpretationType.DELEGATION,
                        interpret(runtime, "leave it to the team").interpretationType());
            }
        }
    }

    @Nested
    @DisplayName("Answering, later, and withdrawal")
    class Answers {

        @Test
        @DisplayName("answers a decision and removes it from the open list")
        void answers() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var request = new DecisionAnswerRequest(Map.of("1", "2"), "2", null,
                        "tested locally", "answer", null, "cli", "terminal-1");
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-007/answer", request.toJson()).status());
                assertEquals(404, get(runtime, "/api/v1/decisions/Q-007").status());
            }
        }

        @Test
        @DisplayName("later leaves the decision open")
        void later() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-012/answer",
                        "{\"action\":\"later\"}").status());
                assertEquals(200, get(runtime, "/api/v1/decisions/Q-012").status());
            }
        }

        @Test
        @DisplayName("withdraws a decision and asks it anew naming the one it replaces")
        void withdraws() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-007/withdraw", "{}").status());
                assertEquals(404, get(runtime, "/api/v1/decisions/Q-007").status());
                assertEquals(200, get(runtime, "/api/v1/decisions/Q-009").status());
            }
        }

        @Test
        @DisplayName("answers parts, own words, and refuses an unknown decision")
        void partsAndUnknown() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-012/answer",
                        "{\"words\":\"custom answer text\"}").status());
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-031/answer",
                        "{\"part_answers\":{\"1\":\"2\"}}").status());
                assertEquals(404, post(runtime, "/api/v1/decisions/NONEXISTENT/answer", "{}").status());
                assertEquals(404, post(runtime, "/api/v1/decisions/NONEXISTENT/withdraw", "{}").status());
                assertEquals(400, post(runtime, "/api/v1/decisions/WR-3/answer", "{invalid").status());
            }
        }
    }

    @Nested
    @DisplayName("A decision answered in the other client")
    class AnsweredElsewhere {

        @Test
        @DisplayName("leaves the decision out of the open list")
        void answeredElsewhere() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK
                         + "#decisions.answered-elsewhere")) {
                assertEquals(404, get(runtime, "/api/v1/decisions/Q-007").status());
                var list = (List<?>) JsonTree.parse(get(runtime, "/api/v1/decisions").body());
                assertEquals(7, list.size());
            }
        }

        @Test
        @DisplayName("announces decision.answered and decision.closed")
        void announcesTheEvents() {
            var state = ScenarioLoader.load(StandardScenarios.RUNNING_WORK)
                    .state("decisions.answered-elsewhere");
            var names = state.events().stream().map(event -> event.event()).toList();
            assertEquals(List.of("decision.answered", "decision.closed"), names);
        }
    }

    @Nested
    @DisplayName("A withdrawn decision")
    class Withdrawn {

        @Test
        @DisplayName("leaves the decision out and keeps the replacement")
        void withdrawn() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#decisions.withdrawn")) {
                assertEquals(404, get(runtime, "/api/v1/decisions/Q-007").status());
                assertEquals(200, get(runtime, "/api/v1/decisions/Q-009").status());
            }
        }

        @Test
        @DisplayName("announces decision.changed with the replacement")
        void announcesTheChange() {
            var state = ScenarioLoader.load(StandardScenarios.RUNNING_WORK).state("decisions.withdrawn");
            assertEquals(1, state.events().size());
            assertEquals("decision.changed", state.events().getFirst().event());
            var data = JsonTree.asObject(state.events().getFirst().data());
            assertEquals("Q-009", JsonTree.string(data, "replaced_by"));
        }
    }

    @Test
    @DisplayName("keeps a decision added at runtime")
    void addedDecision() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var custom = new DecisionEnvelope("Q-999", 1, "Custom", null, "plan custom",
                    DecisionScope.PLAN, "refine", "planner", "plan", Reversibility.REVERSIBLE,
                    null, null, List.of(), Instant.now(), false, null, null, null);
            runtime.engine().addDecisionAndBroadcast(custom);
            assertEquals(200, get(runtime, "/api/v1/decisions/Q-999").status());
        }
    }

    private static DecisionInterpretation interpret(MockRuntime runtime, String words) throws Exception {
        var response = post(runtime, "/api/v1/decisions/Q-007/interpret",
                JsonTree.write(Map.of("words", words)));
        assertEquals(200, response.status());
        return DecisionInterpretation.fromJson(response.bodyText());
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
