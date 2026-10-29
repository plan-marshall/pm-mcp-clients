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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * The plan detail contract of CLI-UC-F-13: {@code GET /api/v1/plans/{id}/process} answers the plan
 * the id names, and an id the scenario does not know is a {@code 404 plan_not_found} rather than a
 * copy of another plan.
 */
@DisplayName("PlanDetailTest")
class PlanDetailTest {

    private static final String AUTH = "Bearer test-token";

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
                            JsonTree.string(JsonTree.object(dropLegacy, "project"), "project_id")));

            var unknown = answer(engine, "/api/v1/plans/nope/process");
            assertEquals(404, unknown.status(), "an unknown plan id answers 404");
            assertNotNull(unknown.json());
            assertEquals("plan_not_found", unknown.json().get("error"));
        }
    }

    private static MockRuntimeEngine engine() {
        var registry = new ScenarioRegistry();
        return new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.RUNNING_WORK),
                "test-token", "test-secret");
    }

    private static Map<String, Object> get(MockRuntimeEngine engine, String path) throws IOException {
        var answer = answer(engine, path);
        assertEquals(200, answer.status(), path + " must answer 200");
        return answer.json();
    }

    private static Response answer(MockRuntimeEngine engine, String path) throws IOException {
        var request = new MockHttpRequest("GET", path, Map.of("Authorization", AUTH), new byte[0],
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
