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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
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
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.mock.runtime.MockRuntimeEngine;
import de.planmarshall.mock.scenario.ScenarioLoader;

@DisplayName("DecisionsScenario (UC-PLN-2)")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class DecisionsScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String STATE = StandardScenarios.RUNNING_WORK + "#decisions.open";
    /** How long an answer's {@code decision.answered} is waited for on the stream. */
    private static final Duration ANSWERED_WAIT = Duration.ofSeconds(10);

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

                var noDecisions = Generators.letterStrings(4, 12).next().toLowerCase();
                var none = (List<?>) JsonTree.parse(
                        get(runtime, "/api/v1/decisions?project=" + noDecisions).body());
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
                        Generators.letterStrings(4, 12).next(), "cli", null);
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
                        Generators.letterStrings(4, 12).next(), "answer", null, "cli",
                        "terminal-" + Generators.integers(1, 99).next());
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
                var unknownDecision = "Q-" + Generators.integers(500, 999).next();
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-012/answer",
                        "{\"words\":\"" + Generators.letterStrings(4, 12).next() + "\"}").status());
                assertEquals(200, post(runtime, "/api/v1/decisions/Q-031/answer",
                        "{\"part_answers\":{\"1\":\"2\"}}").status());
                assertEquals(404, post(runtime, "/api/v1/decisions/" + unknownDecision + "/answer", "{}").status());
                assertEquals(404, post(runtime, "/api/v1/decisions/" + unknownDecision + "/withdraw", "{}").status());
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
    @DisplayName("The decision.answered payload")
    class AnsweredPayload {

        @Test
        @DisplayName("an answer on the Unix socket is the terminal's, and names no device whatever the body says")
        void answerWithoutDevice() throws Exception {
            var data = answeredData("{\"option\":\"2\",\"channel\":\"web\"}");

            assertAll(
                    () -> assertEquals("Q-007", data.get("id")),
                    () -> assertEquals("cli", data.get("channel"), "the channel is the listener's"),
                    () -> assertFalse(data.containsKey("device"), data.toString()));
        }

        @Test
        @DisplayName("an answer carries no device on the socket, even one the body names")
        void answerWithDevice() throws Exception {
            var device = Generators.letterStrings(3, 12).next();

            var data = answeredData("{\"option\":\"2\",\"channel\":\"web\",\"device\":\"" + device + "\"}");

            assertAll(
                    () -> assertEquals("cli", data.get("channel"), "the channel is the listener's"),
                    () -> assertFalse(data.containsKey("device"), data.toString()));
        }

        private static Map<String, Object> answeredData(String answer) throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();
                try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH_HEADER, auth))) {
                    assertTrue(exchange.isEventStream());
                    var sse = exchange.events();

                    assertEquals(200, post(runtime, "/api/v1/decisions/Q-007/answer", answer).status());

                    // The stream stays open, so a missing event would block the read: the wait is bounded, and
                    // leaving the try closes the exchange, which ends a read still waiting.
                    return assertTimeoutPreemptively(ANSWERED_WAIT, () -> {
                        for (var i = 0; i < 20; i++) {
                            var event = sse.next();
                            assertNotNull(event, "the stream ended before decision.answered");
                            if ("decision.answered".equals(event.event())) {
                                return JsonTree.asObject(JsonTree.parse(event.data()));
                            }
                        }
                        return fail("no decision.answered among the first 20 events");
                    }, () -> "no decision.answered within " + ANSWERED_WAIT.toSeconds() + " s of the answer");
                }
            }
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

    @Nested
    @DisplayName("Refusals in the catalogue's codes")
    class Refusals {

        @Test
        @DisplayName("an unknown decision is scope_not_found when read, answered, withdrawn, or interpreted")
        void unknownDecision() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var unknown = "/api/v1/decisions/Q-" + Generators.integers(500, 999).next();
                var refusals = List.of(
                        get(runtime, unknown),
                        post(runtime, unknown + "/answer", JsonTree.write(Map.of("option", "1"))),
                        post(runtime, unknown + "/withdraw", "{}"),
                        post(runtime, unknown + "/interpret", JsonTree.write(Map.of("words", "rebase"))));
                for (var refusal : refusals) {
                    assertEquals(404, refusal.status());
                    assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ApiErrorCode.fromWireOrNull(errorOf(refusal)),
                            refusal.bodyText());
                }
            }
        }

        @Test
        @DisplayName("a body that is no answer object is refused and chooses nothing for the operator")
        void unreadableAnswer() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var unreadable = post(runtime, "/api/v1/decisions/Q-007/answer",
                        "{" + Generators.letterStrings(4, 12).next());
                assertEquals(400, unreadable.status());
                assertEquals(ApiErrorCode.INVALID_ARGUMENTS, ApiErrorCode.fromWireOrNull(errorOf(unreadable)));
                assertEquals(200, get(runtime, "/api/v1/decisions/Q-007").status(),
                        "the decision stays open");
            }
        }

        @Test
        @DisplayName("an answer that names no choice is refused and the decision stays open")
        void answerWithoutChoice() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var noChoice = post(runtime, "/api/v1/decisions/Q-007/answer",
                        JsonTree.write(Map.of("channel", "cli",
                                "note", Generators.letterStrings(4, 12).next())));

                assertEquals(400, noChoice.status());
                assertEquals(ApiErrorCode.INVALID_ARGUMENTS, ApiErrorCode.fromWireOrNull(errorOf(noChoice)));
                assertEquals(200, get(runtime, "/api/v1/decisions/Q-007").status(),
                        "an answer that chooses nothing leaves the decision open");
            }
        }

        @Test
        @DisplayName("words that are no object are refused, words under own_words are read")
        void interpretBody() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var unreadable = post(runtime, "/api/v1/decisions/Q-007/interpret",
                        "[\"" + Generators.letterStrings(4, 12).next() + "\"]");
                assertEquals(400, unreadable.status());
                assertEquals(ApiErrorCode.INVALID_ARGUMENTS, ApiErrorCode.fromWireOrNull(errorOf(unreadable)));

                var ownWords = post(runtime, "/api/v1/decisions/Q-007/interpret",
                        JsonTree.write(Map.of("own_words", "neither, squash it")));
                assertEquals(200, ownWords.status());
                assertEquals(InterpretationType.OWN_OPTION,
                        DecisionInterpretation.fromJson(ownWords.bodyText()).interpretationType());
            }
        }
    }

    @Nested
    @DisplayName("The question and plan forms of a decision")
    class OtherForms {

        @Test
        @DisplayName("a question is read at its own path and only its answers act answers it")
        void questionPath() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var read = DecisionEnvelope.fromJson(get(runtime, "/api/v1/questions/Q-007").bodyText());
                assertEquals("Q-007", read.id());

                var noAct = "/api/v1/questions/Q-007/" + Generators.letterStrings(4, 10).next().toLowerCase();
                assertEquals(404, post(runtime, noAct, JsonTree.write(Map.of("option", "2"))).status());
                assertEquals(200, get(runtime, "/api/v1/decisions/Q-007").status(),
                        "a request that names no answer leaves the question open");

                var option = String.valueOf(Generators.integers(1, 3).next());
                var answered = post(runtime, "/api/v1/questions/Q-007/answers",
                        JsonTree.write(Map.of("option", option, "channel", "cli")));
                assertEquals(200, answered.status());
                var body = JsonTree.asObject(JsonTree.parse(answered.body()));
                assertEquals("answered", JsonTree.string(body, "status"));
                assertEquals(option, JsonTree.string(body, "answer"));
                assertEquals(404, get(runtime, "/api/v1/questions/Q-007").status());
            }
        }

        @Test
        @DisplayName("serves a plan's decisions at the plan's path and answers one there in own words")
        void planPath() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var listed = (List<?>) JsonTree.parse(get(runtime, "/api/v1/plans/fix-relay/decisions").body());
                assertFalse(listed.isEmpty());
                for (var item : listed) {
                    assertEquals("fix-relay", ((Map<?, ?>) item).get("plan"));
                }

                assertEquals("Q-007", DecisionEnvelope.fromJson(
                        get(runtime, "/api/v1/plans/fix-relay/decisions/Q-007").bodyText()).id());
                var words = "words " + Generators.letterStrings(4, 12).next();
                var answered = post(runtime, "/api/v1/plans/fix-relay/decisions/Q-007/answer",
                        JsonTree.write(Map.of("words", words, "channel", "cli")));
                assertEquals(200, answered.status());
                assertEquals(words, JsonTree.string(JsonTree.asObject(JsonTree.parse(answered.body())), "answer"));
                assertEquals(404, get(runtime, "/api/v1/plans/fix-relay/decisions/Q-007").status());
            }
        }

        @Test
        @DisplayName("a waiver put off for later stays open and is then declined with its reason")
        void waiverLaterThenDeclined() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var later = post(runtime, "/api/v1/decisions/WR-2/answer",
                        JsonTree.write(Map.of("option", "later")));
                assertEquals(200, later.status());
                assertEquals("open", JsonTree.string(JsonTree.asObject(JsonTree.parse(later.body())), "status"));
                assertEquals(200, get(runtime, "/api/v1/decisions/WR-2").status());

                var reason = Generators.letterStrings(4, 12).next();
                var declined = post(runtime, "/api/v1/decisions/WR-2/answer",
                        JsonTree.write(Map.of("action", "decline", "reason", reason)));
                assertEquals(200, declined.status());
                var body = JsonTree.asObject(JsonTree.parse(declined.body()));
                assertEquals("declined", JsonTree.string(body, "status"));
                assertEquals(reason, JsonTree.string(body, "reason"));
                assertEquals(404, get(runtime, "/api/v1/decisions/WR-2").status());
            }
        }
    }

    @Test
    @DisplayName("refuses a waiver action that is neither waive, decline nor later, and grants nothing")
    void waiverWithAnUnknownActionIsRefused() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var unknown = Generators.letterStrings(1, 8).next().toLowerCase();
            var action = MockRuntimeEngine.WAIVER_ACTIONS.contains(unknown) ? unknown + "x" : unknown;

            var refused = post(runtime, "/api/v1/decisions/WR-2/answer",
                    JsonTree.write(Map.of("action", action, "reason", Generators.letterStrings(4, 12).next())));

            assertEquals(400, refused.status());
            assertEquals("invalid_arguments",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(refused.body())), "error"));
            assertEquals(200, get(runtime, "/api/v1/decisions/WR-2").status(), "the request stays open");
        }
    }

    @Test
    @DisplayName("refuses a waiver answer that names no action, and grants nothing")
    void waiverWithoutAnActionIsRefused() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var refused = post(runtime, "/api/v1/decisions/WR-2/answer",
                    JsonTree.write(Map.of("reason", Generators.letterStrings(4, 12).next())));

            assertEquals(400, refused.status());
            assertEquals("invalid_arguments",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(refused.body())), "error"));
            assertEquals(200, get(runtime, "/api/v1/decisions/WR-2").status(), "the request stays open");
        }
    }

    @Test
    @DisplayName("keeps a decision added at runtime")
    void addedDecision() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var decisionId = "Q-" + Generators.integers(500, 999).next();
            var custom = new DecisionEnvelope(decisionId, 1, Generators.letterStrings(4, 10).next(), null,
                    "plan " + Generators.letterStrings(4, 10).next(),
                    DecisionScope.PLAN, "refine", "planner", "plan", Reversibility.REVERSIBLE,
                    null, null, List.of(), Instant.now(), false, null, null, null, null, null);
            runtime.engine().addDecisionAndBroadcast(custom);
            assertEquals(200, get(runtime, "/api/v1/decisions/" + decisionId).status());
        }
    }

    private static DecisionInterpretation interpret(MockRuntime runtime, String words) throws Exception {
        var response = post(runtime, "/api/v1/decisions/Q-007/interpret",
                JsonTree.write(Map.of("words", words)));
        assertEquals(200, response.status());
        return DecisionInterpretation.fromJson(response.bodyText());
    }

    private static String errorOf(HttpResponse response) throws Exception {
        return JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), "error");
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
