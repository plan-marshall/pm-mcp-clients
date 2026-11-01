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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.ScenarioLoader;
import de.planmarshall.mock.scenario.ScenarioValidationException;

@DisplayName("Operator interaction scenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
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
                assertEquals("wait.check", JsonTree.string(JsonTree.object(check, "state"), "state_id"));

                // the state's operator links are keyed by their id and carry the whole contract
                var links = JsonTree.asArray(check.get("links"));
                assertEquals(1, links.size());
                var retry = JsonTree.asObject(links.getFirst());
                assertEquals("retry", JsonTree.string(retry, "id"));
                assertEquals("retry", JsonTree.string(retry, "rel"));
                assertEquals("POST", JsonTree.string(retry, "method"));
                assertEquals("/api/v1/machine/links/retry", JsonTree.string(retry, "href"));

                // 2. the check.result events of the closed catalogue arrive as envelopes
                var version = sse.next();
                assertNotNull(version);
                assertEquals("check.result", version.event());
                var versionData = JsonTree.asObject(JsonTree.parse(version.data()));
                assertEquals("check.result", JsonTree.string(versionData, "event"));
                assertNotNull(JsonTree.string(versionData, "at"));
                assertEquals("machine", JsonTree.string(JsonTree.object(versionData, "scope"), "kind"));
                assertEquals("version", JsonTree.string(versionData, "check_id"));
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
                var unknownLink = "unknown-" + Generators.letterStrings(4, 10).next().toLowerCase();
                var unknown = client.send(HttpRequest.json("POST", "/api/v1/machine/links/" + unknownLink, "{}")
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
        var scenario = ScenarioLoader.load(StandardScenarios.ONBOARDING);
        assertEquals("onboarding", scenario.name());
        assertEquals("onboard.mode", scenario.start());
        assertTrue(scenario.states().containsKey("wait.choose"));
    }

    @Test
    @DisplayName("a scenario link no operator state offers, or an event no catalogue defines, fails the load")
    void invalidOperatorScenarioIsRefused() {
        var suffix = Generators.letterStrings(3, 8).next().toLowerCase();
        var linkNotOffered = """
                {"format_version":1,"name":"%1$s","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state":{"state_id":"onboarding.choose"},"links":[]},
                 "links":{"yes":{"next":"a.b"}}}}}
                """.formatted("bad-" + suffix);
        assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(linkNotOffered, "bad-link-" + suffix));

        var linkWithoutRoute = """
                {"format_version":1,"name":"%1$s","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state":{"state_id":"onboarding.choose"},
                 "links":[{"id":"yes","rel":"next","method":"POST","href":"/api/v1/machine/links/yes"}]},"links":{}}}}
                """.formatted("bad-" + suffix);
        assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(linkWithoutRoute, "bad-route-" + suffix));

        var unknownEvent = """
                {"format_version":1,"name":"%1$s","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state":{"state_id":"onboarding.choose"},"links":[]},
                 "events":[{"after_ms":1,"event":"check","data":{}}]}}}
                """.formatted("bad-" + suffix);
        assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(unknownEvent, "bad-event-" + suffix));

        var eventWithoutEnvelope = """
                {"format_version":1,"name":"%1$s","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state":{"state_id":"onboarding.choose"},"links":[]},
                 "events":[{"after_ms":1,"event":"check.result","data":{"check_id":"version"}}]}}}
                """.formatted("bad-" + suffix);
        assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(eventWithoutEnvelope, "bad-envelope-" + suffix));

        var unknownRelation = """
                {"format_version":1,"name":"%1$s","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},"state":{"state_id":"onboarding.choose"},
                 "links":[{"id":"yes","rel":"nope","method":"POST","href":"/api/v1/x"}]}}}}
                """.formatted("bad-" + suffix);
        assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(unknownRelation, "bad-rel-" + suffix));
    }

    private static void waitForState(MockRuntime runtime, String state) {
        var deadline = System.nanoTime() + 5_000_000_000L;
        while (!state.equals(runtime.currentStateName()) && System.nanoTime() < deadline) {
            LockSupport.parkNanos(20_000_000L);
        }
        assertEquals(state, runtime.currentStateName());
    }
}
