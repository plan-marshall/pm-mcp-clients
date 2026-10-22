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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReadReportsRequest;
import de.planmarshall.api.report.ReadReportsResponse;
import de.planmarshall.api.report.ReportKind;
import de.planmarshall.api.report.ReportsPage;

@DisplayName("ReportsScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ReportsScenarioTest {

    @Test
    @DisplayName("loads reports scenario and serves paged unread reports")
    void shouldLoadScenarioAndListReports() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.REPORTS_AND_SYSTEM, runtime.currentScenarioName());
            assertEquals("reports.unread", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/reports").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var page = ReportsPage.fromJson(res.bodyText());
            assertEquals(5, page.items().size());
            assertEquals(7, page.unreadCount());
            assertTrue(page.hasOlder());
            assertEquals(2, page.olderCount());
            assertNotNull(page.oldestUnreadTime());

            var first = page.items().get(0);
            assertTrue("rep-0143".equals(first.id()) || "rep-0144".equals(first.id()));

            // Check item sent significantly later
            var typo = page.items().stream().filter(r -> "rep-0139".equals(r.id())).findFirst().orElseThrow();
            assertTrue(typo.isSentLater());
            assertEquals(ReportKind.PLAN_ENDED, typo.kind());
        }
    }

    @Test
    @DisplayName("pages older reports with ?older=true")
    void shouldPageOlderReports() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/reports?older=true").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var page = ReportsPage.fromJson(res.bodyText());
            assertEquals(2, page.items().size());
            assertFalse(page.hasOlder());
            assertEquals("rep-0120", page.items().get(0).id());
            assertEquals("rep-0110", page.items().get(1).id());
        }
    }

    @Test
    @DisplayName("filters reports by project query parameter")
    void shouldFilterReportsByProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/reports?project=pm-mcp-core").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var page = ReportsPage.fromJson(res.bodyText());
            assertEquals(2, page.items().size());
            assertTrue(page.items().stream().allMatch(r -> "pm-mcp-core".equals(r.from().projectId())));
        }
    }

    @Test
    @DisplayName("marks single report read and broadcasts SSE event")
    void shouldMarkSingleReportReadAndBroadcastEvent() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var res = client.send(HttpRequest.json("POST", "/api/v1/reports/rep-0142/read", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("report.read", event.event());
                var eventData = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("rep-0142", JsonTree.string(eventData, "report_id"), event.data());

                // Verify report leaves unread list
                var listRes = client.send(HttpRequest.get("/api/v1/reports").withHeader("Authorization", auth));
                var page = ReportsPage.fromJson(listRes.bodyText());
                assertFalse(page.items().stream().anyMatch(r -> "rep-0142".equals(r.id())));

                // Second call returns already_read
                var secondRes = client.send(HttpRequest.json("POST", "/api/v1/reports/rep-0142/read", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, secondRes.status());
                var secondData = JsonTree.asObject(JsonTree.parse(secondRes.body()));
                assertEquals("already_read", JsonTree.string(secondData, "status"));
            }
        }
    }

    @Test
    @DisplayName("marks all unread reports read via POST /api/v1/reports/read")
    void shouldMarkAllReportsRead() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var reqBody = ReadReportsRequest.markAll().toJson();
            var res = client.send(HttpRequest.json("POST", "/api/v1/reports/read", reqBody)
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var readResp = ReadReportsResponse.fromJson(res.bodyText());
            assertEquals(7, readResp.markedCount());

            // Listing is now empty
            var listRes = client.send(HttpRequest.get("/api/v1/reports").withHeader("Authorization", auth));
            var page = ReportsPage.fromJson(listRes.bodyText());
            assertEquals(0, page.unreadCount());
            assertTrue(page.items().isEmpty());
        }
    }

    @Test
    @DisplayName("serves plan completion report with full token breakdown")
    void shouldServePlanCompletionReport() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/fix-relay/completion")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var report = PlanCompletionReport.fromJson(res.bodyText());
            assertEquals("fix-relay", report.planId());
            assertEquals("merged", report.outcome());
            assertEquals(214, report.pullRequestNumber());
            assertEquals(3, report.deliverables().size());
            assertEquals(1, report.replanningRecords().size());
            assertEquals(2, report.decisionRecords().size());

            assertNotNull(report.tokenUsage());
            assertEquals(412000L, report.tokenUsage().totalTokens());
            assertTrue(report.tokenUsage().isFloor());
            assertEquals("their worker was terminated", report.tokenUsage().unmeasuredReason());
            assertEquals(4, report.tokenUsage().byPhase().size());
            assertEquals(3, report.tokenUsage().byRole().size());
            assertEquals(3, report.tokenUsage().byModel().size());
            assertEquals(3, report.tokenUsage().wasteRecords().size());
            assertEquals(52000L, report.tokenUsage().totalWasteTokens());
            assertEquals(13, report.tokenUsage().wasteSharePercent());
        }
    }

    @Test
    @DisplayName("serves a completion report with the plan's own calls and a change map")
    void shouldServeOwnCallsAndChangeMap() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/fix-relay/completion")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var report = PlanCompletionReport.fromJson(res.bodyText());
            assertEquals(3, report.ownCalls().size());
            var shortcut = report.ownCalls().getFirst();
            assertEquals("shortcut", shortcut.kind());
            assertEquals("the single-subscriber path only", shortcut.limit());
            assertEquals("a second subscriber connects", shortcut.endsWhen());
            assertEquals("conflicts", shortcut.judgment());
            assertEquals("a test per transport, websocket and SSE", shortcut.conflictTarget());
            assertNotNull(report.changeMap());
            assertEquals("partial", report.changeMap().state());
            assertEquals("the vendored tree is not tracked", report.changeMap().cause());
            assertTrue(report.changeMap().diagram().contains("cursor"), report.changeMap().diagram());
        }
    }

    @Test
    @DisplayName("serves a completion report without own calls or a change map")
    void shouldServeCompletionReportWithoutOwnCalls() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/docs-typo/completion")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var report = PlanCompletionReport.fromJson(res.bodyText());
            assertEquals("docs-typo", report.planId());
            assertTrue(report.ownCalls().isEmpty());
            assertNull(report.changeMap());
        }
    }

    @Test
    @DisplayName("refuses completion report for running plan with 409 Conflict")
    void shouldRefuseCompletionReportForRunningPlan() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/relay-token-store/completion")
                    .withHeader("Authorization", auth));
            assertEquals(409, res.status());

            var body = JsonTree.asObject(JsonTree.parse(res.body()));
            assertEquals("plan_running", JsonTree.string(body, "error"));
            assertTrue(JsonTree.string(body, "message").contains("running"));
        }
    }

    @Test
    @DisplayName("serves the aborted and the failed completion report from the scenario fixtures")
    void shouldServeTheAbortedAndFailedCompletionReports() throws Exception {
        try (var aborted = MockRuntime.start(StandardScenarios.REPORTS_AND_SYSTEM + "#reports.completion-aborted")) {
            var client = new UnixSocketHttpClient(aborted.socketPath());
            var res = client.send(HttpRequest.get("/api/v1/plans/relay-aborted/completion")
                    .withHeader("Authorization", "Bearer " + aborted.token()));

            var report = PlanCompletionReport.fromJson(res.bodyText());
            assertEquals("aborted", report.outcome());
            assertNotNull(report.reason());
            assertEquals(18000L, report.tokenUsage().totalTokens());
        }

        try (var failed = MockRuntime.start(StandardScenarios.REPORTS_AND_SYSTEM + "#reports.completion-failed")) {
            var client = new UnixSocketHttpClient(failed.socketPath());
            var res = client.send(HttpRequest.get("/api/v1/plans/relay-failed/completion")
                    .withHeader("Authorization", "Bearer " + failed.token()));

            var report = PlanCompletionReport.fromJson(res.bodyText());
            assertEquals("failed", report.outcome());
            assertTrue(report.tokenUsage().isFloor());
            assertEquals("the reviewer's harness reported no tokens", report.tokenUsage().unmeasuredReason());
        }
    }

    @Test
    @DisplayName("serves an empty inbox from the reports.empty state fixture")
    void shouldServeTheEmptyInboxState() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS_AND_SYSTEM + "#reports.empty")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var res = client.send(HttpRequest.get("/api/v1/reports")
                    .withHeader("Authorization", "Bearer " + runtime.token()));

            var page = ReportsPage.fromJson(res.bodyText());
            assertEquals(0, page.unreadCount());
            assertTrue(page.items().isEmpty());
        }
    }

    @Test
    @DisplayName("returns 404 for non-existent report or completion report")
    void shouldReturn404ForNonExistentResources() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res1 = client.send(HttpRequest.get("/api/v1/reports/rep-unknown").withHeader("Authorization", auth));
            assertEquals(404, res1.status());

            var res2 = client.send(HttpRequest.json("POST", "/api/v1/reports/rep-unknown/read", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(404, res2.status());

            var res3 = client.send(HttpRequest.get("/api/v1/plans/plan-unknown/completion")
                    .withHeader("Authorization", auth));
            assertEquals(404, res3.status());
        }
    }

    @Test
    @DisplayName("filters reports by project=. dot alias and fetches single report")
    void shouldFilterByDotAliasAndFetchSingleReport() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.REPORTS)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/reports?project=.").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var page = ReportsPage.fromJson(res.bodyText());
            assertTrue(page.items().stream().allMatch(r -> "pm-mcp-clients".equals(r.from().projectId())));

            var singleRes = client.send(HttpRequest.get("/api/v1/reports/rep-0142").withHeader("Authorization", auth));
            assertEquals(200, singleRes.status());
            var singleData = JsonTree.asObject(JsonTree.parse(singleRes.body()));
            assertEquals("rep-0142", JsonTree.string(singleData, "id"));

            // Mark specific reports read
            var markReq = ReadReportsRequest.of(List.of("rep-0142", "rep-0139")).toJson();
            var markRes = client.send(HttpRequest.json("POST", "/api/v1/reports/read", markReq)
                    .withHeader("Authorization", auth));
            assertEquals(200, markRes.status());
            var markResp = ReadReportsResponse.fromJson(markRes.bodyText());
            assertEquals(2, markResp.markedCount());
        }
    }
}
