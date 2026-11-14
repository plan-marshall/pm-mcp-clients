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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatTurnPointer;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@EnableGeneratorController
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
        var id = "rep-" + Generators.nonBlankStrings().next();
        var subject = Generators.nonBlankStrings().next();
        var body = Generators.nonBlankStrings().next();
        var device = Generators.nonBlankStrings().next();
        var created = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var sent = created.plusSeconds(Generators.integers(61, 600).next());
        var read = sent.plusSeconds(Generators.integers(1, 600).next());

        var item = new ReportItem(
                id,
                new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay"),
                ReportKind.PLAN_ENDED,
                subject,
                Map.of("plan", "fix-relay", "outcome", "merged (#214)"),
                body,
                created,
                sent,
                read,
                "web",
                device,
                "/api/v1/plans/fix-relay/completion"
        );

        assertTrue(item.isRead());
        assertTrue(item.isSentLater());

        var json = item.toJson();
        var parsed = ReportItem.fromJson(json);

        assertEquals(id, parsed.id());
        assertEquals(ReportKind.PLAN_ENDED, parsed.kind());
        assertEquals(subject, parsed.subject());
        assertEquals("fix-relay", parsed.subjectParams().get("plan"));
        assertEquals(body, parsed.body());
        assertEquals(created, parsed.createdAt());
        assertEquals(sent, parsed.sentAt());
        assertEquals(read, parsed.readAt());
        assertEquals("web", parsed.readChannel());
        assertEquals(device, parsed.readDevice());
        assertEquals("/api/v1/plans/fix-relay/completion", parsed.detailLink());
    }

    @Test
    void shouldRoundTripReportsPage() throws Exception {
        var created = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var unreadCount = Generators.integers(1, 50).next();
        var olderCount = Generators.integers(1, 50).next();
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

        var page = new ReportsPage(List.of(item), unreadCount, created, true, olderCount);
        var json = page.toJson();
        var parsed = ReportsPage.fromJson(json);

        assertEquals(1, parsed.items().size());
        assertEquals(unreadCount, parsed.unreadCount());
        assertEquals(created, parsed.oldestUnreadTime());
        assertTrue(parsed.hasOlder());
        assertEquals(olderCount, parsed.olderCount());

        // Parse from bare array
        var arrayJson = ReportItem.writeList(List.of(item));
        var parsedArray = ReportsPage.fromJson(arrayJson);
        assertEquals(1, parsedArray.items().size());
        assertEquals(1, parsedArray.unreadCount());
        assertFalse(parsedArray.hasOlder());
    }

    @Test
    void shouldRoundTripReadReportsRequestsAndResponses() throws Exception {
        var reportId1 = Generators.nonBlankStrings().next();
        var reportId2 = Generators.nonBlankStrings().next();
        var markedCount = Generators.integers(0, 100).next();

        var req2 = ReadReportsRequest.of(List.of(reportId1, reportId2));
        var parsed2 = ReadReportsRequest.fromJson(req2.toJson());
        assertEquals(List.of(reportId1, reportId2), parsed2.reportIds());

        var resp = new ReadReportsResponse(markedCount, "ok");
        var parsedResp = ReadReportsResponse.fromJson(resp.toJson());
        assertEquals(markedCount, parsedResp.markedCount());
        assertEquals("ok", parsedResp.status());
    }

    @Test
    void shouldRoundTripEvents() throws Exception {
        var created = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var reportId = Generators.nonBlankStrings().next();
        var device = Generators.nonBlankStrings().next();
        var item = new ReportItem(reportId, new ReportFrom("p1", null, null), ReportKind.PLAN_STARTED,
                "Started", Map.of(), null, created, null, null, null, null, null);

        var sentEvent = new ReportSentEvent(item);
        assertEquals("report.sent", ReportSentEvent.EVENT_NAME);
        var parsedSent = ReportSentEvent.fromJson(sentEvent.toJson());
        assertEquals(reportId, parsedSent.report().id());

        var readEvent = new ReportReadEvent(reportId, created, "cli", device);
        assertEquals("report.read", ReportReadEvent.EVENT_NAME);
        var parsedRead = ReportReadEvent.fromJson(readEvent.toJson());
        assertEquals(reportId, parsedRead.reportId());
        assertEquals("cli", parsedRead.channel());
        assertEquals(device, parsedRead.device());
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
                new ReplanningRecord("the base moved", "09:55", "TASK-5 added", null)
        );

        var decisions = List.of(
                new DecisionRecord("Q-003", "Rebase onto main", "rebase", "operator", "web", "iPad", null),
                new DecisionRecord("Q-005", "Pick transport", "sse", "delegated", null, null, null)
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
        var map = new LinkedHashMap<String, Object>(base);
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
    void shouldRoundTripTokenComponentRecord() throws Exception {
        var input = (long) Generators.integers(0, 1_000_000).next();
        var cacheRead = (long) Generators.integers(0, 1_000_000).next();
        var cacheWrite = (long) Generators.integers(0, 1_000_000).next();
        var output = (long) Generators.integers(0, 1_000_000).next();
        var reasoning = (long) Generators.integers(0, 1_000_000).next();

        var comp = new TokenComponentRecord(input, cacheRead, cacheWrite, output, reasoning);
        assertEquals(input, comp.input());
        assertEquals(cacheRead, comp.cacheRead());
        assertEquals(cacheWrite, comp.cacheWrite());
        assertEquals(output, comp.output());
        assertEquals(reasoning, comp.reasoning());

        var map = comp.toMap();
        var fromMap = TokenComponentRecord.fromMap(map);
        assertEquals(comp, fromMap);
    }

    @Test
    void shouldRoundTripSupportingRecords() throws Exception {
        var deliverableName = Generators.nonBlankStrings().next();
        var replanTrigger = Generators.nonBlankStrings().next();
        var replanChange = Generators.nonBlankStrings().next();
        var wasteTokens = (long) Generators.integers(0, 1_000_000).next();
        var wasteSharePercent = Generators.integers(0, 100).next();
        var wasteTasksCount = Generators.integers(0, 100).next();
        var wasteCause = Generators.nonBlankStrings().next();

        var deliv = new DeliverableSummary(deliverableName, "done");
        assertEquals(deliverableName, deliv.name());
        assertEquals("done", deliv.state());
        assertEquals(deliv, DeliverableSummary.fromMap(deliv.toMap()));

        var replan = new ReplanningRecord(replanTrigger, "14:00", replanChange, null);
        assertEquals(replanTrigger, replan.trigger());
        assertEquals("14:00", replan.time());
        assertEquals(replanChange, replan.change());
        assertNull(replan.chat());
        assertFalse(replan.toMap().containsKey("chat"));
        assertEquals(replan, ReplanningRecord.fromMap(replan.toMap()));

        var waste = new TokenWasteRecord("retries", wasteTokens, wasteSharePercent, wasteTasksCount,
                wasteCause);
        assertEquals("retries", waste.wasteClass());
        assertEquals(wasteTokens, waste.tokens());
        assertEquals(wasteSharePercent, waste.sharePercent());
        assertEquals(wasteTasksCount, waste.tasksCount());
        assertEquals(wasteCause, waste.causeSummary());
        assertEquals(waste, TokenWasteRecord.fromMap(waste.toMap()));
    }

    @Test
    void shouldRoundTripReplanningRecordWithItsChat() throws Exception {
        var chat = new ChatTurnPointer(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next());
        var replan = new ReplanningRecord(Generators.nonBlankStrings().next(), "14:00",
                Generators.nonBlankStrings().next(), chat);

        var parsed = ReplanningRecord.fromMap(replan.toMap());

        assertAll(
                () -> assertEquals(replan, parsed),
                () -> assertEquals(chat, parsed.chat()),
                () -> assertThrows(JsonFormatException.class,
                        () -> ReplanningRecord.fromMap(with(replan.toMap(), "chat", Map.of("chat_id", "c-1")))),
                () -> assertThrows(JsonFormatException.class,
                        () -> ReplanningRecord.fromMap(with(replan.toMap(), "chat", "not-an-object"))));
    }

    @Test
    void shouldRoundTripDecisionRecordWithAndWithoutAfterChat() throws Exception {
        var afterChat = new ChatTurnPointer(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next());
        var withChat = new DecisionRecord(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next(),
                Generators.nonBlankStrings().next(), "operator", "web", Generators.nonBlankStrings().next(), afterChat);
        var withoutChat = new DecisionRecord(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next(),
                null, "delegated", null, null, null);

        var parsedWithChat = DecisionRecord.fromMap(withChat.toMap());

        assertAll(
                () -> assertEquals(withChat, parsedWithChat),
                () -> assertEquals(afterChat, parsedWithChat.afterChat()),
                () -> assertEquals(withoutChat, DecisionRecord.fromMap(withoutChat.toMap())),
                () -> assertFalse(withoutChat.toMap().containsKey("after_chat")),
                () -> assertThrows(JsonFormatException.class, () -> DecisionRecord.fromMap(
                        with(withChat.toMap(), "after_chat", Map.of("turn_id", "t-1")))));
    }

    @Test
    void shouldCarryTheChatPointersThroughACompletionReport() throws Exception {
        var chat = new ChatTurnPointer(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next());
        var withChats = new PlanCompletionReport("fix-relay", "fix-relay",
                new ReportFrom("pm-mcp-clients", null, "fix-relay"), "merged", null, null, null, null,
                Instant.parse("2026-10-16T08:40:00Z"), Instant.parse("2026-10-16T09:00:00Z"), 1200, 0, null,
                List.of(),
                List.of(new ReplanningRecord("the operator asked", "10:00", "TASK-6 added", chat)),
                List.of(new DecisionRecord("Q-007", "Split TASK-4", "split", "operator", "web", "iPad", chat)),
                null);

        var parsed = PlanCompletionReport.fromJson(withChats.toJson());

        assertAll(
                () -> assertEquals(chat, parsed.replanningRecords().getFirst().chat()),
                () -> assertEquals(chat, parsed.decisionRecords().getFirst().afterChat()));
    }

    @Test
    void shouldTestTokenUsageRecordFallbacksAndAccessors() throws Exception {
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

    @Nested
    @DisplayName("TokenUsageRecord read leniently")
    class TokenUsageReading {

        @Test
        @DisplayName("should read an empty usage as zero figures, not floor, without reason and without rows")
        void shouldReadEmptyUsage() throws Exception {
            var usage = TokenUsageRecord.fromMap(Map.of());

            assertAll(
                    () -> assertEquals(0L, usage.totalTokens()),
                    () -> assertEquals(0L, usage.durationSeconds()),
                    () -> assertEquals(0, usage.tasksMeasured()),
                    () -> assertEquals(0, usage.tasksTotal()),
                    () -> assertFalse(usage.isFloor()),
                    () -> assertNull(usage.unmeasuredReason()),
                    () -> assertFalse(usage.toMap().containsKey("unmeasured_reason")),
                    () -> assertTrue(usage.byPhase().isEmpty()),
                    () -> assertTrue(usage.wasteRecords().isEmpty()),
                    () -> assertEquals(0L, usage.totalWasteTokens()));
        }

        @Test
        @DisplayName("should replace absent row lists of a constructed usage by empty lists")
        void shouldReplaceAbsentListsByEmptyLists() {
            var total = (long) Generators.integers(1, 1_000_000).next();

            var usage = new TokenUsageRecord(total, total, 0L, 0L, 0, 0, 0, 0, false, null, null, null, null, null,
                    0L, 0);

            assertAll(
                    () -> assertTrue(usage.byPhase().isEmpty()),
                    () -> assertTrue(usage.byRole().isEmpty()),
                    () -> assertTrue(usage.byModel().isEmpty()),
                    () -> assertTrue(usage.wasteRecords().isEmpty()),
                    () -> assertEquals(List.of(), usage.toMap().get("by_model")));
        }

        @Test
        @DisplayName("should prefer the members of the current schema over coverage and legacy waste")
        void shouldPreferCurrentMembers() throws Exception {
            var workers = Generators.integers(1, 9).next();
            var models = Generators.integers(1, 9).next();
            var tasksTotal = Generators.integers(2, 50).next();
            var measured = Generators.integers(1, tasksTotal).next();
            var reason = Generators.nonBlankStrings().next();
            var wasteTotal = (long) Generators.integers(1, 1_000_000).next();
            var wasteRecord = new TokenWasteRecord("retries", wasteTotal, 3, 1, null);
            var row = new TokenBreakdownItem("execute", 10, 8, 2, 0, false, null, null);
            var map = new LinkedHashMap<String, Object>();
            map.put("workers_count", (long) workers);
            map.put("models_count", (long) models);
            map.put("tasks_total", (long) tasksTotal);
            map.put("tasks_measured", (long) measured);
            map.put("unmeasured_reason", reason);
            map.put("coverage", Map.of("workers", 99L, "models", 99L, "total_tasks", 99L, "unmeasured", 1L,
                    "reason", "other"));
            map.put("waste_records", List.of(wasteRecord.toMap(), "not-an-object"));
            map.put("total_waste_tokens", wasteTotal);
            map.put("waste_share_percent", 3L);
            map.put("waste", Map.of("total", 99L, "share_percent", 99L, "retries_tokens", 99L));
            map.put("by_phase", List.of(row.toMap(), "not-an-object"));

            var usage = TokenUsageRecord.fromMap(map);

            assertAll(
                    () -> assertEquals(workers, usage.workersCount()),
                    () -> assertEquals(models, usage.modelsCount()),
                    () -> assertEquals(tasksTotal, usage.tasksTotal()),
                    () -> assertEquals(measured, usage.tasksMeasured()),
                    () -> assertEquals(reason, usage.unmeasuredReason()),
                    () -> assertEquals(List.of(wasteRecord), usage.wasteRecords()),
                    () -> assertEquals(wasteTotal, usage.totalWasteTokens()),
                    () -> assertEquals(3, usage.wasteSharePercent()),
                    () -> assertEquals(List.of(row), usage.byPhase()));
        }

        @Test
        @DisplayName("should derive legacy waste rows only for the classes that carry tokens or tasks")
        void shouldDeriveLegacyWasteRowsSparingly() throws Exception {
            var noProductTasks = Generators.integers(1, 20).next();
            var retriesTokens = (long) Generators.integers(1, 1_000_000).next();
            var map = Map.<String, Object>of(
                    "coverage", Map.of(),
                    "waste", Map.of(
                            "no_product_tasks", (long) noProductTasks,
                            "retries_tokens", retriesTokens,
                            "retries_causes", "not-a-list",
                            "remediation_tokens", 0L));

            var usage = TokenUsageRecord.fromMap(map);
            var empty = TokenUsageRecord.fromMap(Map.of("waste", Map.of("no_product_tokens", 0L)));

            assertAll(
                    () -> assertEquals(0, usage.tasksMeasured()),
                    () -> assertEquals(List.of(
                            new TokenWasteRecord("no_product", 0L, 0, noProductTasks, null),
                            new TokenWasteRecord("retries", retriesTokens, 0, 1, null)), usage.wasteRecords()),
                    () -> assertTrue(empty.wasteRecords().isEmpty()));
        }
    }

    @Nested
    @DisplayName("ReportsPage read leniently")
    class ReportsPageReading {

        @Test
        @DisplayName("should read the short member names, count the items, and skip an item that is no object")
        void shouldReadShortMemberNames() throws Exception {
            var id = Generators.nonBlankStrings().next();
            var older = Generators.integers(1, 50).next();
            var json = JsonTree.write(Map.of("reports", List.of(Map.of("id", id), 1),
                    "oldest_unread", "yesterday 18:40", "older_count", older));

            var page = ReportsPage.fromJson(json);

            assertAll(
                    () -> assertEquals(List.of(id), page.items().stream().map(ReportItem::id).toList()),
                    () -> assertEquals(1, page.unreadCount()),
                    () -> assertNull(page.oldestUnreadTime()),
                    () -> assertTrue(page.hasOlder()),
                    () -> assertEquals(older, page.olderCount()));
        }

        @Test
        @DisplayName("should read an empty page and write no absent oldest unread time")
        void shouldReadEmptyPage() throws Exception {
            var unread = Generators.integers(0, 50).next();

            var empty = ReportsPage.fromMap(Map.of("items", Generators.nonBlankStrings().next()));
            var constructed = new ReportsPage(null, unread, null, false, 0);

            assertAll(
                    () -> assertTrue(empty.items().isEmpty()),
                    () -> assertEquals(0, empty.unreadCount()),
                    () -> assertFalse(empty.hasOlder()),
                    () -> assertTrue(constructed.items().isEmpty()),
                    () -> assertFalse(constructed.toMap().containsKey("oldest_unread_time")),
                    () -> assertEquals(constructed, ReportsPage.fromMap(constructed.toMap())));
        }

        @Test
        @DisplayName("should refuse a count with a fraction, JSON that is neither page nor list, and skip a list's non-object")
        void shouldRefuseMalformedShapes() throws Exception {
            var id = Generators.nonBlankStrings().next();
            var fraction = JsonTree.write(Map.of("unread_count", 2.5));
            var scalar = JsonTree.write(Generators.integers(0, 100).next());

            var list = ReportsPage.fromJson(JsonTree.write(List.of("x", Map.of("id", id))));

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> ReportsPage.fromJson(fraction)),
                    () -> assertThrows(JsonFormatException.class, () -> ReportsPage.fromJson(scalar)),
                    () -> assertEquals(1, list.unreadCount()));
        }
    }

    @Test
    @DisplayName("TokenComponentRecord should read an absent or non-numeric component as unreported and zero")
    void shouldReadUnreportedTokenComponents() throws Exception {
        var components = TokenComponentRecord.fromMap(Map.of("input", Generators.nonBlankStrings().next()));

        assertAll(
                () -> assertNull(components.inputTokens()),
                () -> assertEquals(0L, components.input()),
                () -> assertEquals(0L, components.cacheRead()),
                () -> assertEquals(0L, components.cacheWrite()),
                () -> assertEquals(0L, components.output()),
                () -> assertEquals(0L, components.reasoning()),
                () -> assertTrue(components.toMap().isEmpty()));
    }

    @Nested
    @DisplayName("ReportItem read leniently")
    class ReportItemReading {

        @Test
        @DisplayName("should not mark a report sent later whose displayed sent time equals its created time")
        void shouldNotMarkEqualDisplayedTimesSentLater() throws Exception {
            var shown = "today " + Generators.integers(10, 23).next() + ":00";

            var item = ReportItem.fromMap(Map.of("id", Generators.nonBlankStrings().next(), "from", "p",
                    "created", shown, "sent", shown));

            assertAll(
                    () -> assertFalse(item.isSentLater()),
                    () -> assertEquals(shown, item.created()),
                    () -> assertEquals(shown, item.sent()));
        }

        @Test
        @DisplayName("should keep a sent time that is no instant as its text and invent no instant for it")
        void shouldKeepAnUnparseableSentTimeAsText() throws Exception {
            var created = Instant.ofEpochSecond(Generators.integers(0, 1_000_000_000).next());
            var shown = "Tuesday " + Generators.integers(10, 23).next() + ":00";

            var item = ReportItem.fromMap(Map.of("id", Generators.nonBlankStrings().next(), "from", "p",
                    "created_at", created.toString(), "sent_at", shown));

            assertAll(
                    () -> assertNull(item.sentAt()),
                    () -> assertEquals(shown, item.sent()),
                    () -> assertFalse(item.isSentLater()),
                    () -> assertEquals(created, item.createdAt()),
                    () -> assertEquals(ReportItem.formatTime(created), item.created()));
        }

        @Test
        @DisplayName("should default every absent member of a report that carries only its id")
        void shouldDefaultAbsentMembers() throws Exception {
            var id = Generators.nonBlankStrings().next();
            var before = Instant.now();

            var item = ReportItem.fromMap(Map.of("id", id, "read_at", "not-an-instant",
                    "detail", "/api/v1/reports/" + id));
            var after = Instant.now();

            assertAll(
                    () -> assertEquals(ReportKind.PLAN_ENDED, item.kind()),
                    () -> assertEquals("", item.subject()),
                    () -> assertEquals(new ReportFrom("", null, null), item.from()),
                    () -> assertFalse(item.createdAt().isBefore(before)),
                    () -> assertFalse(item.createdAt().isAfter(after)),
                    () -> assertNull(item.sentAt()),
                    () -> assertEquals(item.created(), item.sent()),
                    () -> assertNull(item.readAt()),
                    () -> assertFalse(item.isRead()),
                    () -> assertNull(item.body()),
                    () -> assertEquals("/api/v1/reports/" + id, item.detail()));
        }

        @ParameterizedTest(name = "{0} of kind {1}")
        @CsvSource({
                "proj, plan.ended, proj, , ",
                "proj › epic › plan, plan.ended, proj, epic, plan",
                "proj > epic, epic.ended, proj, epic, ",
                "proj > plan, plan.started, proj, , plan"
        })
        @DisplayName("should read a from path by its parts and the kind of the report")
        void shouldReadAFromPath(String path, String kind, String project, String epic, String plan)
                throws Exception {
            var item = ReportItem.fromMap(Map.of("id", Generators.nonBlankStrings().next(), "kind", kind,
                    "from", path));

            assertEquals(new ReportFrom(project, epic, plan), item.from());
        }

        @Test
        @DisplayName("should refuse a from that is neither a path nor an object")
        void shouldRefuseAFromOfAnotherType() {
            var map = Map.<String, Object>of("id", Generators.nonBlankStrings().next(), "from", List.of());

            assertThrows(JsonFormatException.class, () -> ReportItem.fromMap(map));
        }

        @Test
        @DisplayName("should refuse a report or a list of reports in the wrong JSON shape and skip non-objects")
        void shouldRefuseWrongShapes() throws Exception {
            var id = Generators.nonBlankStrings().next();

            var list = ReportItem.parseList(JsonTree.write(List.of(1, Map.of("id", id))));

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> ReportItem.fromJson("[]")),
                    () -> assertThrows(JsonFormatException.class, () -> ReportItem.parseList("{}")),
                    () -> assertEquals(List.of(id), list.stream().map(ReportItem::id).toList()));
        }

        @Test
        @DisplayName("should replace absent subject parameters of a constructed report by an empty map")
        void shouldReplaceAbsentSubjectParameters() {
            var item = new ReportItem(Generators.nonBlankStrings().next(), new ReportFrom("p", null, null),
                    ReportKind.PLAN_STARTED, Generators.nonBlankStrings().next(), null, null, Instant.EPOCH, null,
                    null, null, null, null);

            assertAll(
                    () -> assertTrue(item.subjectParams().isEmpty()),
                    () -> assertFalse(item.toMap().containsKey("subject_params")));
        }
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

    @Nested
    @DisplayName("PlanCompletionReport read leniently")
    class CompletionReading {

        @Test
        @DisplayName("should take the plan id from the short member and refuse a report without one")
        void shouldReadOrRefuseThePlanId() throws Exception {
            var plan = Generators.nonBlankStrings().next();

            var report = PlanCompletionReport.fromMap(Map.of("plan", plan, "started_at", "2026-10-16T08:40:00Z",
                    "ended_at", "2026-10-16T09:00:00Z"));

            assertAll(
                    () -> assertEquals(plan, report.planId()),
                    () -> assertEquals(plan, report.title()),
                    () -> assertThrows(JsonFormatException.class,
                            () -> PlanCompletionReport.fromMap(Map.of("title", plan))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> PlanCompletionReport.fromJson(JsonTree.write(List.of(plan)))));
        }

        @Test
        @DisplayName("should read a one-part from path as the project and a from of another type as the plan's own")
        void shouldReadFromVariants() throws Exception {
            var project = "proj" + Generators.integers(0, 999).next();
            var planId = "plan" + Generators.integers(0, 999).next();

            var onePart = PlanCompletionReport.fromMap(Map.of("plan_id", planId, "from", project));
            var number = PlanCompletionReport.fromMap(Map.of("plan_id", planId,
                    "from", Generators.integers(0, 999).next()));

            assertAll(
                    () -> assertEquals(new ReportFrom(project, null, null), onePart.from()),
                    () -> assertEquals(new ReportFrom("project", null, planId), number.from()));
        }

        @Test
        @DisplayName("should read an unparseable pull request as none and keep an end time that is no instant as text")
        void shouldReadUnparseablePullRequestAndEndText() throws Exception {
            var shown = "Tuesday " + Generators.integers(10, 23).next() + ":00";

            var report = PlanCompletionReport.fromMap(Map.of("plan_id", Generators.nonBlankStrings().next(),
                    "pull_request", "#" + Generators.nonBlankStrings().next() + "x", "ended", shown));

            assertAll(
                    () -> assertNull(report.pullRequestNumber()),
                    () -> assertEquals(shown, report.rawEnded()),
                    () -> assertEquals(shown, report.ended()));
        }

        @Test
        @DisplayName("should skip a non-object entry of the deliverables, re-plannings and decisions, and a token usage that is no object")
        void shouldSkipNonObjectEntries() throws Exception {
            var name = Generators.nonBlankStrings().next();
            var noObject = Generators.nonBlankStrings().next();
            var deliverable = new DeliverableSummary(name, "done");
            var replan = new ReplanningRecord(name, "14:00", name, null);
            var decision = new DecisionRecord("Q-001", name, null, "delegated", null, null, null);

            var report = PlanCompletionReport.fromMap(Map.of("plan_id", name,
                    "deliverables", List.of(noObject, deliverable.toMap()),
                    "replanning", List.of(noObject, replan.toMap()),
                    "decisions", List.of(noObject, decision.toMap()),
                    "tokens", noObject));

            assertAll(
                    () -> assertEquals(List.of(deliverable), report.deliverables()),
                    () -> assertEquals(List.of(replan), report.replanningRecords()),
                    () -> assertEquals(List.of(decision), report.decisionRecords()),
                    () -> assertNull(report.tokenUsage()));
        }

        static Stream<Arguments> startTimesNotGiven() {
            var text = Generators.letterStrings(3, 12).next();
            return Stream.of(
                    Arguments.of("absent", Map.of()),
                    Arguments.of("started_at no instant", Map.of("started_at", text)),
                    Arguments.of("started no instant", Map.of("started", text)));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("startTimesNotGiven")
        @DisplayName("should keep a start time the runtime did not give as an instant absent, never stand in the present")
        void shouldKeepAnAbsentOrUnreadableStartTimeAbsent(String variant, Map<String, Object> startMember)
                throws Exception {
            var member = new LinkedHashMap<String, Object>(startMember);
            member.put("plan_id", Generators.nonBlankStrings().next());

            var report = PlanCompletionReport.fromMap(member);

            assertAll(
                    () -> assertNull(report.startedAt(), variant),
                    () -> assertFalse(report.toMap().containsKey("started_at"), variant));
        }

        @Test
        @DisplayName("should keep an absent end time absent, with no end text to display")
        void shouldKeepAnAbsentEndTimeAbsent() throws Exception {
            var report = PlanCompletionReport.fromMap(Map.of("plan_id", Generators.nonBlankStrings().next()));

            assertAll(
                    () -> assertNull(report.endedAt()),
                    () -> assertNull(report.rawEnded()),
                    () -> assertNull(report.ended()),
                    () -> assertFalse(report.toMap().containsKey("ended_at")));
        }

        @Test
        @DisplayName("should keep an end text that is no instant as text and leave the end instant absent")
        void shouldKeepAnUnreadableEndInstantAbsent() throws Exception {
            var shown = Generators.letterStrings(3, 12).next();

            var report = PlanCompletionReport.fromMap(Map.of("plan_id", Generators.nonBlankStrings().next(),
                    "ended_at", shown));

            assertAll(
                    () -> assertNull(report.endedAt()),
                    () -> assertEquals(shown, report.rawEnded()),
                    () -> assertEquals(shown, report.ended()));
        }

        @Test
        @DisplayName("should keep the absence of both times across the wire round trip")
        void shouldKeepAbsentTimesAcrossTheRoundTrip() throws Exception {
            var planId = Generators.nonBlankStrings().next();
            var report = new PlanCompletionReport(planId, planId, new ReportFrom("p", null, planId), "merged", null,
                    null, null, null, null, null, 0, 0, null, null, null, null, null, null, null);

            var json = report.toJson();
            var read = PlanCompletionReport.fromJson(json);

            assertAll(
                    () -> assertFalse(json.contains("started_at")),
                    () -> assertFalse(json.contains("ended_at")),
                    () -> assertNull(read.startedAt()),
                    () -> assertNull(read.endedAt()),
                    () -> assertNull(read.ended()),
                    () -> assertEquals(report, read));
        }

        @Test
        @DisplayName("should replace absent lists of a constructed report by empty lists")
        void shouldReplaceAbsentLists() {
            var planId = Generators.nonBlankStrings().next();

            var report = new PlanCompletionReport(planId, planId, new ReportFrom("p", null, planId), "merged", null,
                    null, null, null, Instant.EPOCH, Instant.EPOCH, 0, 0, null, null, null, null, null, null, null);

            assertAll(
                    () -> assertTrue(report.deliverables().isEmpty()),
                    () -> assertTrue(report.replanningRecords().isEmpty()),
                    () -> assertTrue(report.decisionRecords().isEmpty()),
                    () -> assertTrue(report.ownCalls().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Report counts on the wire")
    class WholeCounts {

        /** A reader of one report record from a parsed JSON object. */
        @FunctionalInterface
        interface RecordReader {
            Object read(Map<String, Object> object) throws Exception;
        }

        static Stream<Arguments> countMembers() {
            RecordReader usage = TokenUsageRecord::fromMap;
            RecordReader row = TokenBreakdownItem::fromMap;
            RecordReader components = TokenComponentRecord::fromMap;
            RecordReader waste = TokenWasteRecord::fromMap;
            RecordReader report = PlanCompletionReport::fromMap;
            return Stream.of(
                    Arguments.of("usage total_tokens", usage, "{\"total_tokens\":%s}"),
                    Arguments.of("usage coverage workers", usage, "{\"coverage\":{\"workers\":%s}}"),
                    Arguments.of("usage legacy waste total", usage, "{\"waste\":{\"total\":%s}}"),
                    Arguments.of("row tokens", row, "{\"label\":\"execute\",\"tokens\":%s}"),
                    Arguments.of("usage row unmeasured", usage, "{\"by_role\":[{\"label\":\"r\",\"unmeasured\":%s}]}"),
                    Arguments.of("components input", components, "{\"input\":%s}"),
                    Arguments.of("row components reasoning", row, "{\"label\":\"x\",\"components\":{\"reasoning\":%s}}"),
                    Arguments.of("waste tokens", waste, "{\"waste_class\":\"retries\",\"tokens\":%s}"),
                    Arguments.of("usage waste record share", usage,
                            "{\"waste_records\":[{\"waste_class\":\"retries\",\"share_percent\":%s}]}"),
                    Arguments.of("report paused_seconds", report, "{\"plan_id\":\"p\",\"paused_seconds\":%s}"),
                    Arguments.of("report pull_request_number", report,
                            "{\"plan_id\":\"p\",\"pull_request_number\":%s}"),
                    Arguments.of("report token usage", report,
                            "{\"plan_id\":\"p\",\"token_usage\":{\"output_tokens\":%s}}"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("countMembers")
        @DisplayName("should refuse a count with a fraction instead of truncating it")
        void shouldRefuseACountWithAFraction(String member, RecordReader reader, String template) throws Exception {
            var fraction = Generators.integers(0, 1_000_000).next() + ".5";
            var object = JsonTree.asObject(JsonTree.parse(template.formatted(fraction)));

            assertThrows(JsonFormatException.class, () -> reader.read(object), member);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("countMembers")
        @DisplayName("should read a whole count written with a zero fraction as that count")
        void shouldReadAWholeCountWithAZeroFraction(String member, RecordReader reader, String template)
                throws Exception {
            var whole = Generators.integers(0, 1_000_000).next() + ".0";
            var object = JsonTree.asObject(JsonTree.parse(template.formatted(whole)));

            assertNotNull(reader.read(object), member);
        }

        @Test
        @DisplayName("should refuse a count held as a fractional number built in memory")
        void shouldRefuseAFractionalNumberInMemory() {
            var fraction = Generators.integers(0, 1_000_000).next() + 0.5;

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> TokenComponentRecord.fromMap(Map.of("output", fraction))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> TokenWasteRecord.fromMap(Map.of("tokens", fraction))));
        }
    }
}

