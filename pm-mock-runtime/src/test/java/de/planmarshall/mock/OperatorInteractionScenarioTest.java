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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.locks.LockSupport;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.ScenarioLoader;
import de.planmarshall.mock.scenario.ScenarioValidationException;

@DisplayName("Operator interaction scenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OperatorInteractionScenarioTest {

    @Test
    @DisplayName("serves an operator state with links keyed by id, check.result events, and a moved state")
    void operatorInteraction() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.OPERATOR_INTERACTION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                // 1. the preselected operator link 'yes' of wait.choose leads to wait.check
                var yes = client.send(HttpRequest.json("POST", "/api/v1/machine/links/yes", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, yes.status());
                var check = JsonTree.asObject(JsonTree.parse(yes.body()));
                assertEquals("onboarding.check", JsonTree.string(check, "state_id"));

                // the state's operator links are keyed by their id and carry the whole contract
                var links = JsonTree.asArray(check.get("links"));
                assertEquals(1, links.size());
                var retry = JsonTree.asObject(links.getFirst());
                assertEquals("retry", JsonTree.string(retry, "id"));
                assertEquals("retry", JsonTree.string(retry, "rel"));
                assertEquals("POST", JsonTree.string(retry, "method"));
                assertEquals("/api/v1/machine/links/retry", JsonTree.string(retry, "href"));

                // 2. the check.result events of the closed catalogue arrive on the stream
                var version = sse.next();
                assertNotNull(version);
                assertEquals("check.result", version.event());
                assertEquals("version", JsonTree.string(JsonTree.asObject(JsonTree.parse(version.data())), "check_id"));
                var login = sse.next();
                assertNotNull(login);
                assertEquals("check.result", login.event());
                var loginData = JsonTree.asObject(JsonTree.parse(login.data()));
                assertEquals("login", JsonTree.string(loginData, "check_id"));
                assertEquals("fail", JsonTree.string(loginData, "result"));

                // 3. the state moves on its own; the stale link of the left state is refused act_not_available
                waitForState(runtime, "wait.moved");
                var stale = client.send(HttpRequest.json("POST", "/api/v1/machine/links/yes", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(409, stale.status());
                var error = JsonTree.asObject(JsonTree.parse(stale.body()));
                assertEquals("act_not_available", JsonTree.string(error, "error"));
                assertEquals("wait.moved", JsonTree.string(error, "state"));

                // a request that was never offered is still mock_no_route
                var unknown = client.send(HttpRequest.json("POST", "/api/v1/machine/links/never", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(404, unknown.status());
                assertEquals("mock_no_route",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(unknown.body())), "error"));
            }
        }
    }

    @Test
    @DisplayName("loads the scenario and validates its operator representation against the pm-api codecs")
    void scenarioValidatesAgainstTheContract() {
        // the delivered scenario decodes with the operator recording codecs
        var scenario = ScenarioLoader.load(StandardScenarios.OPERATOR_INTERACTION);
        assertEquals("operator-interaction", scenario.name());
        assertEquals("wait.choose", scenario.start());
    }

    @Test
    @DisplayName("a scenario link no operator state offers, or an event no catalogue defines, fails the load")
    void invalidOperatorScenarioIsRefused() {
        var linkNotOffered = """
                {"format_version":1,"name":"bad","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state_id":"onboarding.choose","links":[]},
                 "links":{"yes":{"next":"a.b"}}}}}
                """;
        assertThrows(ScenarioValidationException.class, () -> ScenarioLoader.load(linkNotOffered, "bad-link"));

        var unknownEvent = """
                {"format_version":1,"name":"bad","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state_id":"onboarding.choose","links":[]},
                 "events":[{"after_ms":1,"event":"check","data":{}}]}}}
                """;
        assertThrows(ScenarioValidationException.class, () -> ScenarioLoader.load(unknownEvent, "bad-event"));

        var unknownRelation = """
                {"format_version":1,"name":"bad","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state_id":"onboarding.choose",
                 "links":[{"id":"yes","rel":"nope","method":"POST","href":"/x"}]}}}}
                """;
        assertThrows(ScenarioValidationException.class, () -> ScenarioLoader.load(unknownRelation, "bad-rel"));
    }

    private static void waitForState(MockRuntime runtime, String state) {
        var deadline = System.nanoTime() + 5_000_000_000L;
        while (!state.equals(runtime.currentStateName()) && System.nanoTime() < deadline) {
            LockSupport.parkNanos(20_000_000L);
        }
        assertEquals(state, runtime.currentStateName());
    }
}
