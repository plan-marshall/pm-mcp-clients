/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import de.planmarshall.api.overview.ActiveEpicSummary;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.ProjectSummary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OverviewFormatting")
class OverviewFormattingTest {

    private final Instant baseInstant = Instant.parse("2026-10-20T12:00:00Z");
    private final Clock clock = Clock.fixed(baseInstant, ZoneOffset.UTC);

    @Test
    @DisplayName("clean sanitizes control characters and collapses whitespace")
    void cleanSanitizesAndCollapses() {
        assertEquals("", OverviewFormatting.clean(null));
        assertEquals("", OverviewFormatting.clean("   "));
        assertEquals("Clean Text", OverviewFormatting.clean(" Clean Text \n "));
        assertEquals("Red Alert", OverviewFormatting.clean("\u001B[31mRed Alert\u001B[0m"));
        assertEquals("A B C", OverviewFormatting.clean("A\tB\nC"));
    }

    @Test
    @DisplayName("needsOperator evaluates plans correctly, ignoring paused alone")
    void planNeedsOperator() {
        var normalPlan = new ActivePlanSummary("p1", "Normal", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null);
        assertFalse(OverviewFormatting.needsOperator(normalPlan));

        var pausedPlan = new ActivePlanSummary("p2", "Paused", "execute", "paused", null, null, null, 0, 0, null, null, null, null, null, "paused", 1);
        assertFalse(OverviewFormatting.needsOperator(pausedPlan));

        var decisionPlan = new ActivePlanSummary("p3", "Waiting", "refine", "waiting_for_you", null, null, null, 0, 0, null, null, null, null, null, "decision", 1);
        assertTrue(OverviewFormatting.needsOperator(decisionPlan));
    }

    @Test
    @DisplayName("needsOperator evaluates projects and setup correctly")
    void projectNeedsOperator() {
        var normalProject = new ProjectSummary("proj1", "Proj 1", 1, 0, 0, null, false, null);
        assertFalse(OverviewFormatting.needsOperator(normalProject));
        assertEquals(0, OverviewFormatting.projectAttentionCount(normalProject));

        var setupProject = new ProjectSummary("proj2", "Proj 2", 0, 0, 0, null, true, null);
        assertTrue(OverviewFormatting.needsOperator(setupProject));
        assertEquals(1, OverviewFormatting.projectAttentionCount(setupProject));

        var busyProject = new ProjectSummary("proj3", "Proj 3", 2, 1, 3, null, false, null);
        assertTrue(OverviewFormatting.needsOperator(busyProject));
        assertEquals(3, OverviewFormatting.projectAttentionCount(busyProject));
    }

    @Test
    @DisplayName("sortProjects prioritizes attention projects then recent activity")
    void sortProjects() {
        var p1 = new ProjectSummary("p1", "Old Normal", 1, 0, 0, null, false, Instant.parse("2026-10-20T10:00:00Z"));
        var p2 = new ProjectSummary("p2", "New Normal", 1, 0, 0, null, false, Instant.parse("2026-10-20T11:00:00Z"));
        var p3 = new ProjectSummary("p3", "Attention", 1, 0, 1, null, false, Instant.parse("2026-10-20T09:00:00Z"));

        var sorted = OverviewFormatting.sortProjects(List.of(p1, p2, p3));
        assertEquals(List.of("p3", "p2", "p1"), sorted.stream().map(ProjectSummary::projectId).toList());
    }

    @Test
    @DisplayName("sortPlans prioritizes attention plans")
    void sortPlans() {
        var plan1 = new ActivePlanSummary("p1", "Normal", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null);
        var plan2 = new ActivePlanSummary("p2", "Needs Attention", "refine", "waiting", null, null, null, 0, 0, null, null, null, null, null, "decision", 1);

        var sorted = OverviewFormatting.sortPlans(List.of(plan1, plan2));
        assertEquals(List.of("p2", "p1"), sorted.stream().map(ActivePlanSummary::planId).toList());
    }

    @Test
    @DisplayName("sortEpics prioritizes attention epics and epics with attention plans")
    void sortEpics() {
        var childPlan1 = new ActivePlanSummary("cp1", "Normal Child", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null);
        var childPlan2 = new ActivePlanSummary("cp2", "Needs Child", "refine", "waiting", null, null, null, 0, 0, null, null, null, null, null, "decision", 1);

        var epic1 = new ActiveEpicSummary("e1", "Normal Epic", null, 1, 0, 0, 1, null, null, List.of(childPlan1));
        var epic2 = new ActiveEpicSummary("e2", "Has Attention Child", null, 1, 0, 0, 1, null, null, List.of(childPlan2));

        var sorted = OverviewFormatting.sortEpics(List.of(epic1, epic2));
        assertEquals(List.of("e2", "e1"), sorted.stream().map(ActiveEpicSummary::epicId).toList());
    }

