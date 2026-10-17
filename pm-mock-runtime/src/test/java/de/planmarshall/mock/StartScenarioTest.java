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

import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.start.IntakeDraft;
import de.planmarshall.api.start.PlanStartResult;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.SourceInput;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.StartEpicRequest;
import de.planmarshall.api.start.StartPlanRequest;

@DisplayName("StartScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartScenarioTest {

    @Test
    @DisplayName("loads the start scenario and lists the offered recipes")
    void shouldLoadScenarioAndListRecipes() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.START, runtime.currentScenarioName());
            assertEquals("start.sources", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/recipes").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var recipes = parseRecipes(res.bodyText());
            assertTrue(recipes.stream().anyMatch(r -> "agentfile-hygiene".equals(r.key())));
            assertTrue(recipes.stream().anyMatch(r -> "code-review".equals(r.key())));
            assertTrue(recipes.stream().anyMatch(r -> "lesson-cleanup".equals(r.key())));
            var audit = recipes.stream().filter(r -> "audit".equals(r.key())).findFirst().orElseThrow();
            assertTrue(audit.takesRequest(), "the audit recipe takes a request");
        }
    }

    @Test
    @DisplayName("starts a plan from a description and broadcasts plan.started")
    void shouldStartPlanFromDescription() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var request = new StartPlanRequest("pm-mcp-clients",
                        "Add a Codeberg provider for issues", null, Map.of(),
                        List.of("doc/concepts/forgejo.adoc"), "4711", null);
                var res = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/plans/start",
                        request.toJson()).withHeader("Authorization", auth));
                assertEquals(201, res.status());

                var result = PlanStartResult.fromJson(res.bodyText());
                assertEquals(PlanStartResult.KIND_PLAN, result.kind());
                assertEquals(PlanStartResult.STATE_STARTED, result.state());
                assertEquals("Add a Codeberg provider for issues", result.title());
                assertEquals("add-a-codeberg-provider-for-issues", result.id());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("plan.started", event.event());
                var data = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("add-a-codeberg-provider-for-issues", JsonTree.string(data, "plan_id"));
            }
        }
    }

    @Test
    @DisplayName("derives a recipe plan id from the recipe key and the time")
    void shouldDeriveRecipePlanId() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new StartPlanRequest("pm-mcp-clients", null, "audit", Map.of("modules", "all 9"),
                    List.of(), null, null);
            var res = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/plans/start",
                    request.toJson()).withHeader("Authorization", auth));
            assertEquals(201, res.status());

            var result = PlanStartResult.fromJson(res.bodyText());
            assertTrue(result.id().startsWith("recipe-audit-"), "the id derives from the recipe and the time: " + result.id());
        }
    }

    @Test
    @DisplayName("starts an epic and broadcasts epic.started")
    void shouldStartEpic() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var sse = exchange.events();

                var request = new StartEpicRequest("pm-mcp-clients", "Relay hardening", Boolean.FALSE, null);
                var res = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/epics/start",
                        request.toJson()).withHeader("Authorization", auth));
                assertEquals(201, res.status());

                var result = PlanStartResult.fromJson(res.bodyText());
                assertEquals(PlanStartResult.KIND_EPIC, result.kind());
                assertEquals("relay-hardening", result.id());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("epic.started", event.event());
            }
        }
    }

    @Test
    @DisplayName("refuses a start in a project that is not ready")
    void shouldRefuseNotReadyProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new StartPlanRequest("plan-marshall", "something", null, Map.of(), List.of(), null, null);
            var res = client.send(HttpRequest.json("POST", "/api/v1/projects/plan-marshall/plans/start",
                    request.toJson()).withHeader("Authorization", auth));
            assertEquals(409, res.status());

            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("project_not_ready", JsonTree.string(body, "reason"));
        }
    }

    @Test
    @DisplayName("answers 404 for a start in an unknown project")
    void shouldRefuseUnknownProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new StartPlanRequest("no-such-project", null, null, Map.of(), List.of(), null, null);
            var res = client.send(HttpRequest.json("POST", "/api/v1/projects/no-such-project/plans/start",
                    request.toJson()).withHeader("Authorization", auth));
            assertEquals(404, res.status());
        }
    }

    @Test
    @DisplayName("opens a draft intake and broadcasts intake.created")
    void shouldOpenIntakeDraft() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var sse = exchange.events();

                var body = JsonTree.write(Map.of(
                        "project_id", "pm-mcp-clients",
                        "prompt", "start from this issue",
                        "sources", List.of(new SourceInput(SourceKind.ISSUE, "4711", null).toMap())));
                var res = client.send(HttpRequest.json("POST", "/api/v1/intake/draft", body)
                        .withHeader("Authorization", auth));
                assertEquals(201, res.status());

                var draft = IntakeDraft.fromJson(res.bodyText());
                assertEquals("pm-mcp-clients", draft.projectId());
                assertEquals(1, draft.sources().size());
                assertEquals(SourceKind.ISSUE, draft.sources().getFirst().kind());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("intake.created", event.event());
            }
        }
    }

    private static List<RecipeRef> parseRecipes(String json) throws Exception {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        var arr = JsonTree.asArray(obj.get("recipes"));
        return arr.stream().map(item -> {
            try {
                return RecipeRef.fromMap(JsonTree.asObject(item));
            }
            /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
    }
}
