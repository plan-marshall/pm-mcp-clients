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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

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

    private PlanCompletionReport createFullPlanCompletionReport() {
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

        var ownCalls = List.of(
                new OwnCallRecord("shortcut", "reuse the existing SSE adapter", "the single-subscriber path only",
                        "a second subscriber connects", "conflicts", "a test per transport, websocket and SSE"),
                new OwnCallRecord("design", "the relay writes the cursor", null, null, "consistent", null),
                new OwnCallRecord("dependency", "a file-backed cursor replaces the in-memory one", null, null,
                        "consistent", null)
        );
        var changeMap = new ChangeMapRecord("relay -> cursor -> fsync\n  +--> sse -> websocket (not traced)",
                "partial", "the vendored tree is not tracked");

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

        return new PlanCompletionReport(
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
                tokenUsage,
                ownCalls,
                changeMap
        );
    }

    @Test
    void shouldRoundTripPlanCompletionReportMetadata() throws Exception {
        var report = createFullPlanCompletionReport();
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
    }

    @Test
    void shouldRoundTripOwnCallsAndChangeMap() throws Exception {
        var parsed = PlanCompletionReport.fromJson(createFullPlanCompletionReport().toJson());

        assertEquals(3, parsed.ownCalls().size());
        var shortcut = parsed.ownCalls().getFirst();
        assertEquals("shortcut", shortcut.kind());
        assertEquals("reuse the existing SSE adapter", shortcut.description());
        assertEquals("the single-subscriber path only", shortcut.limit());
        assertEquals("a second subscriber connects", shortcut.endsWhen());
        assertEquals("conflicts", shortcut.judgment());
        assertEquals("a test per transport, websocket and SSE", shortcut.conflictTarget());
        assertNull(parsed.ownCalls().get(1).limit());

        assertNotNull(parsed.changeMap());
        assertEquals("partial", parsed.changeMap().state());
        assertEquals("the vendored tree is not tracked", parsed.changeMap().cause());
        assertEquals("relay -> cursor -> fsync\n  +--> sse -> websocket (not traced)",
                parsed.changeMap().diagram());
    }

    @Test
    void shouldRejectAnOwnCallWithoutItsRequiredMembers() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new OwnCallRecord(null, "design", null, null, "consistent", null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new OwnCallRecord("design", null, null, null, "consistent", null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new OwnCallRecord("design", "a call", null, null, null, null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new ChangeMapRecord(null, "ok", null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new ChangeMapRecord("diagram", null, null))
        );
    }

    @Test
    void shouldRejectAnOwnCallOrChangeMapMissingItsContractFields() {
        assertAll(
                () -> assertThrows(JsonFormatException.class,
                        () -> OwnCallRecord.fromMap(Map.of("description", "a call", "judgment", "consistent"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> OwnCallRecord.fromMap(Map.of("kind", "design", "description", "a call"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> OwnCallRecord.fromMap(Map.of("kind", "design", "judgment", "consistent"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> ChangeMapRecord.fromMap(Map.of("state", "ok"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> ChangeMapRecord.fromMap(Map.of("diagram", "relay -> cursor")))
        );
    }

    @Test
    void shouldRejectAnOwnCallOrChangeMapThatViolatesItsStateInvariants() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new OwnCallRecord("shortcut", "a shortcut", null, "a second subscriber",
                                "consistent", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new OwnCallRecord("shortcut", "a shortcut", "single subscriber", null,
                                "consistent", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new OwnCallRecord("design", "a call", null, null, "conflicts", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ChangeMapRecord("relay -> cursor", "partial", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ChangeMapRecord("relay -> cursor", "indeterminate", " ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ChangeMapRecord("relay -> cursor", "ok", "not a cause"))
        );
    }

    @Test
    void shouldRejectMalformedOwnCallAndChangeMapSections() {
        var base = Map.<String, Object>of(
                "plan_id", "p",
                "started_at", "2026-10-16T08:40:00Z",
                "ended_at", "2026-10-16T09:00:00Z");
        assertAll(
                () -> assertThrows(JsonFormatException.class,
                        () -> PlanCompletionReport.fromMap(with(base, "own_calls", "not-an-array"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> PlanCompletionReport.fromMap(with(base, "own_calls", List.of("not-an-object")))),
                () -> assertThrows(JsonFormatException.class,
                        () -> PlanCompletionReport.fromMap(with(base, "change_map", "not-an-object")))
        );
    }

    private static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        var map = new java.util.LinkedHashMap<String, Object>(base);
        map.put(key, value);
        return map;
    }

    @Test
    void shouldRoundTripTokenUsageInCompletionReport() throws Exception {
        var report = createFullPlanCompletionReport();
        var parsed = PlanCompletionReport.fromJson(report.toJson());

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
        assertTrue(parsed.ownCalls().isEmpty());
        assertNull(parsed.changeMap());
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

    @Test
    void shouldRoundTripTokenComponentRecord() {
        var comp = new TokenComponentRecord(10000L, 2000L, 300L, 4000L, 500L);
        assertEquals(10000L, comp.input());
        assertEquals(2000L, comp.cacheRead());
        assertEquals(300L, comp.cacheWrite());
        assertEquals(4000L, comp.output());
        assertEquals(500L, comp.reasoning());

        var map = comp.toMap();
        var fromMap = TokenComponentRecord.fromMap(map);
        assertEquals(comp, fromMap);
    }

    @Test
    void shouldRoundTripSupportingRecords() {
        var deliv = new DeliverableSummary("doc updates", "done");
        assertEquals("doc updates", deliv.name());
        assertEquals("done", deliv.state());
        assertEquals(deliv, DeliverableSummary.fromMap(deliv.toMap()));

        var replan = new ReplanningRecord("scope changed", "14:00", "TASK-3 modified");
        assertEquals("scope changed", replan.trigger());
        assertEquals("14:00", replan.time());
        assertEquals("TASK-3 modified", replan.change());
        assertEquals(replan, ReplanningRecord.fromMap(replan.toMap()));

        var waste = new TokenWasteRecord("retries", 12000L, 3, 2, "timeout 2");
        assertEquals("retries", waste.wasteClass());
        assertEquals(12000L, waste.tokens());
        assertEquals(3, waste.sharePercent());
        assertEquals(2, waste.tasksCount());
        assertEquals("timeout 2", waste.causeSummary());
        assertEquals(waste, TokenWasteRecord.fromMap(waste.toMap()));
    }

    @Test
    void shouldTestTokenUsageRecordFallbacksAndAccessors() {
        var map = Map.<String, Object>of(
                "total", 50000L,
                "input", 45000L,
                "output", 5000L,
                "duration", 300L,
                "floor", true,
                "coverage", Map.of(
                        "workers", 2L,
                        "models", 1L,
                        "total_tasks", 10L,
                        "unmeasured", 2L,
                        "reason", "stalled"
                ),
                "waste", Map.of(
                "total", 3000L,
                "share_percent", 6L,
                "no_product_tokens", 1000L,
                "no_product_tasks", 1L,
                "retries_tokens", 1500L,
                "retries_causes", List.of("timeout"),
                "remediation_tokens", 500L,
                "remediation_causes", List.of("findings")
        )
        );

        var tokenRecord = TokenUsageRecord.fromMap(map);
        assertEquals(50000L, tokenRecord.total());
        assertEquals(45000L, tokenRecord.input());
        assertEquals(5000L, tokenRecord.output());
        assertTrue(tokenRecord.floor());
        assertEquals(8, tokenRecord.tasksMeasured());
        assertEquals(10, tokenRecord.tasksTotal());
        assertEquals("stalled", tokenRecord.unmeasuredReason());
        assertEquals(3, tokenRecord.wasteRecords().size());
        assertEquals(3000L, tokenRecord.totalWasteTokens());
        assertEquals(6, tokenRecord.wasteSharePercent());
    }

    @Test
    void shouldTestPlanCompletionReportEdgeCases() throws Exception {
        var map = Map.<String, Object>of(
                "plan_id", "test-plan",
                "from", "project > epic > plan",
                "pull_request", "#99",
                "started", "2026-10-16T10:00:00Z",
                "ended", "10:30",
                "run_duration_seconds", 1800L,
                "ran", "30 min"
        );

        var report = PlanCompletionReport.fromMap(map);
        assertEquals("test-plan", report.planId());
        assertEquals("project", report.from().projectId());
        assertEquals("epic", report.from().epicId());
        assertEquals("plan", report.from().planId());
        assertEquals(99, report.pullRequestNumber());
        assertEquals("10:30", report.ended());
        assertEquals("30 min", report.ran());
        assertEquals(1800L, report.durationSeconds());

        // 2-part from path
        var map2 = Map.<String, Object>of(
                "plan_id", "test-plan-2",
                "from", "project > plan2",
                "pull_request", 101L,
                "started", "invalid-date",
                "ended", "invalid-date"
        );
        var report2 = PlanCompletionReport.fromMap(map2);
        assertEquals("plan2", report2.from().planId());
        assertEquals(101, report2.pullRequestNumber());
    }

    @Test
    void shouldTestReportItemEdgeCasesAndFormatting() throws Exception {
        var now = Instant.now();
        var item = new ReportItem(
                "rep-999",
                new ReportFrom("proj", null, null),
                ReportKind.PLAN_ENDED,
                "ended",
                Map.of(),
                "body",
                now.minusSeconds(120),
                now,
                null,
                null,
                null,
                "/plans/p1"
        );

        assertFalse(item.isRead());
        assertTrue(item.isSentLater());
        assertEquals("/plans/p1", item.detail());
        assertNotNull(item.sent());

        // Test string from with plan prefix
        var map = Map.<String, Object>of(
                "id", "rep-1",
                "kind", "plan.ended",
                "from", "my-proj > my-plan",
                "summary", "my summary",
                "created", "today 10:00",
                "sent", "today 10:05"
        );
        var parsed = ReportItem.fromMap(map);
        assertEquals("my-plan", parsed.from().planId());
        assertEquals("my summary", parsed.body());
        assertTrue(parsed.isSentLater());

        // parseList and writeList
        var listStr = ReportItem.writeList(List.of(parsed));
        var listParsed = ReportItem.parseList(listStr);
        assertEquals(1, listParsed.size());
        assertEquals("rep-1", listParsed.get(0).id());

        // formatTime
        var yesterday = now.minus(Duration.ofDays(1));
        var old = now.minus(Duration.ofDays(5));
        assertNotNull(ReportItem.formatTime(now));
        assertNotNull(ReportItem.formatTime(yesterday));
        assertTrue(ReportItem.formatTime(old).contains("d ago"));
    }
}

