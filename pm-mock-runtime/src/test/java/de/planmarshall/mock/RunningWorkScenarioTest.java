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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.EndedOutcome;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.overview.ProjectSummary;

@DisplayName("RunningWorkScenarioTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class RunningWorkScenarioTest {

    private static final String AUTH_HEADER = "Authorization";

    @Test
    @DisplayName("loads the consolidated scenario and serves the sorted projects list")
    void shouldLoadScenarioAndListProjects() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.RUNNING_WORK, runtime.currentScenarioName());
            assertEquals("running_work.active", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader(AUTH_HEADER, auth));
            assertEquals(200, res.status());

            var overview = ProjectOverviewResponse.fromJson(new String(res.body()));
            var projects = overview.projects();
            assertEquals(3, projects.size());

            var clients = projects.getFirst();
            assertEquals("pm-mcp-clients", clients.projectId());
            assertEquals(2, clients.needsOperatorCount());
            assertEquals("decision", clients.needsOperatorSummary());
            assertFalse(clients.setupNeedsOperator());

            var marshall = projects.get(1);
            assertEquals("plan-marshall", marshall.projectId());
            assertTrue(marshall.setupNeedsOperator());
            assertEquals("setup", marshall.needsOperatorSummary());

            var core = projects.get(2);
            assertEquals("pm-mcp-core", core.projectId());
            assertEquals(0, core.needsOperatorCount());
            assertNull(core.needsOperatorSummary());
        }
    }

    @Nested
    @DisplayName("A rich workspace")
    class RunningWorkspace {

        @Test
        @DisplayName("serves project detail with plans, epics, alongside landings, and the off-track worker")
        void shouldServeProjectDetail() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients")
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, res.status());

                var detail = ProjectDetailResponse.fromJson(new String(res.body()));
                assertEquals("pm-mcp-clients", detail.projectId());
                assertEquals(3, detail.plans().size());
                assertEquals(1, detail.epics().size());
                assertEquals(2, detail.alongsideLandings().size());

                var epic = detail.epics().getFirst();
                assertEquals("relay-hardening", epic.epicId());
                assertEquals(1, epic.runningPlansCount());
                assertEquals(3, epic.stagedPlansCount());
                assertEquals(1, epic.plans().size());

                // A worker off track is shown with its signal, apart from what needs the operator.
                var offTrack = detail.plans().stream()
                        .filter(plan -> "fix-relay".equals(plan.planId()))
                        .findFirst().orElseThrow();
                assertEquals("repeating", offTrack.offTrackSignal());
                assertNull(offTrack.needsOperatorKind());

                // A paused plan is counted in no attention badge.
                var paused = detail.plans().stream()
                        .filter(plan -> "relay-metrics".equals(plan.planId()))
                        .findFirst().orElseThrow();
                assertEquals("paused", paused.phaseState());
                assertNull(paused.needsOperatorKind());
                assertNull(paused.needsOperatorCount());

                // The decision that needs the operator names it.
                var waiting = detail.plans().stream()
                        .filter(plan -> "drop-legacy-api".equals(plan.planId()))
                        .findFirst().orElseThrow();
                assertEquals("waiting_for_you", waiting.phaseState());
                assertEquals("decision", waiting.needsOperatorKind());
                assertEquals(1, waiting.needsOperatorCount());

                var failed = detail.alongsideLandings().stream()
                        .filter(landing -> "failed".equals(landing.state()))
                        .findFirst().orElseThrow();
                assertEquals("remediation.ci-failure", failed.unit());
                assertEquals("landing_failed", failed.holdKind());
            }
        }

        @Test
        @DisplayName("serves ended work with pagination and an outcome for every ended plan")
        void shouldServeEndedWorkPage() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/ended")
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, res.status());

                var page = EndedWorkPage.fromJson(new String(res.body()));
                assertEquals("pm-mcp-clients", page.projectId());
                assertEquals(5, page.items().size());
                assertEquals(1, page.page());
                assertEquals(3, page.totalPages());
                assertFalse(page.hasNewer());
                assertTrue(page.hasOlder());

                var first = page.items().getFirst();
                assertEquals("fix-relay-v1", first.id());
                assertEquals(EndedOutcome.MERGED, EndedOutcome.fromValue(first.outcome()));
                assertEquals(214, first.pullRequestNumber());

                var page2 = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/ended?page=2")
                        .withHeader(AUTH_HEADER, auth));
                var second = EndedWorkPage.fromJson(new String(page2.body()));
                assertEquals(2, second.page());
                assertTrue(second.hasNewer());
                assertTrue(second.hasOlder());
                assertEquals("arch-overhaul", second.items().getFirst().id());
            }
        }

        @Test
        @DisplayName("broadcasts overview SSE events over the runtime events stream")
        void shouldBroadcastOverviewEvents() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                             .withHeader(AUTH_HEADER, auth))) {
                    assertTrue(exchange.isEventStream());
                    var sse = exchange.events();
                    Thread.sleep(50);

                    var broadcastBody = JsonTree.write(Map.of(
                            "event", "task.off_track",
                            "data", Map.of("plan_id", "fix-relay", "signal", "repeating",
                                    "off_track", true)));
                    var broadcastRes = client.send(HttpRequest.json("POST", "/mock/broadcast", broadcastBody));
                    assertEquals(200, broadcastRes.status());

                    var event = assertTimeoutPreemptively(Duration.ofSeconds(5), sse::next);
                    assertNotNull(event);
                    assertEquals("task.off_track", event.event());
                    var parsed = JsonTree.asObject(JsonTree.parse(event.data()));
                    assertEquals("fix-relay", JsonTree.string(parsed, "plan_id"));
                    assertEquals("repeating", JsonTree.string(parsed, "signal"));
                    assertNotNull(JsonTree.string(parsed, "at"));
                    assertNotNull(JsonTree.object(parsed, "scope"));
                }
            }
        }
    }

    @Nested
    @DisplayName("The other overview states")
    class OverviewStates {

        @Test
        @DisplayName("an onboarded machine with no project serves an empty overview")
        void emptyMachine() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#overview.empty")) {
                var overview = ProjectOverviewResponse.fromJson(
                        get(runtime, "/api/v1/projects").bodyText());
                assertTrue(overview.projects().isEmpty());

                var decisions = JsonTree.asArray(JsonTree.parse(
                        get(runtime, "/api/v1/decisions").bodyText()));
                assertNotNull(decisions);
                assertTrue(decisions.isEmpty());

                var plans = JsonTree.asObject(JsonTree.parse(
                        get(runtime, "/api/v1/plans").bodyText()));
                assertNotNull(plans);
                assertTrue(JsonTree.asArray(plans.get("plans")).isEmpty());
            }
        }

        @Test
        @DisplayName("a project whose setup asks one question names only the setup")
        void setupOnly() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#overview.setup-only")) {
                var overview = ProjectOverviewResponse.fromJson(
                        get(runtime, "/api/v1/projects").bodyText());
                assertEquals(1, overview.projects().size());
                var project = overview.projects().getFirst();
                assertEquals("plan-marshall", project.projectId());
                assertTrue(project.setupNeedsOperator());
                assertEquals("setup", project.needsOperatorSummary());

                var decisions = JsonTree.asArray(JsonTree.parse(
                        get(runtime, "/api/v1/decisions").bodyText()));
                assertEquals(1, decisions.size());
                assertEquals("Q-031", JsonTree.string(JsonTree.asObject(decisions.getFirst()), "id"));
            }
        }

        @Test
        @DisplayName("a plan ending while selected leaves every outcome the ended list can carry")
        void planEnding() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#overview.plan-ending")) {
                var detail = ProjectDetailResponse.fromJson(
                        get(runtime, "/api/v1/projects/pm-mcp-clients").bodyText());
                var ended = detail.plans().stream()
                        .filter(plan -> "drop-legacy-api".equals(plan.planId()))
                        .findFirst().orElseThrow();
                assertEquals("ended", ended.phaseState());

                var page = EndedWorkPage.fromJson(
                        get(runtime, "/api/v1/projects/pm-mcp-clients/ended").bodyText());
                var outcomes = page.items().stream()
                        .map(item -> item.outcome() != null ? item.outcome() : item.terminalReason())
                        .toList();
                assertTrue(outcomes.containsAll(List.of(
                        "merged", "no_change", "aborted", "handed_back", "cancelled", "abandoned",
                        "released", "completed")));
            }
        }
    }

    @Nested
    @DisplayName("The stable order")
    class TheStableOrder {

        @Test
        @DisplayName("serves projects that need the operator first, then the oldest active work")
        void projectsInStableOrder() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                // pm-mcp-clients and plan-marshall need the operator; of the rest, pm-mcp-core's
                // oldest active work (09:38) started before plan-marshall's (14:38).
                assertEquals(List.of("pm-mcp-clients", "plan-marshall", "pm-mcp-core"), projectIds(client, auth));

                var gotoRes = client.send(HttpRequest.json("POST", "/mock/goto/overview.needs-changed", "{}"));
                assertEquals(200, gotoRes.status());

                // plan-marshall answered its setup, so it falls behind pm-mcp-core, whose work
                // started first.
                assertEquals(List.of("pm-mcp-clients", "pm-mcp-core", "plan-marshall"), projectIds(client, auth));
            }
        }

        @Test
        @DisplayName("an activity event moves no row")
        void activityMovesNoRow() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var before = planIds(client, auth);
                assertEquals(List.of("drop-legacy-api", "fix-relay", "relay-metrics"), before);

                var broadcast = JsonTree.write(Map.of(
                        "event", "activity.changed",
                        "data", Map.of("plan_id", "relay-metrics", "phase", "execute")));
                assertEquals(200, client.send(HttpRequest.json("POST", "/mock/broadcast", broadcast)).status());

                assertEquals(before, planIds(client, auth));
            }
        }

        @Test
        @DisplayName("a row moves when its item stops needing the operator and another starts")
        void needsChangeMovesTheRow() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(List.of("drop-legacy-api", "fix-relay", "relay-metrics"), planIds(client, auth));

                var gotoRes = client.send(HttpRequest.json("POST", "/mock/goto/overview.needs-changed", "{}"));
                assertEquals(200, gotoRes.status());

                // relay-metrics now needs the operator and stands first; drop-legacy-api answered
                // its decision and falls behind fix-relay, which started later.
                assertEquals(List.of("relay-metrics", "fix-relay", "drop-legacy-api"), planIds(client, auth));
            }
        }

        @Test
        @DisplayName("a row moves when its item starts")
        void startedRowMoves() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(List.of("session-log", "profile-loader"),
                        planIdsOf(client, auth, "pm-mcp-core"));

                var gotoRes = client.send(HttpRequest.json("POST", "/mock/goto/overview.plan-started", "{}"));
                assertEquals(200, gotoRes.status());

                // The plan that just started is the newest, so it stands first.
                assertEquals(List.of("nightly-reports", "session-log", "profile-loader"),
                        planIdsOf(client, auth, "pm-mcp-core"));
            }
        }

        @Test
        @DisplayName("a row moves when its item ends")
        void endedRowMoves() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(List.of("drop-legacy-api", "fix-relay", "relay-metrics"), planIds(client, auth));

                var gotoRes = client.send(HttpRequest.json("POST", "/mock/goto/overview.plan-ending", "{}"));
                assertEquals(200, gotoRes.status());

                // An ended row is no running work: it stands after the active rows.
                assertEquals(List.of("fix-relay", "relay-metrics", "drop-legacy-api"), planIds(client, auth));
            }
        }

        @Test
        @DisplayName("serves a measured size of change on a plan and none on a plan without one")
        void servesTheChangeSize() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var detail = JsonTree.asObject(JsonTree.parse(
                        get(runtime, "/api/v1/projects/pm-mcp-clients").bodyText()));
                var plans = JsonTree.asArray(detail.get("plans"));

                var measured = JsonTree.asObject(plans.get(1));
                assertEquals("fix-relay", JsonTree.string(measured, "plan_id"));
                var change = JsonTree.asObject(measured.get("change"));
                assertNotNull(change, "a plan of a measured scenario carries change");
                assertEquals(12, JsonTree.integer(change, "files"));
                assertEquals(340, JsonTree.integer(change, "insertions"));
                assertEquals(27, JsonTree.integer(change, "deletions"));
                assertEquals(2, JsonTree.integer(change, "unplanned"));

                var unmeasured = JsonTree.asObject(plans.get(0));
                assertEquals("drop-legacy-api", JsonTree.string(unmeasured, "plan_id"));
                assertNull(unmeasured.get("change"), "a plan with no capture carries no size");
            }
        }

        private static List<String> planIds(UnixSocketHttpClient client, String auth) throws Exception {
            return planIdsOf(client, auth, "pm-mcp-clients");
        }

        private static List<String> planIdsOf(UnixSocketHttpClient client, String auth, String projectId)
                throws Exception {
            var res = client.send(HttpRequest.get("/api/v1/projects/" + projectId)
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());
            return ProjectDetailResponse.fromJson(new String(res.body())).plans().stream()
                    .map(ActivePlanSummary::planId)
                    .toList();
        }

        private static List<String> projectIds(UnixSocketHttpClient client, String auth) throws Exception {
            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            return ProjectOverviewResponse.fromJson(new String(res.body())).projects().stream()
                    .map(ProjectSummary::projectId)
                    .toList();
        }
    }

    @Test
    @DisplayName("handles dynamic fallback routes for unknown projects and ended work pages")
    void shouldHandleFallbackRoutes() throws Exception {
        var unknownProject = Generators.letterStrings(4, 12).next().toLowerCase();
        var invalidPage = Generators.letterStrings(4, 8).next().toLowerCase();
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var resUnknown = client.send(HttpRequest.get("/api/v1/projects/" + unknownProject)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(200, resUnknown.status());
            var detailUnknown = ProjectDetailResponse.fromJson(new String(resUnknown.body()));
            assertEquals(unknownProject, detailUnknown.projectId());
            assertTrue(detailUnknown.plans().isEmpty());

            var resEndedUnknown = client.send(HttpRequest.get("/api/v1/projects/" + unknownProject + "/ended")
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(200, resEndedUnknown.status());
            var endedUnknown = EndedWorkPage.fromJson(new String(resEndedUnknown.body()));
            assertEquals(unknownProject, endedUnknown.projectId());
            assertTrue(endedUnknown.items().isEmpty());

            var resInvalidPage = client.send(HttpRequest.get(
                    "/api/v1/projects/pm-mcp-clients/ended?page=" + invalidPage)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(1, EndedWorkPage.fromJson(new String(resInvalidPage.body())).page());
        }
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