    @Test
    @DisplayName("formatAttentionMarker formats single and plural counts")
    void attentionMarkers() {
        assertEquals("", OverviewFormatting.formatAttentionMarker("en", null, 0));
        assertEquals("", OverviewFormatting.formatAttentionMarker("en", "paused", 1));

        var singleDecision = OverviewFormatting.formatAttentionMarker("en", "decision", 1);
        assertTrue(singleDecision.contains("decision") || singleDecision.contains("!"), singleDecision);

        var multipleDecisions = OverviewFormatting.formatAttentionMarker("en", "decision", 2);
        assertTrue(multipleDecisions.contains("! 2"), multipleDecisions);

        var germanDecision = OverviewFormatting.formatAttentionMarker("de", "decision", 1);
        assertTrue(germanDecision.contains("Entscheidung") || germanDecision.contains("!"), germanDecision);
    }

    @Test
    @DisplayName("formatPhaseState localizes states in English and German")
    void phaseStateLocalization() {
        assertEquals("running", OverviewFormatting.formatPhaseState("en", "running", null));
        assertEquals("läuft", OverviewFormatting.formatPhaseState("de", "running", null));

        assertEquals("paused", OverviewFormatting.formatPhaseState("en", "paused", null));
        assertEquals("angehalten", OverviewFormatting.formatPhaseState("de", "paused", null));

        assertEquals("waiting for you", OverviewFormatting.formatPhaseState("en", "waiting_for_you", null));
        assertEquals("wartet auf dich", OverviewFormatting.formatPhaseState("de", "waiting_for_you", null));

        assertEquals("held", OverviewFormatting.formatPhaseState("en", "held", null));
        assertEquals("aufgehalten", OverviewFormatting.formatPhaseState("de", "held", null));
    }

    @Test
    @DisplayName("formatDuration formats seconds, minutes, hours")
    void durationFormatting() {
        assertEquals("", OverviewFormatting.formatDuration((Instant) null, clock));
        assertEquals("30 s", OverviewFormatting.formatDuration(baseInstant.minusSeconds(30), clock));
        assertEquals("5 min", OverviewFormatting.formatDuration(baseInstant.minusSeconds(300), clock));
        assertEquals("1 h 15 min", OverviewFormatting.formatDuration(baseInstant.minusSeconds(4500), clock));
        assertEquals("2 h", OverviewFormatting.formatDuration(baseInstant.minusSeconds(7200), clock));
    }

    @Test
    @DisplayName("formatRelativeActivity formats relative times")
    void relativeActivity() {
        assertEquals("", OverviewFormatting.formatRelativeActivity("en", (Instant) null, clock));
        assertEquals("2 min ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(120), clock));
        assertEquals("vor 2 Min.", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(120), clock));
        assertEquals("3 h ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(10800), clock));
        assertEquals("vor 3 Std.", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(10800), clock));
        assertEquals("1 day ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(86400 + 3600), clock));
        assertEquals("vor 1 Tag", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(86400 + 3600), clock));
        assertEquals("3 days ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(86400 * 3), clock));
        assertEquals("vor 3 Tagen", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(86400 * 3), clock));
    }

    @Test
    @DisplayName("sortPlans and sortProjects comparator is transitive with null instants")
    void sortComparatorTransitivity() {
        var p1 = new ProjectSummary("p1", "With Instant 10", 1, 0, 0, null, false, baseInstant.minusSeconds(100));
        var p2 = new ProjectSummary("p2", "Null Instant", 1, 0, 0, null, false, null);
        var p3 = new ProjectSummary("p3", "With Instant 20", 1, 0, 0, null, false, baseInstant.minusSeconds(50));

        var sorted = OverviewFormatting.sortProjects(List.of(p1, p2, p3));
        assertEquals(List.of("p3", "p1", "p2"), sorted.stream().map(ProjectSummary::projectId).toList());
    }

    @Test
    @DisplayName("formatEndedTime handles today, yesterday, and older dates")
    void endedTime() {
        assertEquals("", OverviewFormatting.formatEndedTime("en", (Instant) null, clock));

        // Same day: relative
        var today = OverviewFormatting.formatEndedTime("en", baseInstant.minusSeconds(600), clock);
        assertTrue(today.contains("ago"), today);

        // Yesterday
        var yesterday = OverviewFormatting.formatEndedTime("en", baseInstant.minusSeconds(86400), clock);
        assertTrue(yesterday.startsWith("yesterday "), yesterday);

        // Older (e.g. 15 days ago)
        var older = OverviewFormatting.formatEndedTime("en", baseInstant.minusSeconds(86400 * 15), clock);
        assertTrue(older.contains("Oct"), older);
    }

    @Test
    @DisplayName("osc8Link formats terminal hyperlinks safely")
    void osc8Hyperlink() {
        assertEquals("Label", OverviewFormatting.osc8Link(null, "Label"));
        assertEquals("Label", OverviewFormatting.osc8Link("", "Label"));
        assertEquals("\u001B]8;;https://example.com/pr/1\u001B\\#1\u001B]8;;\u001B\\",
                            OverviewFormatting.osc8Link("https://example.com/pr/1", "#1"));
    }

