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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;


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

/**
 * The scoped Server-Sent Events streams of the mock runtime: filtering by scope, replay after
 * {@code Last-Event-ID}, and the {@code reset} envelope for a stale id or after {@code /mock/reset}.
 */
@DisplayName("Scoped event streams")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class EventStreamScopeTest {

    @Test
    @DisplayName("delivers a plan event to the runtime-wide and its own plan stream, not to another plan")
    void filtersByScope() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var firstPlan = "p-" + Generators.integers(1, 99).next();
            var secondPlan = "p-" + Generators.integers(100, 199).next();

            try (var machine = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth));
                 var firstPlanStream = client.open(HttpRequest.get("/api/v1/plans/" + firstPlan + "/events")
                         .withHeader("Authorization", auth));
                 var secondPlanStream = client.open(HttpRequest.get("/api/v1/plans/" + secondPlan + "/events")
                         .withHeader("Authorization", auth))) {
                assertTrue(machine.isEventStream());
                var machineEvents = machine.events();
                var firstPlanEvents = firstPlanStream.events();
                var secondPlanEvents = secondPlanStream.events();

                runtime.engine().broadcast("phase.changed", Map.of(
                        "plan_id", firstPlan, "phase", "execute", "project_id", "pm-mcp-clients"));

                assertEquals("phase.changed", machineEvents.next().event());
                var scoped = firstPlanEvents.next();
                assertEquals("phase.changed", scoped.event());
                var data = JsonTree.asObject(JsonTree.parse(scoped.data()));
                assertEquals(firstPlan, JsonTree.string(data, "plan_id"));
                assertEquals("plan", JsonTree.string(JsonTree.object(data, "scope"), "kind"));

                // The unrelated plan stream sees only its own event.
                runtime.engine().broadcast("phase.changed", Map.of("plan_id", secondPlan, "phase", "refine"));
                var other = secondPlanEvents.next();
                assertEquals(secondPlan, JsonTree.string(JsonTree.asObject(JsonTree.parse(other.data())), "plan_id"));
            }
        }
    }

    @Test
    @DisplayName("replays the events after Last-Event-ID in order")
    void replaysAfterLastEventId() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            String lastId;
            var firstCheck = "first-" + Generators.letterStrings(4, 10).next().toLowerCase();
            var secondCheck = "second-" + Generators.letterStrings(4, 10).next().toLowerCase();
            var thirdCheck = "third-" + Generators.letterStrings(4, 10).next().toLowerCase();
            try (var first = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var events = first.events();
                runtime.engine().broadcast("check.result", Map.of("check_id", firstCheck, "result", "pass"));
                var seen = events.next();
                assertEquals("check.result", seen.event());
                assertNotNull(seen.id());
                lastId = seen.id();
            }

            runtime.engine().broadcast("check.result", Map.of("check_id", secondCheck, "result", "fail"));
            runtime.engine().broadcast("check.result", Map.of("check_id", thirdCheck, "result", "pass"));

            try (var resumed = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", auth)
                         .withHeader("Last-Event-ID", lastId))) {
                var events = resumed.events();
                var login = events.next();
                assertEquals("check.result", login.event());
                assertEquals(secondCheck, JsonTree.string(JsonTree.asObject(JsonTree.parse(login.data())), "check_id"));
                var token = events.next();
                assertEquals(thirdCheck, JsonTree.string(JsonTree.asObject(JsonTree.parse(token.data())), "check_id"));
            }
        }
    }

    @Test
    @DisplayName("emits reset for an unknown Last-Event-ID and after /mock/reset")
    void emitsResetForStaleIdAndAfterReset() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var staleId = String.valueOf(Generators.integers(1000, 9999).next());
            try (var stale = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", auth)
                         .withHeader("Last-Event-ID", staleId))) {
                var reset = stale.events().next();
                assertEquals("reset", reset.event());
                assertEquals("reset", JsonTree.string(JsonTree.asObject(JsonTree.parse(reset.data())), "event"));
            }

            String lastId;
            try (var first = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var events = first.events();
                runtime.engine().broadcast("check.result", Map.of("check_id", "version", "result", "pass"));
                lastId = events.next().id();
            }

            runtime.reset();

            try (var resumed = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", auth)
                         .withHeader("Last-Event-ID", lastId))) {
                assertEquals("reset", resumed.events().next().event());
            }
        }
    }
}
