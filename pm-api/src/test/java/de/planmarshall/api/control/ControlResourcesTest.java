/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("ControlResources")
class ControlResourcesTest {

    @Test
    @DisplayName("round-trips an epic's tokens with a floor and its coverage")
    void epicTokens() throws Exception {
        var total = new TokenFigure(3_860_000L, 3_520_000L, 340_000L, true, true, 4, 1);
        var tokens = new EpicTokens(total,
                new TokenFigure(120_000L, 110_000L, 10_000L, true, false, 1, 0),
                List.of(new PlanTokenFigure("relay-reconnect-backoff",
                        new TokenFigure(1_400_000L, 1_300_000L, 100_000L, true, false, 2, 0))));

        var parsed = EpicTokens.fromJson(tokens.toJson());

        assertEquals(3_860_000L, parsed.total().total());
        assertTrue(parsed.total().floor());
        assertEquals(4, parsed.total().measuredTasks());
        assertEquals(1, parsed.notMeasuredCount());
        assertFalse(parsed.notMeasured());
        assertEquals(1, parsed.plans().size());
        assertEquals("relay-reconnect-backoff", parsed.plans().getFirst().planId());

        var unmeasured = new EpicTokens(TokenFigure.notMeasured(), TokenFigure.notMeasured(), List.of());
        assertTrue(EpicTokens.fromJson(unmeasured.toJson()).notMeasured());
    }

    @Test
    @DisplayName("round-trips the epic resource with its rows, waits, and tokens")
    void epicResource() throws Exception {
        var row = new WorkRow("relay-token-rotation", "relay-token-rotation", "staged", "dependency",
                "relay-auth-v2", List.of(), null, null, null, "rotate the tokens",
                List.of("the rotation", "a test"), List.of("pm-api/src/token"), List.of("relay-auth-v2"));
        var overlap = new WorkRow("relay-docs", "relay-docs", "staged", "overlap", "relay-auth-v2",
                List.of("pm-api/src/store/Doc.java"), null, null, null, null, List.of(), List.of(), List.of());
        var resource = new EpicResource("relay-hardening", "pm-mcp-clients", "pm-mcp-clients",
                "https://github.com/plan-marshall/pm-mcp-clients/tree/epic/relay-hardening",
                "running", true, 0, 3, 2, null,
                List.of(new EpicWorkstream("WS-2", "Authentication", true, List.of(row, overlap))),
                List.of("2 documents · 1 issue"), 2,
                new RegisterSummary(3, 4, 2), new RegisterSummary(1, 4, 0),
                new EpicTokens(new TokenFigure(3_860_000L, 3_520_000L, 340_000L, true, false, 4, 0),
                        TokenFigure.notMeasured(), List.of()),
                List.of(new EpicEvent("2026-10-16T12:05:00Z",
                        "relay-reconnect-backoff landed; relay-reconnect-metrics re-ordered")));

        var parsed = EpicResource.fromJson(resource.toJson());

        assertEquals("relay-hardening", parsed.epicId());
        assertEquals("pm-mcp-clients", parsed.projectId());
        assertEquals("running", parsed.state());
        assertTrue(parsed.autoStart());
        assertEquals(3, parsed.scope());
        assertEquals(2, parsed.running());
        assertFalse(parsed.hasEnded());
        var parsedRow = parsed.workstreams().getFirst().rows().getFirst();
        assertEquals(EpicWaitReason.DEPENDENCY, parsedRow.waits());
        assertEquals("relay-auth-v2", parsedRow.waitPlanId());
        var parsedOverlap = parsed.workstreams().getFirst().rows().get(1);
        assertEquals(EpicWaitReason.OVERLAP, parsedOverlap.waits());
        assertEquals(List.of("pm-api/src/store/Doc.java"), parsedOverlap.overlapFiles());
        assertEquals(3, parsed.findings().newCount());
        assertEquals(1, parsed.events().size());
        assertEquals("WS-2", parsed.workstreams().getFirst().id());
    }

