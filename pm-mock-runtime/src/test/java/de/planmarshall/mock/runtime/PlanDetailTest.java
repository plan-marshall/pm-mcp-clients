/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * The plan detail contract of CLI-UC-F-13: {@code GET /api/v1/plans/{id}/process} answers the plan
 * the id names, and an id the scenario does not know is a {@code 404 plan_not_found}. A plan the
 * scenario lists without a route of its own is answered from its own summary entry, not with
 * another plan's epic, pull request or tree.
 */
@DisplayName("PlanDetailTest")
@EnableGeneratorController
class PlanDetailTest {

    private String runtimeToken;
    private String auth;

    @BeforeEach
    void drawCredentials() {
        runtimeToken = Generators.letterStrings(8, 16).next();
        auth = "Bearer " + runtimeToken;
    }

    @Test
    @DisplayName("two plan ids serve two different plans and an unknown id answers plan_not_found")
    void servesThePlanPerId() throws Exception {
        try (var engine = engine()) {
            var fixRelay = get(engine, "/api/v1/plans/fix-relay/process");
            var dropLegacy = get(engine, "/api/v1/plans/drop-legacy-api/process");

            assertNotNull(fixRelay, "fix-relay must answer a plan");
            assertNotNull(dropLegacy, "drop-legacy-api must answer its own plan");
            assertAll("the two plans differ in their own facts",
                    () -> assertEquals("fix-relay", JsonTree.string(fixRelay, "plan_id")),
                    () -> assertEquals("drop-legacy-api", JsonTree.string(dropLegacy, "plan_id")),
                    () -> assertNotEquals(JsonTree.string(fixRelay, "title"),
                            JsonTree.string(dropLegacy, "title"), "the titles differ"),
                    () -> assertEquals("Drop the legacy endpoints",
                            JsonTree.string(dropLegacy, "title")),
                    () -> assertEquals("finalize", JsonTree.string(dropLegacy, "current_phase")),
                    () -> assertEquals("waiting_for_you", JsonTree.string(dropLegacy, "phase_state")),
                    () -> assertEquals("pm-mcp-clients",
                            JsonTree.string(JsonTree.object(dropLegacy, "project"), "project_id")),
                    () -> assertNull(dropLegacy.get("epic"), "the plan names no other plan's epic"),
                    () -> assertNull(dropLegacy.get("pull_request_number"),
                            "the plan names no other plan's pull request"),
                    () -> assertEquals("done", phaseState(dropLegacy, "execute"),
                            "the execute phase is behind a finalize plan"),
                    () -> assertEquals("waiting_for_operator", phaseState(dropLegacy, "finalize"),
                            "the finalize phase is the current one"));

            var unknownPlan = Generators.letterStrings(4, 10).next().toLowerCase();
            var unknown = answer(engine, "/api/v1/plans/" + unknownPlan + "/process");
            assertEquals(404, unknown.status(), "an unknown plan id answers 404");
            assertNotNull(unknown.json());
            assertEquals("plan_not_found", unknown.json().get("error"));

            var unknownActivity = answer(engine, "/api/v1/plans/" + unknownPlan + "/tasks/TASK-001/activity");
            assertEquals(404, unknownActivity.status(), "an unknown plan id has no task activity");
            assertEquals("plan_not_found", unknownActivity.json().get("error"));
        }
    }

    @Test
    @DisplayName("a plan listed only inside an epic answers its own process")
    void servesAPlanListedInsideAnEpic() throws Exception {
        try (var engine = engine()) {
            var process = get(engine, "/api/v1/plans/relay-reconnect-backoff/process");

            assertAll("the row's own facts",
                    () -> assertEquals("relay-reconnect-backoff", JsonTree.string(process, "plan_id")),
                    () -> assertEquals("relay-reconnect-backoff", JsonTree.string(process, "title")),
                    () -> assertEquals("pm-mcp-clients",
                            JsonTree.string(JsonTree.object(process, "project"), "project_id")),
                    () -> assertEquals("relay-hardening",
                            JsonTree.string(JsonTree.object(process, "epic"), "epic_id"),
                            "the plan names the epic that lists it"),
                    () -> assertEquals("done", JsonTree.string(process, "phase_state"),
                            "the shipped row is a done plan"),
                    () -> assertNotNull(JsonTree.asArray(process.get("nodes")),
                            "the plan answers a node list"),
                    () -> assertEquals(List.of("done"), nodeStates(process),
                            "every phase of a done plan is done"));
        }
    }

    private static @Nullable String phaseState(Map<String, Object> plan, String nodeId) {
        var nodes = JsonTree.asArray(plan.get("nodes"));
        assertNotNull(nodes, "the plan must answer a node list");
        for (var element : nodes) {
            var node = JsonTree.asObject(element);
            if (node != null && nodeId.equals(JsonTree.string(node, "node_id"))) {
                return JsonTree.string(node, "state");
            }
        }
        return null;
    }

    /**
     * @param plan the plan's process representation
     * @return the distinct states of its phase nodes, in order
     */
    private static List<String> nodeStates(Map<String, Object> plan) {
        var nodes = JsonTree.asArray(plan.get("nodes"));
        assertNotNull(nodes, "the plan must answer a node list");
        var states = new LinkedHashSet<String>();
        for (var element : nodes) {
            var node = JsonTree.asObject(element);
            assertNotNull(node, "every node is an object");
            states.add(JsonTree.string(node, "state"));
        }
        return List.copyOf(states);
    }

    private MockRuntimeEngine engine() {
        var registry = new ScenarioRegistry();
        return new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.RUNNING_WORK),
                runtimeToken, Generators.letterStrings(8, 16).next());
    }

    private Map<String, Object> get(MockRuntimeEngine engine, String path) throws IOException {
        var answer = answer(engine, path);
        assertEquals(200, answer.status(), path + " must answer 200");
        return answer.json();
    }

    private Response answer(MockRuntimeEngine engine, String path) throws IOException {
        var request = new MockHttpRequest("GET", path, Map.of("Authorization", auth), new byte[0],
                System.nanoTime());
        var out = new ByteArrayOutputStream();
        engine.dispatch(request, new MockHttpResponse(out), false);
        var raw = out.toString(StandardCharsets.UTF_8);
        var split = raw.indexOf("\r\n\r\n");
        var body = split >= 0 ? raw.substring(split + 4) : "";
        var status = -1;
        var firstLine = raw.split("\r\n")[0].split(" ");
        if (firstLine.length >= 2) {
            status = Integer.parseInt(firstLine[1]);
        }
        Map<String, Object> json = null;
        if (!body.isBlank()) {
            json = JsonTree.asObject(JsonTree.parse(body));
        }
        return new Response(status, json);
    }

    private record Response(int status, Map<String, Object> json) {
    }
}
