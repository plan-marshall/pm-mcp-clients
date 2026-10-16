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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;

@DisplayName("RunningWorkScenarioTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
class RunningWorkScenarioTest {

    @Test
    @DisplayName("loads running-work scenario and serves sorted projects list")
    void shouldLoadScenarioAndListProjects() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.RUNNING_WORK, runtime.currentScenarioName());
            assertEquals("running_work.active", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var overview = ProjectOverviewResponse.fromJson(new String(res.body()));
            var projects = overview.projects();
            assertEquals(3, projects.size());

            // First two projects need operator attention
            var p1 = projects.get(0);
            var p2 = projects.get(1);
            var p3 = projects.get(2);

            assertTrue(p1.needsOperatorCount() > 0 || p1.setupNeedsOperator(), "First project must need operator");
            assertTrue(p2.needsOperatorCount() > 0 || p2.setupNeedsOperator(), "Second project must need operator");
            assertEquals("pm-mcp-core", p3.projectId(), "Third project has no waiting items");
            assertEquals(0, p3.needsOperatorCount());
        }
    }

    @Test
    @DisplayName("serves project detail for pm-mcp-clients with plans, epics, and alongside landings")
    void shouldServeProjectDetail() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var detail = ProjectDetailResponse.fromJson(new String(res.body()));
            assertEquals("pm-mcp-clients", detail.projectId());
            assertEquals(3, detail.plans().size());
            assertEquals(1, detail.epics().size());
            assertEquals(2, detail.alongsideLandings().size());

            var epic = detail.epics().getFirst();
            assertEquals("epc-client-work", epic.epicId());
            assertEquals(2, epic.runningPlansCount());
            assertEquals(3, epic.stagedPlansCount());
            assertEquals(2, epic.plans().size());

            // Check off-track signal on Message catalogue
            var offTrackPlan = detail.plans().stream()
                    .filter(p -> "pln-message-catalogue".equals(p.planId()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(offTrackPlan);
            assertEquals("repeating", offTrackPlan.offTrackSignal());

            // Check alongside landings
            var landingFailed = detail.alongsideLandings().stream()
                    .filter(l -> "failed".equals(l.state()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(landingFailed);
            assertEquals("remediation.ci-failure", landingFailed.unit());
            assertEquals("landing_failed", landingFailed.holdKind());
        }
    }

    @Test
    @DisplayName("serves ended work page with pagination for pm-mcp-clients")
    void shouldServeEndedWorkPage() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/ended").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var page = EndedWorkPage.fromJson(new String(res.body()));
            assertEquals("pm-mcp-clients", page.projectId());
            assertEquals(5, page.items().size());
            assertEquals(1, page.page());
            assertEquals(3, page.totalPages());
            assertFalse(page.hasNewer());
            assertTrue(page.hasOlder());

            var firstItem = page.items().getFirst();
            assertEquals("fix-relay", firstItem.id());
            assertEquals("merged", firstItem.outcome());
            assertEquals(214, firstItem.pullRequestNumber());

            // Test page 2 navigation
            var resPage2 = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/ended?page=2").withHeader("Authorization", auth));
            assertEquals(200, resPage2.status());
            var page2 = EndedWorkPage.fromJson(new String(resPage2.body()));
            assertEquals(2, page2.page());
            assertTrue(page2.hasNewer());
            assertTrue(page2.hasOlder());
        }
    }

    @Test
    @DisplayName("broadcasts overview SSE events over runtime events stream")
    void shouldBroadcastOverviewEvents() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                // Broadcast a task.off_track event
                var broadcastBody = JsonTree.write(Map.of(
                        "event", "task.off_track",
                        "data", Map.of("plan_id", "pln-overview-resource", "signal", "stalled_turn", "off_track", true)
                ));
                var broadcastRes = client.send(HttpRequest.json("POST", "/mock/broadcast", broadcastBody));
                assertEquals(200, broadcastRes.status());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("task.off_track", event.event());
                var parsed = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("pln-overview-resource", JsonTree.string(parsed, "plan_id"));
                assertEquals("stalled_turn", JsonTree.string(parsed, "signal"));
            }
        }
    }

    @Test
    @DisplayName("handles dynamic fallback routes for unknown projects and ended work pages")
    void shouldHandleFallbackRoutes() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // Unknown project detail fallback
            var resUnknown = client.send(HttpRequest.get("/api/v1/projects/non-existent-proj").withHeader("Authorization", auth));
            assertEquals(200, resUnknown.status());
            var detailUnknown = ProjectDetailResponse.fromJson(new String(resUnknown.body()));
            assertEquals("non-existent-proj", detailUnknown.projectId());
            assertTrue(detailUnknown.plans().isEmpty());

            // Unknown project ended work fallback
            var resEndedUnknown = client.send(HttpRequest.get("/api/v1/projects/non-existent-proj/ended").withHeader("Authorization", auth));
            assertEquals(200, resEndedUnknown.status());
            var endedUnknown = EndedWorkPage.fromJson(new String(resEndedUnknown.body()));
            assertEquals("non-existent-proj", endedUnknown.projectId());
            assertTrue(endedUnknown.items().isEmpty());

            // Non-numeric page param falls back to page 1
            var resInvalidPage = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/ended?page=not_a_num").withHeader("Authorization", auth));
            assertEquals(200, resInvalidPage.status());
            var invalidPage = EndedWorkPage.fromJson(new String(resInvalidPage.body()));
            assertEquals(1, invalidPage.page());
        }
    }
}
