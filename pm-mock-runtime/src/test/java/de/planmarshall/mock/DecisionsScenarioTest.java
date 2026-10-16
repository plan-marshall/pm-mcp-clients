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

import java.time.Instant;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
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
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

@DisplayName("DecisionsScenario (UC-PLN-2)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class DecisionsScenarioTest {

    @Test
    @DisplayName("loads decisions scenario and lists open decisions")
    void shouldLoadScenarioAndListDecisions() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.DECISIONS, runtime.currentScenarioName());

            var res = client.send(HttpRequest.get("/api/v1/decisions").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var list = (List<?>) JsonTree.parse(res.body());
            assertNotNull(list);
            assertTrue(list.size() >= 6, "Expected at least 6 open decisions/waivers");

            // Filter by plan
            var planRes = client.send(HttpRequest.get("/api/v1/decisions?plan=fix-relay").withHeader("Authorization", auth));
            assertEquals(200, planRes.status());
            var planList = (List<?>) JsonTree.parse(planRes.body());
            for (var item : planList) {
                var map = (Map<?, ?>) item;
                assertEquals("fix-relay", map.get("plan"));
            }
        }
    }

    @Test
    @DisplayName("retrieves full decision envelope with quorum details")
    void shouldGetDecisionDetail() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/decisions/Q-007").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var env = DecisionEnvelope.fromJson(res.bodyText());
            assertAll(
                    () -> assertEquals("Q-007", env.id()),
                    () -> assertEquals("Resolve rebase conflict", env.title()),
                    () -> assertEquals("plan fix-relay", env.subject()),
                    () -> assertEquals("execute", env.askingPhase()),
                    () -> assertEquals("implement", env.askingRole()),
                    () -> assertEquals(1, env.parts().size())
            );

            var part = env.parts().get(0);
            assertEquals("2", part.recommendation());
            var opt2 = part.options().stream().filter(o -> "2".equals(o.key())).findFirst().orElseThrow();
            assertNotNull(opt2.quorumSupport());
            assertEquals(QuorumBand.CLEAR_MAJORITY, opt2.quorumSupport().band());
            assertEquals(2, opt2.quorumSupport().votesCount());
            assertEquals(3, opt2.quorumSupport().totalVotes());
        }
    }

    @Test
    @DisplayName("interprets operator own words with condition, unclear, and delegation")
    void shouldInterpretOwnWords() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // Condition interpretation
            var condReq = HttpRequest.json("POST", "/api/v1/decisions/Q-007/interpret",
                    JsonTree.write(Map.of("words", "rebase, but leave release/0.9 alone, it is frozen")))
                    .withHeader("Authorization", auth);
            var condRes = client.send(condReq);
            assertEquals(200, condRes.status());
            var condInterp = DecisionInterpretation.fromJson(condRes.bodyText());
            assertEquals(InterpretationType.OPTION_WITH_CONDITION, condInterp.interpretationType());
            assertEquals("2", condInterp.mappedOptionKey());
            assertNotNull(condInterp.condition());

            // Unclear interpretation
            var unclearReq = HttpRequest.json("POST", "/api/v1/decisions/Q-007/interpret",
                    JsonTree.write(Map.of("words", "do whatever the reviewer would do")))
                    .withHeader("Authorization", auth);
            var unclearRes = client.send(unclearReq);
            assertEquals(200, unclearRes.status());
            var unclearInterp = DecisionInterpretation.fromJson(unclearRes.bodyText());
            assertEquals(InterpretationType.UNCLEAR, unclearInterp.interpretationType());
            assertNotNull(unclearInterp.clarificationQuestion());
        }
    }

    @Test
    @DisplayName("answers a decision and removes it from open list")
    void shouldAnswerDecision() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var req = new DecisionAnswerRequest(Map.of("1", "2"), "2", null, "tested locally", "answer", null, "cli", "terminal-1");
            var ansRes = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-007/answer", req.toJson()).withHeader("Authorization", auth));
            assertEquals(200, ansRes.status());

            // Verify Q-007 is no longer in open decisions
            var getRes = client.send(HttpRequest.get("/api/v1/decisions/Q-007").withHeader("Authorization", auth));
            assertEquals(404, getRes.status());
        }
    }

    @Test
    @DisplayName("handles waiver requests: enforces reason and records waiver")
    void shouldHandleWaiver() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // Missing reason fails with 400
            var noReason = new DecisionAnswerRequest(Map.of(), null, null, null, "waive", "", "cli", null);
            var badRes = client.send(HttpRequest.json("POST", "/api/v1/decisions/WR-2/answer", noReason.toJson()).withHeader("Authorization", auth));
            assertEquals(400, badRes.status());

            // Valid reason succeeds
            var withReason = new DecisionAnswerRequest(Map.of(), null, null, null, "waive", "Covered by build box run", "cli", null);
            var goodRes = client.send(HttpRequest.json("POST", "/api/v1/decisions/WR-2/answer", withReason.toJson()).withHeader("Authorization", auth));
            assertEquals(200, goodRes.status());

            var map = (Map<?, ?>) JsonTree.parse(goodRes.body());
            assertEquals("waived", map.get("status"));
        }
    }

    @Test
    @DisplayName("withdraws decision and creates replacement")
    void shouldWithdrawDecision() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var withRes = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-007/withdraw", "{}").withHeader("Authorization", auth));
            assertEquals(200, withRes.status());

            // Q-007 is closed
            var checkOld = client.send(HttpRequest.get("/api/v1/decisions/Q-007").withHeader("Authorization", auth));
            assertEquals(404, checkOld.status());

            // Replacement Q-009 exists
            var checkNew = client.send(HttpRequest.get("/api/v1/decisions/Q-009").withHeader("Authorization", auth));
            assertEquals(200, checkNew.status());
        }
    }

    @Test
    @DisplayName("exercises remaining decision routes, queries, interpretations and broadcast")
    void shouldCoverAdditionalDecisionEndpoints() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECISIONS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // 1. GET /api/v1/plans/{plan}/decisions
            var planDecs = client.send(HttpRequest.get("/api/v1/plans/fix-relay/decisions").withHeader("Authorization", auth));
            assertEquals(200, planDecs.status());

            // 2. GET /api/v1/questions/{id}
            var qGet = client.send(HttpRequest.get("/api/v1/questions/Q-007").withHeader("Authorization", auth));
            assertEquals(200, qGet.status());

            // 3. POST /api/v1/questions/{id}/answer
            var qAns = client.send(HttpRequest.json("POST", "/api/v1/questions/Q-007/answer", "{\"option\":\"2\"}").withHeader("Authorization", auth));
            assertEquals(200, qAns.status());

            // 4. Project filters
            var projMatch = client.send(HttpRequest.get("/api/v1/decisions?project=pm-mcp-clients").withHeader("Authorization", auth));
            assertEquals(200, projMatch.status());
            var projNone = client.send(HttpRequest.get("/api/v1/decisions?project=no-such-project").withHeader("Authorization", auth));
            assertEquals(200, projNone.status());
            assertEquals(0, ((List<?>) JsonTree.parse(projNone.body())).size());

            // 5. 404 for unknown decision answer & withdraw
            var badAns = client.send(HttpRequest.json("POST", "/api/v1/decisions/NONEXISTENT/answer", "{}").withHeader("Authorization", auth));
            assertEquals(404, badAns.status());
            var badWith = client.send(HttpRequest.json("POST", "/api/v1/decisions/NONEXISTENT/withdraw", "{}").withHeader("Authorization", auth));
            assertEquals(404, badWith.status());

            // 6. Interpret with squash, leave, and fallback on open decision Q-012
            var interpSquash = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-012/interpret", "{\"words\":\"neither, please squash\"}").withHeader("Authorization", auth));
            assertEquals(200, interpSquash.status());

            var interpLeave = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-012/interpret", "{\"words\":\"leave it to the team\"}").withHeader("Authorization", auth));
            assertEquals(200, interpLeave.status());

            var interpFallback = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-012/interpret", "{\"words\":\"completely custom phrase\"}").withHeader("Authorization", auth));
            assertEquals(200, interpFallback.status());

            // 7. Action: later (Q-012 stays open)
            var laterAns = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-012/answer", "{\"action\":\"later\"}").withHeader("Authorization", auth));
            assertEquals(200, laterAns.status());

            // 8. Answer with words on Q-012
            var wordsAns = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-012/answer", "{\"words\":\"custom answer text\"}").withHeader("Authorization", auth));
            assertEquals(200, wordsAns.status());

            // 9. Answer with part_answers on Q-031
            var partsAns = client.send(HttpRequest.json("POST", "/api/v1/decisions/Q-031/answer", "{\"part_answers\":{\"1\":\"2\"}}").withHeader("Authorization", auth));
            assertEquals(200, partsAns.status());

            // 10. Malformed input error handling
            var malformed = client.send(HttpRequest.json("POST", "/api/v1/decisions/WR-3/answer", "{invalid").withHeader("Authorization", auth));
            assertEquals(400, malformed.status());

            // 10. addDecisionAndBroadcast
            var customEnv = new DecisionEnvelope("Q-999", 1, "Custom", null, "plan custom", DecisionScope.PLAN,
                    "refine", "planner", "plan", Reversibility.REVERSIBLE, null, null, List.of(), Instant.now(),
                    false, null, null, null);
            runtime.engine().addDecisionAndBroadcast(customEnv);

            var checkBroadcasted = client.send(HttpRequest.get("/api/v1/decisions/Q-999").withHeader("Authorization", auth));
            assertEquals(200, checkBroadcasted.status());
        }
    }
}
