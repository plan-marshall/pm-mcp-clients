/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReportRecordsTest {

    @Test
    void shouldVerifyAll16ReportKinds() {
        assertEquals(16, ReportKind.values().length);
        for (ReportKind kind : ReportKind.values()) {
            assertEquals(kind, ReportKind.fromValue(kind.value()));
            assertEquals(kind, ReportKind.fromValue(kind.name()));
            assertNotNull(kind.value());
            assertFalse(kind.value().isBlank());
        }
        assertThrows(IllegalArgumentException.class, () -> ReportKind.fromValue("unknown.kind"));
    }

    @Test
    void shouldRoundTripReportFromAndFormatPath() throws Exception {
        var from1 = new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay");
        assertEquals("pm-mcp-clients › relay-hardening › fix-relay", from1.path());

        var from2 = new ReportFrom("pm-mcp-clients", null, "docs-typo");
        assertEquals("pm-mcp-clients › docs-typo", from2.path());

        var from3 = new ReportFrom("pm-mcp-core", null, null);
        assertEquals("pm-mcp-core", from3.path());

        var fullFrom = new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay",
                "PM Clients", "Relay Hardening", "Fix Relay", "/plans/fix-relay");
        var json = fullFrom.toJson();
        var parsed = ReportFrom.fromJson(json);

        assertEquals("pm-mcp-clients", parsed.projectId());
        assertEquals("relay-hardening", parsed.epicId());
        assertEquals("fix-relay", parsed.planId());
        assertEquals("PM Clients", parsed.projectTitle());
        assertEquals("Relay Hardening", parsed.epicTitle());
        assertEquals("Fix Relay", parsed.planTitle());
        assertEquals("/plans/fix-relay", parsed.detailLink());
    }

    @Test
    void shouldRoundTripReportItem() throws Exception {
        var created = Instant.parse("2026-10-16T11:20:00Z");
        var sent = Instant.parse("2026-10-16T11:22:00Z");
        var read = Instant.parse("2026-10-16T12:00:00Z");

        var item = new ReportItem(
                "rep-0142",
                new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay"),
                ReportKind.PLAN_ENDED,
                "fix-relay ended: merged (#214)",
                Map.of("plan", "fix-relay", "outcome", "merged (#214)"),
                "3 of 3 deliverables · 2 h 40 min",
                created,
                sent,
                read,
                "web",
                "iPad",
                "/api/v1/plans/fix-relay/completion"
        );

        assertTrue(item.isRead());
        assertTrue(item.isSentLater());

        var json = item.toJson();
        var parsed = ReportItem.fromJson(json);

        assertEquals("rep-0142", parsed.id());
        assertEquals(ReportKind.PLAN_ENDED, parsed.kind());
        assertEquals("fix-relay ended: merged (#214)", parsed.subject());
        assertEquals("fix-relay", parsed.subjectParams().get("plan"));
        assertEquals("3 of 3 deliverables · 2 h 40 min", parsed.body());
        assertEquals(created, parsed.createdAt());
        assertEquals(sent, parsed.sentAt());
        assertEquals(read, parsed.readAt());
        assertEquals("web", parsed.readChannel());
        assertEquals("iPad", parsed.readDevice());
        assertEquals("/api/v1/plans/fix-relay/completion", parsed.detailLink());
    }

    @Test
    void shouldRoundTripReportsPage() throws Exception {
        var created = Instant.parse("2026-10-16T11:20:00Z");
        var item = new ReportItem(
                "rep-0142",
                new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay"),
                ReportKind.PLAN_STARTED,
                "fix-relay started",
                Map.of(),
                null,
                created,
                null,
                null,
                null,
                null,
                null
        );

        var page = new ReportsPage(List.of(item), 5, created, true, 4);
        var json = page.toJson();
        var parsed = ReportsPage.fromJson(json);

        assertEquals(1, parsed.items().size());
        assertEquals(5, parsed.unreadCount());
        assertEquals(created, parsed.oldestUnreadTime());
        assertTrue(parsed.hasOlder());
        assertEquals(4, parsed.olderCount());

        // Parse from bare array
        var arrayJson = ReportItem.writeList(List.of(item));
        var parsedArray = ReportsPage.fromJson(arrayJson);
        assertEquals(1, parsedArray.items().size());
        assertEquals(1, parsedArray.unreadCount());
        assertFalse(parsedArray.hasOlder());
    }

    @Test
    void shouldRoundTripReadReportsRequestsAndResponses() throws Exception {
        var req1 = ReadReportsRequest.markAll();
        assertTrue(req1.all());
        var parsed1 = ReadReportsRequest.fromJson(req1.toJson());
        assertTrue(parsed1.all());
        assertTrue(parsed1.reportIds().isEmpty());

        var req2 = ReadReportsRequest.of(List.of("rep-0142", "rep-0143"));
        assertFalse(req2.all());
        var parsed2 = ReadReportsRequest.fromJson(req2.toJson());
        assertFalse(parsed2.all());
        assertEquals(List.of("rep-0142", "rep-0143"), parsed2.reportIds());

        var resp = new ReadReportsResponse(5, "ok");
        var parsedResp = ReadReportsResponse.fromJson(resp.toJson());
        assertEquals(5, parsedResp.markedCount());
        assertEquals("ok", parsedResp.status());
    }

    @Test
    void shouldRoundTripEvents() throws Exception {
        var created = Instant.parse("2026-10-16T11:20:00Z");
        var item = new ReportItem("rep-001", new ReportFrom("p1", null, null), ReportKind.PLAN_STARTED, "Started", Map.of(), null, created, null, null, null, null, null);

        var sentEvent = new ReportSentEvent(item);
        assertEquals("report.sent", ReportSentEvent.EVENT_NAME);
        var parsedSent = ReportSentEvent.fromJson(sentEvent.toJson());
        assertEquals("rep-001", parsedSent.report().id());

        var readEvent = new ReportReadEvent("rep-001", created, "cli", "terminal");
        assertEquals("report.read", ReportReadEvent.EVENT_NAME);
        var parsedRead = ReportReadEvent.fromJson(readEvent.toJson());
        assertEquals("rep-001", parsedRead.reportId());
        assertEquals("cli", parsedRead.channel());
        assertEquals("terminal", parsedRead.device());
    }

    @Test
    void shouldRoundTripPlanCompletionReportWithFullTokenUsage() throws Exception {
        var start = Instant.parse("2026-10-16T08:40:00Z");
        var end = Instant.parse("2026-10-16T11:20:00Z");

        var deliverables = List.of(
                new DeliverableSummary("cursor write order", "done"),
                new DeliverableSummary("a test per transport", "done"),
                new DeliverableSummary("fsync of the cursor file", "done")
        );

        var replanning = List.of(
                new ReplanningRecord("the base moved", "09:55", "TASK-5 added")
        );

        var decisions = List.of(
                new DecisionRecord("Q-003", "Rebase onto main", "rebase", "operator", "web", "iPad"),
                new DecisionRecord("Q-005", "Pick transport", "sse", "delegated", null, null)
        );

        var components = new TokenComponentRecord(310000L, 62000L, 8000L, 29000L, 3000L);
        var breakdownPhase = List.of(
                new TokenBreakdownItem("refine", 38000, 35000, 3000, 0, false, null, null),
                new TokenBreakdownItem("outline", 96000, 90000, 6000, 0, false, null, null),
                new TokenBreakdownItem("execute", 241000, 222000, 19000, 3, true, null, components),
                new TokenBreakdownItem("finalize", 37000, 33000, 4000, 0, false, null, null)
        );

        var breakdownRole = List.of(
                new TokenBreakdownItem("planner", 128000, 120000, 8000, 0, false, "claude:opus-5.5 · high", null),
                new TokenBreakdownItem("implementer", 270000, 247000, 23000, 0, false, "claude:sonnet-5.5 · medium", null),
                new TokenBreakdownItem("reviewer", 14000, 13000, 1000, 3, true, "codex:gpt-5 · low", null)
        );

        var breakdownModel = List.of(
                new TokenBreakdownItem("claude:opus-5.5", 128000, 120000, 8000, 0, false, null, null),
                new TokenBreakdownItem("claude:sonnet-5.5", 270000, 247000, 23000, 0, false, null, null),
                new TokenBreakdownItem("codex:gpt-5", 14000, 13000, 1000, 3, true, null, null)
        );

        var waste = List.of(
                new TokenWasteRecord("no_product", 18000, 4, 2, "2 tasks"),
                new TokenWasteRecord("retries", 21000, 5, 2, "retryable 1 · stall 1"),
                new TokenWasteRecord("remediation", 13000, 4, 1, "returned with findings 1")
        );

        var tokenUsage = new TokenUsageRecord(
                412000, 380000, 32000, 9600, 6, 3, 38, 41, true,
                "their worker was terminated",
                breakdownPhase, breakdownRole, breakdownModel, waste, 52000, 13
        );

        var report = new PlanCompletionReport(
                "fix-relay",
                "fix-relay",
                new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay"),
                "merged",
                "the pull request #214 was merged into main",
                214,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                "—",
                start,
                end,
                9600,
                1200,
                "you",
                deliverables,
                replanning,
                decisions,
                tokenUsage
        );

        var json = report.toJson();
        assertNotNull(json);

        var parsed = PlanCompletionReport.fromJson(json);
        assertEquals("fix-relay", parsed.planId());
        assertEquals("merged", parsed.outcome());
        assertEquals("the pull request #214 was merged into main", parsed.terminalReason());
        assertEquals(214, parsed.pullRequestNumber());
        assertEquals(3, parsed.deliverables().size());
        assertEquals("done", parsed.deliverables().getFirst().state());
        assertEquals(1, parsed.replanningRecords().size());
        assertEquals("TASK-5 added", parsed.replanningRecords().getFirst().change());
        assertEquals(2, parsed.decisionRecords().size());
        assertEquals("rebase", parsed.decisionRecords().getFirst().answer());

        assertNotNull(parsed.tokenUsage());
        assertEquals(412000, parsed.tokenUsage().totalTokens());
        assertTrue(parsed.tokenUsage().isFloor());
        assertEquals(38, parsed.tokenUsage().tasksMeasured());
        assertEquals(41, parsed.tokenUsage().tasksTotal());
        assertEquals("their worker was terminated", parsed.tokenUsage().unmeasuredReason());
        assertEquals(4, parsed.tokenUsage().byPhase().size());
        assertEquals(3, parsed.tokenUsage().byRole().size());
        assertEquals(3, parsed.tokenUsage().byModel().size());
        assertEquals(3, parsed.tokenUsage().wasteRecords().size());
        assertEquals(52000, parsed.tokenUsage().totalWasteTokens());
        assertEquals(13, parsed.tokenUsage().wasteSharePercent());

        var execPhase = parsed.tokenUsage().byPhase().get(2);
        assertEquals("execute", execPhase.label());
        assertTrue(execPhase.isFloor());
        assertEquals(3, execPhase.unmeasuredTasksCount());
        assertNotNull(execPhase.components());
        assertEquals(310000L, execPhase.components().inputTokens());
    }

    @Test
    void shouldHandleMinimalPlanCompletionReportWithoutTokens() throws Exception {
        var start = Instant.parse("2026-10-16T08:40:00Z");
        var end = Instant.parse("2026-10-16T09:00:00Z");

        var report = new PlanCompletionReport(
                "docs-typo",
                "docs-typo",
                new ReportFrom("pm-mcp-clients", null, "docs-typo"),
                "no_change",
                null,
                null,
                null,
                null,
                start,
                end,
                1200,
                0,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        var json = report.toJson();
        var parsed = PlanCompletionReport.fromJson(json);

        assertEquals("docs-typo", parsed.planId());
        assertEquals("no_change", parsed.outcome());
        assertNull(parsed.tokenUsage());
        assertTrue(parsed.deliverables().isEmpty());
    }

    @Test
    void shouldFormatTokensAccordingToDesignSpecification() {
        // Numbers from texts-and-languages.adoc#numbers
        assertEquals("412", TokenFormat.formatTokens(412, false, "en"));
        assertEquals("412", TokenFormat.formatTokens(412, false, "de"));

        assertEquals("4.2 k", TokenFormat.formatTokens(4200, false, "en"));
        assertEquals("4,2 Tsd.", TokenFormat.formatTokens(4200, false, "de"));

        assertEquals("830 k", TokenFormat.formatTokens(830000, false, "en"));
        assertEquals("830 Tsd.", TokenFormat.formatTokens(830000, false, "de"));

        assertEquals("1.25 M", TokenFormat.formatTokens(1245000, false, "en"));
        assertEquals("1,25 Mio.", TokenFormat.formatTokens(1245000, false, "de"));

        // Floor rounding: rounded down at the precision shown
        assertEquals("≥ 412 k", TokenFormat.formatTokens(412680, true, "en"));
        assertEquals("≥ 412 Tsd.", TokenFormat.formatTokens(412680, true, "de"));

        // Split formatting
        assertEquals("≥ 412 k (≥ 380 k in · ≥ 32 k out)",
                TokenFormat.formatSplit(412000, 380000, 32000, true, "en"));
        assertEquals("≥ 412 Tsd. (≥ 380 Tsd. rein · ≥ 32 Tsd. raus)",
                TokenFormat.formatSplit(412000, 380000, 32000, true, "de"));
    }
}

