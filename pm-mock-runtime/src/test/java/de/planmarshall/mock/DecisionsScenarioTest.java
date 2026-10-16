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

import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.decision.DecisionAnswerRequest;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.InterpretationType;
import de.planmarshall.api.decision.QuorumBand;
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
}