    @Test
    @DisplayName("round-trips an ended epic with its terminal reason")
    void endedEpic() throws Exception {
        var ended = new EpicResource("relay-hardening", "pm-mcp-clients", "pm-mcp-clients", null,
                "ended", false, 0, 3, 0, "completed", List.of(), List.of(), 11,
                new RegisterSummary(0, 0, 3), new RegisterSummary(0, 0, 5),
                new EpicTokens(TokenFigure.notMeasured(), TokenFigure.notMeasured(), List.of()),
                List.of());
        var parsed = EpicResource.fromJson(ended.toJson());
        assertTrue(parsed.hasEnded());
        assertEquals(EpicTerminalReason.COMPLETED, parsed.end());
    }

    @Test
    @DisplayName("round-trips a decomposition draft with its checks")
    void decomposition() throws Exception {
        var plan = new DecompositionPlan("relay-auth-v2", "relay-auth-v2", "the token work",
                List.of("the auth flow"), List.of("pm-api/src/auth"), List.of(),
                1, "changed", false);
        var resource = new DecompositionResource("relay-hardening", "check", 7,
                List.of(new DecompositionWorkstream("WS-2", "Authentication", "the token work", 2, List.of(plan))),
                List.of(new DecompositionCheck("relay-auth-v2", DecompositionCheck.SEVERITY_ERROR,
                        "surface overlaps relay-cursor-order (pm-api)")));

        var parsed = DecompositionResource.fromJson(resource.toJson());

        assertEquals("relay-hardening", parsed.epicId());
        assertEquals("check", parsed.state());
        assertEquals(7L, parsed.revision());
        assertEquals(1, parsed.planCount());
        assertFalse(parsed.everyCheckPasses());
        var parsedPlan = parsed.workstreams().getFirst().plans().getFirst();
        assertEquals("changed", parsedPlan.change());
        assertEquals(List.of("pm-api/src/auth"), parsedPlan.surface());
        assertTrue(new DecompositionResource("e", "commit", 1L, List.of(), List.of()).everyCheckPasses());
    }

    @Test
    @DisplayName("round-trips a chat read-back with its transcript and proposal")
    void chatReadBack() throws Exception {
        var chat = new ChatReadBackResource("C-1", "fix-relay", "plan", "replan", "plan", "read-back",
                List.of(new ChatTurnResource("operator", "Can the cursor test cover SSE too?", "2026-10-16T14:02:00Z", false),
                        new ChatTurnResource("replan", "Yes. I propose a second test.", "2026-10-16T14:03:00Z", false)),
                new ChatProposalResource("a test per transport",
                        "TASK-4  one transport", "TASK-4  a test per transport", "after the current task"));

        var parsed = ChatReadBackResource.fromJson(chat.toJson());

        assertEquals("replan", parsed.chatRole().value());
        assertEquals(ChatScope.PLAN, parsed.chatScope());
        assertEquals(ChatState.READ_BACK, parsed.chatState());
        assertTrue(parsed.hasProposal());
        assertEquals(2, parsed.turns().size());
        assertTrue(parsed.turns().getFirst().isOperator());
        assertEquals("after the current task", parsed.proposal().applies());

        var untrusted = new ChatTurnResource("replan", "from a comment", "2026-10-16T14:04:00Z", true);
        assertTrue(ChatTurnResource.fromJson(untrusted.toJson()).untrusted());
    }

    @Test
    @DisplayName("round-trips a review with a promotion and a read-back")
    void review() throws Exception {
        var item = new ReviewItemResource("F-0004", "findings", "Reconnect loses the last event",
                List.of("a fix must hold for every transport"), "landing of relay-reconnect (#211)",
                "untrusted", "open", "Promote to a plan");
        var review = new ReviewResource("R-1", "relay-hardening", "findings", List.of(item), 3, 7,
                "read-back",
                new DraftedPlan("relay-reconnect-order", "reorder the cursor write",
                        List.of("a test per transport"), List.of("pm-relay/src/net"), List.of()),
                new ReviewReadBackResource(ReviewReadBackResource.KIND_PLAN, "old draft", "new draft", null));

        var parsed = ReviewResource.fromJson(review.toJson());

        assertEquals("findings", parsed.register());
        assertEquals(3, parsed.decided());
        assertEquals(7, parsed.total());
        assertEquals(1, parsed.openItems().size());
        assertEquals(ReviewDisposition.OPEN, parsed.item("F-0004").disposition());
        assertTrue(parsed.item("F-0004").isUntrusted());
        assertEquals("relay-reconnect-order", parsed.draftedPlan().planId());
        assertEquals(ReviewReadBackResource.KIND_PLAN, parsed.readBack().kind());
        assertNull(parsed.readBack().plan());
    }