    @Test
    @DisplayName("formatAttentionMarker covers all kinds, invalid counts, and localized labels")
    void attentionMarkerExhaustive() {
        assertEquals("", OverviewFormatting.formatAttentionMarker("en", "decision", null));
        assertEquals("", OverviewFormatting.formatAttentionMarker("en", "decision", 0));
        assertEquals("", OverviewFormatting.formatAttentionMarker("en", "decision", -1));
        assertEquals("", OverviewFormatting.formatAttentionMarker("en", "   ", 2));

        for (var kind : List.of("question", "waiver", "waiver_request", "waits_for_you", "operator_wait",
                "integrity_pause", "stalled", "condition", "held", "landing_failed", "findings", "setup")) {
            var single = OverviewFormatting.formatAttentionMarker("en", kind, 1);
            assertFalse(single.isBlank(), "Expected non-blank for single: " + kind);
            var plural = OverviewFormatting.formatAttentionMarker("en", kind, 3);
            assertTrue(plural.contains("! 3"), "Expected '! 3' in plural for: " + kind);
        }

        var unknownKind = OverviewFormatting.formatAttentionMarker("en", "custom_attention", 1);
        assertEquals("! custom_attention", unknownKind);
        var unknownPlural = OverviewFormatting.formatAttentionMarker("en", "custom_attention", 2);
        assertTrue(unknownPlural.contains("! 2"), unknownPlural);
    }

    @Test
    @DisplayName("formatPhaseState covers pausing points and unknown states")
    void phaseStateExhaustive() {
        assertEquals("running", OverviewFormatting.formatPhaseState("en", null, null));
        assertEquals("running", OverviewFormatting.formatPhaseState("en", "   ", null));

        var pausingTask = OverviewFormatting.formatPhaseState("en", "pausing", "task");
        assertTrue(pausingTask.contains("task"), pausingTask);

        var pausingDefault = OverviewFormatting.formatPhaseState("en", "pausing", null);
        assertTrue(pausingDefault.contains("phase"), pausingDefault);

        assertEquals("waiting for you", OverviewFormatting.formatPhaseState("en", "waiting", null));
        assertEquals("custom_phase", OverviewFormatting.formatPhaseState("en", "custom_phase", null));
    }

    @Test
    @DisplayName("formatDuration and formatRelativeActivity with ISO string overloads")
    void stringOverloads() {
        assertEquals("", OverviewFormatting.formatDuration((String) null, clock));
        assertEquals("", OverviewFormatting.formatDuration("   ", clock));
        assertEquals("invalid-date", OverviewFormatting.formatDuration("invalid-date", clock));
        assertEquals("5 min", OverviewFormatting.formatDuration("2026-10-20T11:55:00Z", clock));

        assertEquals("", OverviewFormatting.formatRelativeActivity("en", (String) null, clock));
        assertEquals("", OverviewFormatting.formatRelativeActivity("en", "  ", clock));
        assertEquals("custom-str", OverviewFormatting.formatRelativeActivity("en", "custom-str", clock));
        assertEquals("5 min ago", OverviewFormatting.formatRelativeActivity("en", "2026-10-20T11:55:00Z", clock));
    }

    @Test
    @DisplayName("formatEndedTime with ISO string overloads and 3-7 day range")
    void endedTimeExhaustive() {
        assertEquals("", OverviewFormatting.formatEndedTime("en", (String) null, clock));
        assertEquals("", OverviewFormatting.formatEndedTime("en", "   ", clock));
        assertEquals("bad-date", OverviewFormatting.formatEndedTime("en", "bad-date", clock));
        assertEquals("10 min ago", OverviewFormatting.formatEndedTime("en", "2026-10-20T11:50:00Z", clock));

        // 3 days ago in EN and DE
        var threeDaysInstant = baseInstant.minusSeconds(86400 * 3);
        assertEquals("3 days ago", OverviewFormatting.formatEndedTime("en", threeDaysInstant, clock));
        assertEquals("vor 3 Tagen", OverviewFormatting.formatEndedTime("de", threeDaysInstant, clock));

        // 10 days ago in DE
        var tenDaysInstant = baseInstant.minusSeconds(86400 * 10);
        var tenDaysDe = OverviewFormatting.formatEndedTime("de", tenDaysInstant, clock);
        assertTrue(tenDaysDe.contains("Okt"), tenDaysDe);
    }

    @Test
    @DisplayName("needsOperator for ActiveEpicSummary evaluates plans presence")
    void epicNeedsOperator() {
        var epicNoPlans = new ActiveEpicSummary("e0", "Empty", null, 0, 0, 0, 0, null, null, null);
        assertFalse(OverviewFormatting.needsOperator(epicNoPlans));

        var normalPlan = new ActivePlanSummary("p1", "Normal", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null);
        var epicWithNormal = new ActiveEpicSummary("e1", "Normal", null, 1, 0, 0, 1, null, null, List.of(normalPlan));
        assertFalse(OverviewFormatting.needsOperator(epicWithNormal));

        var attentionPlan = new ActivePlanSummary("p2", "Attention", "refine", "waiting", null, null, null, 0, 0, null, null, null, null, null, "decision", 1);
        var epicWithAttention = new ActiveEpicSummary("e2", "Attention", null, 1, 1, 0, 0, "decision", 1, List.of(attentionPlan));
        assertTrue(OverviewFormatting.needsOperator(epicWithAttention));
    }
}
