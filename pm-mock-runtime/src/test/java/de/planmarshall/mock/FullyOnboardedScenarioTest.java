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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.ChatReadBackResource;
import de.planmarshall.api.control.DecompositionResource;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.EpicTokens;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.control.TokenFigure;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.designer.WorkflowCatalogue;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.entry.ConnectionStatus;
import de.planmarshall.api.entry.DeviceListResource;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.intent.EpicIntentResource;
import de.planmarshall.api.intent.OutlineResource;
import de.planmarshall.api.intent.OverSizeDistributionResource;
import de.planmarshall.api.intent.PlanIntentResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.process.EpicProcess;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.start.ConfinedFileListResource;
import de.planmarshall.api.start.IntakeDraft;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * The fully-onboarded showroom scenario: one machine with every operator domain populated at once.
 * Every domain route the two clients read answers 200, and every answer decodes with the
 * {@code pm-api} codec of its contract.
 */
@DisplayName("FullyOnboardedScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class FullyOnboardedScenarioTest {

    private static final String AUTH_HEADER = "Authorization";

    @Test
    @DisplayName("loads from the classpath with the showroom state and pairing code")
    void shouldLoadScenario() {
        var scenario = new ScenarioRegistry()
                .resolve(StandardScenarios.FULLY_ONBOARDED);

        assertEquals(StandardScenarios.FULLY_ONBOARDED, scenario.name());
        assertEquals("showroom.active", scenario.start());
        assertEquals("KXQB-MRTN", scenario.pairingCode());
        assertTrue(scenario.states().containsKey("showroom.active"));
        assertTrue(scenario.routes().keySet().containsAll(List.of(
                "GET /api/v1/status",
                "GET /api/v1/projects",
                "GET /api/v1/overview",
                "GET /api/v1/plans",
                "GET /api/v1/epics",
                "GET /api/v1/plans/fix-relay/process",
                "GET /api/v1/epics/relay-hardening/process")));
        assertTrue(scenario.routes().keySet().containsAll(List.of(
                "GET /api/v1/workflows",
                "GET /api/v1/workflows/catalogue")), "the designer fixtures are routes");
        var representation = JsonTree.asObject(scenario.state("showroom.active").representation());
        var facts = JsonTree.asObject(representation.get("facts"));
        var runtime = JsonTree.asObject(facts.get("runtime"));
        assertNotNull(runtime.get("start"), "the start fixture is seeded in the state");
        assertNotNull(runtime.get("designer"), "the designer state carries its enrolled projects");
    }

    @Test
    @DisplayName("starts with a machine that is onboarded and its web listener open on 7420")
    void shouldStartFullyOnboarded() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            assertEquals(StandardScenarios.FULLY_ONBOARDED, runtime.currentScenarioName());
            assertEquals("showroom.active", runtime.currentStateName());

            var status = getMap(runtime, "/api/v1/status");
            assertEquals("ready", JsonTree.string(status, "status"));
            assertEquals("mock-machine", JsonTree.string(JsonTree.object(status, "machine"), "name"));
            assertEquals(Boolean.TRUE, JsonTree.bool(JsonTree.object(status, "machine"), "onboarded"));
            var web = JsonTree.object(status, "web");
            assertEquals(Boolean.TRUE, JsonTree.bool(web, "lan"));
            assertEquals(Boolean.TRUE, JsonTree.bool(web, "open"));
            assertEquals(7420, JsonTree.integer(web, "port").intValue());
        }
    }

    @Nested
    @DisplayName("Every operator domain")
    class Domains {

        @Test
        @DisplayName("answers 200 and decodes with the pm-api codec of its contract")
        void everyDomainRouteAnswersAndDecodes() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var endpoints = new ArrayList<Endpoint>();
                endpoints.add(new Endpoint("/api/v1/status/connection",
                        body -> ConnectionStatus.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/web/devices",
                        body -> DeviceListResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/machine/settings",
                        body -> MachineSettingsRecord.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects",
                        body -> ProjectOverviewResponse.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects?all=true",
                        body -> ProjectExplorer.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects/pm-mcp-clients",
                        body -> ProjectDetailResponse.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects/pm-mcp-clients/ended",
                        body -> EndedWorkPage.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects/pm-mcp-clients/files",
                        body -> ConfinedFileListResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/overview",
                        body -> ProjectOverviewResponse.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans", FullyOnboardedScenarioTest::assertPlans));
                endpoints.add(new Endpoint("/api/v1/epics", FullyOnboardedScenarioTest::assertEpics));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/process",
                        body -> ProcessTree.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/docs-typo/process",
                        body -> ProcessTree.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/tasks/TASK-003/activity",
                        body -> TaskActivity.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening/process",
                        body -> EpicProcess.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/intent",
                        body -> PlanIntentResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/outline",
                        body -> OutlineResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening/intent",
                        body -> EpicIntentResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects/pm-mcp-clients/metrics/oversize",
                        body -> OverSizeDistributionResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/decisions", FullyOnboardedScenarioTest::assertDecisions));
                endpoints.add(new Endpoint("/api/v1/decisions/Q-007",
                        body -> DecisionEnvelope.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/reports",
                        body -> ReportsPage.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/reports/rep-0142",
                        body -> ReportItem.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/completion",
                        body -> PlanCompletionReport.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/system",
                        body -> SystemSnapshot.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/system/cleanup",
                        body -> CleanupJob.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/recipes", FullyOnboardedScenarioTest::assertRecipes));
                endpoints.add(new Endpoint("/api/v1/workflows",
                        FullyOnboardedScenarioTest::assertWorkflowUnits));
                endpoints.add(new Endpoint("/api/v1/workflows/catalogue",
                        body -> WorkflowCatalogue.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/onboarding/state",
                        FullyOnboardedScenarioTest::assertJsonObject));
                endpoints.add(new Endpoint("/api/v1/entries/ent-showroom",
                        body -> StartStateResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/projects/pm-mcp-clients/sources/lessons",
                        FullyOnboardedScenarioTest::assertJsonObject));
                endpoints.add(new Endpoint("/api/v1/projects/pm-mcp-clients/epics/relay-hardening/decomposition",
                        body -> EpicDecomposition.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening/decomposition",
                        body -> DecompositionResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/actions",
                        body -> WorkActions.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/relay-cursor-order/holds",
                        FullyOnboardedScenarioTest::assertHolds));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/settings",
                        body -> PlanSettingsRecord.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/relay-reconnect-backoff/tokens",
                        body -> TokenFigure.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening/tokens",
                        body -> EpicTokens.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening",
                        body -> EpicResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening/archive-check",
                        body -> ArchiveCheck.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/epics/relay-hardening/review",
                        body -> ReviewResource.fromJson(body)));
                endpoints.add(new Endpoint("/api/v1/plans/fix-relay/chat",
                        body -> ChatReadBackResource.fromJson(body)));

                for (var endpoint : endpoints) {
                    var response = get(runtime, endpoint.path());
                    assertEquals(200, response.status(), endpoint.path() + " answered " + response.bodyText());
                    endpoint.decoder().decode(response.bodyText());
                }
            }
        }

        @Test
        @DisplayName("serves the five unread reports of the showroom inbox and its completion report")
        void reportsAndCompletion() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var page = ReportsPage.fromJson(get(runtime, "/api/v1/reports").bodyText());
                assertEquals(5, page.items().size());
                assertEquals(5, page.unreadCount());
                assertFalse(page.hasOlder());
                assertTrue(page.items().stream().anyMatch(item -> "rep-0142".equals(item.id())));

                var report = ReportItem.fromJson(
                        get(runtime, "/api/v1/reports/rep-0142").bodyText());
                assertEquals("plan.ended", report.kind().value());
                assertNotNull(report.detailLink());
            }
        }

        @Test
        @DisplayName("serves the three pending decisions with a recommendation and a dependent part")
        void decisions() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var decisions = decisionsOf(runtime);
                assertEquals(3, decisions.size());
                assertEquals(List.of("Q-007", "Q-012", "Q-015"),
                        decisions.stream().map(DecisionEnvelope::id).toList());

                var recommendation = decisions.getFirst().parts().getFirst();
                assertNotNull(recommendation.recommendation());
                var multiPart = decisions.get(1).parts();
                assertEquals(3, multiPart.size());
                assertNotNull(multiPart.get(1).dependency());
            }
        }

        @Test
        @DisplayName("serves the running epic with its three workstreams and token split")
        void epicWorkstreams() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var epic = EpicResource.fromJson(
                        get(runtime, "/api/v1/epics/relay-hardening").bodyText());
                assertEquals(3, epic.workstreams().size());
                assertEquals(List.of("WS-1", "WS-2", "WS-3"),
                        epic.workstreams().stream().map(workstream -> workstream.id()).toList());
                assertTrue(epic.tokens().total().total() >= 1_240_000L);
            }
        }

        @Test
        @DisplayName("serves the three enrolled projects, two paired devices, and the file tree")
        void projectsAndDevices() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var explorer = ProjectExplorer.fromJson(get(runtime, "/api/v1/projects?all=true").bodyText());
                assertEquals(3, explorer.allProjects().size());
                assertEquals(2, explorer.favourites().size());
                assertEquals("pm-mcp-clients", explorer.favourites().getFirst().projectId());

                var devices = DeviceListResource.fromJson(get(runtime, "/api/v1/web/devices").bodyText());
                assertEquals(2, devices.size());
                assertEquals(List.of("Primary Desktop Browser", "Terminal Companion"),
                        devices.devices().stream().map(device -> device.label()).toList());

                var files = ConfinedFileListResource.fromJson(
                        get(runtime, "/api/v1/projects/pm-mcp-clients/files").bodyText());
                assertTrue(files.entries().stream().anyMatch(file -> "pom.xml".equals(file.name())));
            }
        }

        @Test
        @DisplayName("serves the designer units and catalogue of the machine scope")
        void designer() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var units = workflowUnitsOf(runtime);
                assertFalse(units.isEmpty());
                var catalogue = WorkflowCatalogue.fromJson(
                        get(runtime, "/api/v1/workflows/catalogue").bodyText());
                assertFalse(catalogue.blocks().isEmpty());
            }
        }

        @Test
        @DisplayName("serves the start entry, its file picker, the recipes, and an intake draft")
        void start() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var entry = StartStateResource.fromJson(
                        get(runtime, "/api/v1/entries/ent-showroom").bodyText());
                assertNotNull(entry.entry());
                assertEquals("ent-showroom", entry.entry().entryId());
                assertEquals(4, entry.entry().sources().size());

                var recipes = recipesOf(runtime);
                assertTrue(recipes.stream().anyMatch(recipe -> "code-review".equals(recipe.key())));

                var draft = IntakeDraft.fromJson(post(runtime, "/api/v1/intake/draft",
                        "{\"project_id\":\"pm-mcp-clients\"}").bodyText());
                assertNotNull(draft.id());
            }
        }

        @Test
        @DisplayName("streams events on the runtime-wide and the plan-scoped SSE streams")
        void eventStreams() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                try (var machine = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH_HEADER, auth));
                     var plan = client.open(HttpRequest.get("/api/v1/plans/fix-relay/events")
                             .withHeader(AUTH_HEADER, auth))) {
                    assertTrue(machine.isEventStream());
                    var machineEvents = machine.events();
                    var planEvents = plan.events();

                    runtime.engine().broadcast("tokens.changed", Map.of(
                            "plan_id", "fix-relay", "project_id", "pm-mcp-clients",
                            "total", Generators.integers(1, 1000).next()));

                    var runtimeWide = assertTimeoutPreemptively(Duration.ofSeconds(5), machineEvents::next);
                    assertNotNull(runtimeWide);
                    assertEquals("tokens.changed", runtimeWide.event());
                    var scoped = assertTimeoutPreemptively(Duration.ofSeconds(5), planEvents::next);
                    assertNotNull(scoped);
                    assertEquals("tokens.changed", scoped.event());
                    var data = JsonTree.asObject(JsonTree.parse(scoped.data()));
                    assertEquals("fix-relay", JsonTree.string(data, "plan_id"));
                    assertEquals("plan", JsonTree.string(JsonTree.object(data, "scope"), "kind"));
                }
            }
        }
    }

    private static void assertPlans(String body) throws Exception {
        var root = JsonTree.asObject(JsonTree.parse(body));
        assertNotNull(root);
        var plans = JsonTree.asArray(root.get("plans"));
        assertNotNull(plans);
        assertEquals(5, plans.size());
        assertTrue(plans.stream().anyMatch(plan -> "fix-relay".equals(JsonTree.string(JsonTree.asObject(plan), "id"))));
    }

    private static void assertEpics(String body) throws Exception {
        var root = JsonTree.asObject(JsonTree.parse(body));
        assertNotNull(root);
        var epics = JsonTree.asArray(root.get("epics"));
        assertNotNull(epics);
        assertTrue(epics.stream().anyMatch(epic ->
                "relay-hardening".equals(JsonTree.string(JsonTree.asObject(epic), "id"))));
    }

    private static void assertDecisions(String body) throws Exception {
        assertEquals(3, decisionsFrom(body).size());
    }

    private static void assertRecipes(String body) throws Exception {
        var recipeList = recipesFrom(body);
        assertFalse(recipeList.isEmpty());
    }

    private static void assertWorkflowUnits(String body) throws Exception {
        assertFalse(workflowUnitsFrom(body).isEmpty());
    }

    private static void assertHolds(String body) throws Exception {
        var holds = JsonTree.asArray(JsonTree.parse(body));
        assertNotNull(holds);
        assertFalse(holds.isEmpty());
        for (var hold : holds) {
            HoldRecord.fromMap(JsonTree.asObject(hold));
        }
    }

    private static void assertJsonObject(String body) throws Exception {
        assertNotNull(JsonTree.asObject(JsonTree.parse(body)));
    }

    private static List<DecisionEnvelope> decisionsOf(MockRuntime runtime) throws Exception {
        return decisionsFrom(get(runtime, "/api/v1/decisions").bodyText());
    }

    private static List<DecisionEnvelope> decisionsFrom(String body) throws Exception {
        var array = JsonTree.asArray(JsonTree.parse(body));
        assertNotNull(array);
        var decisions = new ArrayList<DecisionEnvelope>();
        for (var element : array) {
            decisions.add(DecisionEnvelope.fromMap(JsonTree.asObject(element)));
        }
        return decisions;
    }

    private static List<RecipeRef> recipesOf(MockRuntime runtime) throws Exception {
        return recipesFrom(get(runtime, "/api/v1/recipes").bodyText());
    }

    private static List<RecipeRef> recipesFrom(String body) throws Exception {
        var root = JsonTree.asObject(JsonTree.parse(body));
        assertNotNull(root);
        var recipes = JsonTree.asArray(root.get("recipes"));
        assertNotNull(recipes);
        var result = new ArrayList<RecipeRef>();
        for (var element : recipes) {
            result.add(RecipeRef.fromMap(JsonTree.asObject(element)));
        }
        return result;
    }

    private static List<WorkflowUnit> workflowUnitsOf(MockRuntime runtime) throws Exception {
        return workflowUnitsFrom(get(runtime, "/api/v1/workflows").bodyText());
    }

    private static List<WorkflowUnit> workflowUnitsFrom(String body) throws Exception {
        var units = JsonTree.asArray(JsonTree.parse(body));
        assertNotNull(units);
        var result = new ArrayList<WorkflowUnit>();
        for (var element : units) {
            result.add(WorkflowUnit.fromMap(JsonTree.asObject(element)));
        }
        return result;
    }

    private static Map<String, Object> getMap(MockRuntime runtime, String path) throws Exception {
        return JsonTree.asObject(JsonTree.parse(get(runtime, path).bodyText()));
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String json) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, json).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    /**
     * One endpoint of the matrix and the codec its contract names.
     *
     * @param path    the request path
     * @param decoder the codec call the answer must survive
     */
    private record Endpoint(String path, Decoder decoder) {
    }

    /**
     * A codec call that throws when the answer does not match the contract.
     */
    @FunctionalInterface
    private interface Decoder {

        /**
         * @param body the response body
         * @throws Exception when the body is not the contract's shape
         */
        void decode(String body) throws Exception;
    }
}