    @Test
    @DisplayName("round-trips an archive check and computes whether it passes")
    void archiveCheck() throws Exception {
        var failing = new ArchiveCheck("relay-hardening",
                List.of(new ArchiveCheckLine("control.archive.no_plan_active", true, null, null),
                        new ArchiveCheckLine("control.archive.open_findings", false, "2", "review-findings")),
                3, 5, false);

        var parsed = ArchiveCheck.fromJson(failing.toJson());

        assertFalse(parsed.passed());
        assertEquals(2, parsed.lines().size());
        assertEquals("review-findings", parsed.lines().get(1).action());
        assertEquals(3, parsed.carriedFindings());
        assertEquals(5, parsed.carriedWatches());

        var passing = new ArchiveCheck("relay-hardening",
                List.of(new ArchiveCheckLine("control.archive.no_plan_active", true, null, null)), 3, 5, false);
        assertTrue(passing.passed());

        var reviewFirst = new ArchiveCheck("relay-hardening", List.of(), 0, 0, true);
        assertFalse(reviewFirst.passed());
    }

    @Test
    @DisplayName("answers the derived states of the resources")
    void derivedStates() throws Exception {
        var shipped = new WorkRow("p", "p", "shipped", null, null, List.of(), 211,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/211", null, null,
                List.of(), List.of(), List.of());
        var staged = new WorkRow("q", "q", "staged", "none", null, List.of(), null, null, null, null,
                List.of(), List.of(), List.of());
        assertTrue(shipped.isLocked());
        assertFalse(staged.isLocked());

        var parsedShipped = WorkRow.fromJson(shipped.toJson());
        assertEquals("https://github.com/plan-marshall/pm-mcp-clients/pull/211",
                parsedShipped.pullRequestUrl());
        assertNull(WorkRow.fromJson(staged.toJson()).pullRequestUrl());

        var epic = new EpicResource("e", "prj", "prj", null, "running", true, 0, 3, 1, null,
                List.of(new EpicWorkstream("WS-1", "one", true, List.of(staged, shipped))),
                List.of(), 0, new RegisterSummary(0, 0, 0), new RegisterSummary(0, 0, 0),
                new EpicTokens(TokenFigure.notMeasured(), TokenFigure.notMeasured(), List.of()), List.of());
        assertEquals(1, epic.stagedCount());
        assertEquals(0, epic.tokens().notMeasuredCount());

        var review = new ReviewResource("r", "e", "findings", List.of(), 0, 0, "open", null, null);
        assertNull(review.item("missing"));
    }

    @Test
    @DisplayName("rejects malformed JSON for every new resource")
    void rejectsMalformed() {
        assertThrows(JsonFormatException.class, () -> EpicResource.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> DecompositionResource.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ChatReadBackResource.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ReviewResource.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ArchiveCheck.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> TokenFigure.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> WorkRow.fromJson("{}"));

        // a non-object nested element fails closed instead of being dropped
        assertThrows(JsonFormatException.class,
                () -> ArchiveCheck.fromJson("{\"epic_id\":\"e\",\"lines\":[false]}"));
        // a present non-array value fails closed instead of becoming an empty list
        assertThrows(JsonFormatException.class,
                () -> ArchiveCheck.fromJson("{\"epic_id\":\"e\",\"lines\":\"x\"}"));
        // a present explicit null fails closed, so an archive check cannot pass vacuously
        assertThrows(JsonFormatException.class,
                () -> ArchiveCheck.fromJson("{\"epic_id\":\"e\",\"lines\":null}"));
        // a structured element in a scalar array fails closed instead of being coerced to text
        assertThrows(JsonFormatException.class,
                () -> ReviewItemResource.fromJson("{\"item_id\":\"F\",\"rules\":[{}]}"));
        assertThrows(JsonFormatException.class,
                () -> WorkRow.fromJson("{\"plan_id\":\"p\",\"deliverables\":[{}]}"));
        assertThrows(JsonFormatException.class,
                () -> WorkRow.fromJson("{\"plan_id\":\"p\",\"deliverables\":\"x\"}"));
        assertThrows(JsonFormatException.class,
                () -> ReviewItemResource.fromJson("{\"item_id\":\"F\",\"state\":\"fixed\"}"));
    }
}
