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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import de.planmarshall.api.display.Display;
import de.planmarshall.api.overview.ActiveEpicSummary;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.overview.WorkLocation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@EnableGeneratorController
@DisplayName("OverviewFormatting")
class OverviewFormattingTest {

    /** The English word of every kind, as the catalogue names it. */
    private static final Map<NeedsOperatorKind, String> NEEDS_WORDS_EN = Map.ofEntries(
                        Map.entry(NeedsOperatorKind.DECISION, "decision"),
                        Map.entry(NeedsOperatorKind.WAIVER, "waiver"),
                        Map.entry(NeedsOperatorKind.WAITS_FOR_YOU, "waits for you"),
                        Map.entry(NeedsOperatorKind.STALLED, "stalled"),
                        Map.entry(NeedsOperatorKind.CONDITION, "needs a fix"),
                        Map.entry(NeedsOperatorKind.INTEGRITY_PAUSE, "integrity pause"),
                        Map.entry(NeedsOperatorKind.HELD, "held"),
                        Map.entry(NeedsOperatorKind.LANDING_FAILED, "landing failed"),
                        Map.entry(NeedsOperatorKind.FINDINGS, "new findings"),
                        Map.entry(NeedsOperatorKind.SETUP, "setup"),
                        Map.entry(NeedsOperatorKind.CREDENTIAL, "credential"),
                        Map.entry(NeedsOperatorKind.CLEANUP_DUE, "cleanup due"),
                        Map.entry(NeedsOperatorKind.OTHER, "another reason"));

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
        var normalPlan = new ActivePlanSummary("p1", "Normal", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null, null, null);
        assertFalse(OverviewFormatting.needsOperator(normalPlan));

        var pausedPlan = new ActivePlanSummary("p2", "Paused", "execute", "paused", null, null, null, 0, 0, null, null, null, null, null, "paused", 1, null, null);
        assertFalse(OverviewFormatting.needsOperator(pausedPlan));

        var decisionPlan = new ActivePlanSummary("p3", "Waiting", "refine", "waiting_for_you", null, null, null, 0, 0, null, null, null, null, null, "decision", 1, null, null);
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
    @DisplayName("sortProjects keeps the order the runtime serves, activity moves no row")
    void sortProjectsKeepsServedOrder() {
        var p1 = new ProjectSummary("p1", "Old Normal", 1, 0, 0, null, false, Instant.parse("2026-10-20T10:00:00Z"));
        var p2 = new ProjectSummary("p2", "New Normal", 1, 0, 0, null, false, Instant.parse("2026-10-20T11:00:00Z"));
        var p3 = new ProjectSummary("p3", "Attention", 1, 0, 1, null, false, Instant.parse("2026-10-20T09:00:00Z"));

        var sorted = OverviewFormatting.sortProjects(List.of(p1, p2, p3));
        assertEquals(List.of("p1", "p2", "p3"), sorted.stream().map(ProjectSummary::projectId).toList());
    }

    @Test
    @DisplayName("sortPlans keeps the order the runtime serves, activity moves no row")
    void sortPlansKeepsServedOrder() {
        var plan1 = new ActivePlanSummary("p1", "Normal", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null, null, null);
        var plan2 = new ActivePlanSummary("p2", "Needs Attention", "refine", "waiting", null, null, null, 0, 0, null, null, null, null, null, "decision", 1, null, null);

        var sorted = OverviewFormatting.sortPlans(List.of(plan1, plan2));
        assertEquals(List.of("p1", "p2"), sorted.stream().map(ActivePlanSummary::planId).toList());
    }

    @Test
    @DisplayName("sortEpics keeps the order the runtime serves, activity moves no row")
    void sortEpicsKeepsServedOrder() {
        var childPlan1 = new ActivePlanSummary("cp1", "Normal Child", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null, null, null);
        var childPlan2 = new ActivePlanSummary("cp2", "Needs Child", "refine", "waiting", null, null, null, 0, 0, null, null, null, null, null, "decision", 1, null, null);

        var epic1 = new ActiveEpicSummary("e1", "Normal Epic", null, 1, 0, 0, 1, null, null, List.of(childPlan1));
        var epic2 = new ActiveEpicSummary("e2", "Has Attention Child", null, 1, 0, 0, 1, null, null, List.of(childPlan2));

        var sorted = OverviewFormatting.sortEpics(List.of(epic1, epic2));
        assertEquals(List.of("e1", "e2"), sorted.stream().map(ActiveEpicSummary::epicId).toList());
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
    @DisplayName("Display.duration formats seconds, minutes, hours, and days from an instant")
    void durationFormatting() {
        assertEquals("", Display.duration("en", (Instant) null, clock));
        assertEquals("30 s", Display.duration("en", baseInstant.minusSeconds(30), clock));
        assertEquals("5 min", Display.duration("en", baseInstant.minusSeconds(300), clock));
        assertEquals("1 h 15 min", Display.duration("en", baseInstant.minusSeconds(4500), clock));
        assertEquals("2 h", Display.duration("en", baseInstant.minusSeconds(7200), clock));
        assertEquals("2 d", Display.duration("en", baseInstant.minusSeconds(172800), clock));
        assertEquals("2 T", Display.duration("de", baseInstant.minusSeconds(172800), clock));
    }

    @Test
    @DisplayName("formatRelativeActivity formats relative times through the display rule")
    void relativeActivity() {
        assertEquals("", OverviewFormatting.formatRelativeActivity("en", (Instant) null, clock));
        assertEquals("2 min ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(120), clock));
        assertEquals("vor 2 min", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(120), clock));
        assertEquals("3 h ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(10800), clock));
        assertEquals("vor 3 h", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(10800), clock));
        assertEquals("1 d ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(86400 + 3600), clock));
        assertEquals("vor 1 T", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(86400 + 3600), clock));
        assertEquals("3 d ago", OverviewFormatting.formatRelativeActivity("en", baseInstant.minusSeconds(86400 * 3), clock));
        assertEquals("vor 3 T", OverviewFormatting.formatRelativeActivity("de", baseInstant.minusSeconds(86400 * 3), clock));
    }

    @Test
    @DisplayName("sortPlans and sortProjects keep the served order with null instants")
    void sortComparatorTransitivity() {
        var p1 = new ProjectSummary("p1", "With Instant 10", 1, 0, 0, null, false, baseInstant.minusSeconds(100));
        var p2 = new ProjectSummary("p2", "Null Instant", 1, 0, 0, null, false, null);
        var p3 = new ProjectSummary("p3", "With Instant 20", 1, 0, 0, null, false, baseInstant.minusSeconds(50));

        var sorted = OverviewFormatting.sortProjects(List.of(p1, p2, p3));
        assertEquals(List.of("p1", "p2", "p3"), sorted.stream().map(ProjectSummary::projectId).toList());
    }

    @Test
    @DisplayName("formatEndedTime renders the moment in the short style")
    void endedTime() {
        assertEquals("", OverviewFormatting.formatEndedTime("en", (Instant) null, clock));

        // Same day: the moment itself
        var today = OverviewFormatting.formatEndedTime("en", baseInstant.minusSeconds(600), clock);
        assertTrue(today.startsWith("today "), today);

        // Yesterday: the moment of yesterday
        var yesterday = OverviewFormatting.formatEndedTime("en", baseInstant.minusSeconds(86400), clock);
        assertTrue(yesterday.startsWith("yesterday "), yesterday);

        // Older (e.g. 15 days ago): day and month
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

        for (var kind : List.of("question", "waiver", "waits_for_you", "operator_wait",
                "integrity_pause", "stalled", "condition", "held", "landing_failed", "findings", "setup")) {
            var single = OverviewFormatting.formatAttentionMarker("en", kind, 1);
            assertFalse(single.isBlank(), "Expected non-blank for single: " + kind);
            var plural = OverviewFormatting.formatAttentionMarker("en", kind, 3);
            assertTrue(plural.contains("! 3"), "Expected '! 3' in plural for: " + kind);
        }

        assertEquals("overview.needs.waiver", OverviewFormatting.needsKeyForKind("waiver"));
        assertEquals("overview.needs.waiver", OverviewFormatting.needsKeyForKind("waiver_request"),
                            "the specification's spelling reads as the waiver kind");
        assertEquals("waiver", OverviewFormatting.formatAttentionMarker("en", "waiver", 1));
        assertEquals("waiver", OverviewFormatting.formatAttentionMarker("en", "waiver_request", 1));

        var unknownKind = OverviewFormatting.formatAttentionMarker("en", "custom_attention", 1);
        assertEquals("! unknown", unknownKind);
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
    @DisplayName("Display.duration and formatRelativeActivity with ISO string overloads")
    void stringOverloads() {
        assertEquals("", Display.duration("en", (String) null, clock));
        assertEquals("", Display.duration("en", "   ", clock));
        assertEquals("invalid-date", Display.duration("en", "invalid-date", clock));
        assertEquals("5 min", Display.duration("en", "2026-10-20T11:55:00Z", clock));

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
        assertEquals("today 11:50", OverviewFormatting.formatEndedTime("en", "2026-10-20T11:50:00Z", clock));

        // 3 days ago in EN and DE: the weekday of the moment
        var threeDaysInstant = baseInstant.minusSeconds(86400 * 3);
        assertEquals("Sat 12:00", OverviewFormatting.formatEndedTime("en", threeDaysInstant, clock));
        assertEquals("Sa 12:00", OverviewFormatting.formatEndedTime("de", threeDaysInstant, clock));

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

        var normalPlan = new ActivePlanSummary("p1", "Normal", "execute", "running", null, null, null, 0, 0, null, null, null, null, null, null, null, null, null);
        var epicWithNormal = new ActiveEpicSummary("e1", "Normal", null, 1, 0, 0, 1, null, null, List.of(normalPlan));
        assertFalse(OverviewFormatting.needsOperator(epicWithNormal));

        var attentionPlan = new ActivePlanSummary("p2", "Attention", "refine", "waiting", null, null, null, 0, 0, null, null, null, null, null, "decision", 1, null, null);
        var epicWithAttention = new ActiveEpicSummary("e2", "Attention", null, 1, 1, 0, 0, "decision", 1, List.of(attentionPlan));
        assertTrue(OverviewFormatting.needsOperator(epicWithAttention));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(NeedsOperatorKind.class)
    @DisplayName("formatNeedsItem methods cover the subject, the kind word, blocks and since of every kind")
    void needsItemFormattingExhaustive(NeedsOperatorKind kind) {
        var project = new WorkLocation(Generators.letterStrings(4, 12).next(), Generators.letterStrings(4, 12).next());
        var epic = new WorkLocation(Generators.letterStrings(4, 12).next(), Generators.letterStrings(4, 12).next());
        var plan = new WorkLocation(Generators.letterStrings(4, 12).next(), Generators.letterStrings(4, 12).next());
        var recent = baseInstant.minusSeconds(120);

        assertNull(OverviewFormatting.needsKeyForKind(null));
        assertEquals("", OverviewFormatting.formatNeedsItemKind(null, "en"));

        // The kind word, in the operator's language, and English for a language the catalogue lacks.
        assertEquals(NEEDS_WORDS_EN.get(kind), OverviewFormatting.formatNeedsItemKind(kind, "en"));
        assertEquals(NEEDS_WORDS_EN.get(kind), OverviewFormatting.formatNeedsItemKind(kind, "fr"));
        var german = OverviewFormatting.formatNeedsItemKind(kind, "de");
        assertFalse(german.isBlank(), "de has no word for " + kind);
        assertFalse(german.contains("."), "a catalogue key leaked: " + german);

        // The subject: the narrowest place's own title, and the machine's word where there is none.
        var itemMachine = new NeedsOperatorItem(kind, null, null, null, null, recent, null);
        assertEquals(placelessSubject(kind), OverviewFormatting.formatNeedsItemSubject(itemMachine, "en"));
        var itemProj = new NeedsOperatorItem(kind, null, project, null, null, recent, null);
        assertEquals(project.title(), OverviewFormatting.formatNeedsItemSubject(itemProj, "en"));
        var itemEpic = new NeedsOperatorItem(kind, null, project, epic, null, recent, "epic");
        assertEquals(epic.title(), OverviewFormatting.formatNeedsItemSubject(itemEpic, "en"));
        assertEquals(epic.title(), OverviewFormatting.formatNeedsItemSubject(itemEpic, "de"));
        var itemPlan = new NeedsOperatorItem(kind, null, project, epic, plan, recent, "plan");
        assertEquals(plan.title(), OverviewFormatting.formatNeedsItemSubject(itemPlan, "en"));

        // Since.
        assertEquals("", OverviewFormatting.formatNeedsItemSince(null, "en", clock));
        assertEquals("waits since today 11:58", OverviewFormatting.formatNeedsItemSince(itemPlan.since(), "en", clock));

        // What it blocks, in the operator's words.
        assertEquals("blocks no work", OverviewFormatting.formatNeedsItemBlocks(itemProj.blocks(), "en"));
        assertEquals("blocks no work", OverviewFormatting.formatNeedsItemBlocks(itemProj.blocks(), "fr"));
        assertEquals("blockiert: unbekannt", OverviewFormatting.formatNeedsItemBlocks("other-plan", "de"));
        assertEquals("blockiert: Plan", OverviewFormatting.formatNeedsItemBlocks(itemPlan.blocks(), "de"));
        assertEquals("blocks: plan", OverviewFormatting.formatNeedsItemBlocks(itemPlan.blocks(), "en"));
        assertEquals("blocks: epic", OverviewFormatting.formatNeedsItemBlocks(itemEpic.blocks(), "en"));
    }

    /**
     * @param kind the kind of an item that belongs to no place
     * @return the English subject such an item names
     */
    private static String placelessSubject(NeedsOperatorKind kind) {
        return switch (kind) {
            case CLEANUP_DUE -> "cleanup";
            case CREDENTIAL -> "credential";
            default -> "machine";
        };
    }

    /**
     * @return per kind, an item of that kind and the command its row prints
     */
    static Stream<Arguments> needsItemCommands() {
        var recent = Instant.parse("2026-10-20T11:58:00Z");
        var project = new WorkLocation("proj-1", "Proj");
        var epic = new WorkLocation("epic-1", "Epic");
        var plan = new WorkLocation("plan-1", "Plan");
        return Stream.of(
                            Arguments.of(NeedsOperatorKind.DECISION, new NeedsOperatorItem(NeedsOperatorKind.DECISION,
                                                "/api/v1/decisions/Q-001", project, null, plan, recent, "plan"), "answer Q-001"),
                            // A link with no decision id opens the list of the open questions.
                            Arguments.of(NeedsOperatorKind.DECISION, new NeedsOperatorItem(NeedsOperatorKind.DECISION,
                                                null, project, null, null, recent, null), "questions"),
                            Arguments.of(NeedsOperatorKind.LANDING_FAILED, new NeedsOperatorItem(
                                                NeedsOperatorKind.LANDING_FAILED, null, null, epic, null, recent, null), "epic epic-1"),
                            Arguments.of(NeedsOperatorKind.LANDING_FAILED, new NeedsOperatorItem(
                                                NeedsOperatorKind.LANDING_FAILED, null, null, null, null, recent, null), "plans"),
                            // A plan's id names no epic: the failed landing of a plan alone opens that plan.
                            Arguments.of(NeedsOperatorKind.LANDING_FAILED, new NeedsOperatorItem(
                                                NeedsOperatorKind.LANDING_FAILED, null, null, null, plan, recent, "plan"), "plan plan-1"),
                            Arguments.of(NeedsOperatorKind.HELD, new NeedsOperatorItem(NeedsOperatorKind.HELD, null,
                                                null, epic, null, recent, null), "epic epic-1"),
                            Arguments.of(NeedsOperatorKind.HELD, new NeedsOperatorItem(NeedsOperatorKind.HELD, null,
                                                null, null, null, recent, null), "plans"));
    }

    @ParameterizedTest(name = "{0}: {2}")
    @MethodSource("needsItemCommands")
    @DisplayName("formatNeedsItemCommand prints the command of each kind's row")
    void needsItemCommandPerKind(NeedsOperatorKind kind, NeedsOperatorItem item, String expected) {
        assertAll(
                            () -> assertEquals(expected, OverviewFormatting.formatNeedsItemCommand(item)),
                            () -> assertFalse(OverviewFormatting.formatNeedsItemKind(kind, "en").isBlank(),
                                                "every kind has a label of its own")
        );
    }

    @Test
    @DisplayName("the printed commands follow the design's kind table, word by word")
    void needsItemCommandsFollowTheDesignTable() {
        var plan = new WorkLocation("plan-1", "Plan");
        var epic = new WorkLocation("epic-1", "Epic");
        var project = new WorkLocation("proj-1", "Project");

        assertEquals(List.of("answer", "Q-7"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.WAIVER, "/api/v1/decisions/Q-7", null, null, plan, null, null)));
        assertEquals(List.of("questions"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.WAIVER, "/api/v1/plans/plan-1", null, null, plan, null, null)));
        assertEquals(List.of("plan", "plan-1"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.STALLED, null, null, epic, plan, null, null)));
        assertEquals(List.of("epic", "epic-1"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.CONDITION, null, null, epic, null, null, null)));
        assertEquals(List.of("epic", "epic-1"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.WAITS_FOR_YOU, null, null, epic, null, null, null)));
        assertEquals(List.of("epic", "epic-1"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.HELD, null, null, epic, plan, null, null)));
        assertEquals(List.of("plan", "plan-1"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.HELD, null, null, null, plan, null, null)));
        assertEquals(List.of("epic", "findings", "epic-1"), OverviewFormatting.needsItemCommand(
                            new NeedsOperatorItem(NeedsOperatorKind.FINDINGS, null, null, epic, null, null, null)));
        assertEquals(List.of("reports"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.FINDINGS, null, null, null, null, null, null)));
        assertEquals(List.of("project", "setup", "proj-1"), OverviewFormatting.needsItemCommand(
                            new NeedsOperatorItem(NeedsOperatorKind.SETUP, null, project, null, null, null, null)));
        assertEquals(List.of("project", "setup"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.SETUP, null, null, null, null, null, null)));
        assertEquals(List.of("credentials"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.CREDENTIAL, null, null, null, null, null, null)));
        assertEquals(List.of("cleanup"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.CLEANUP_DUE, null, null, null, null, null, null)));
        assertEquals(List.of("plans"), OverviewFormatting.needsItemCommand(new NeedsOperatorItem(
                            NeedsOperatorKind.OTHER, null, null, null, plan, null, null)));
    }

    @Test
    @DisplayName("a place without a title is named as Display.title names it, never by its bare id")
    void needsItemSubjectFollowsDisplayTitle() {
        var untitled = new NeedsOperatorItem(NeedsOperatorKind.STALLED, null, null, null,
                            new WorkLocation("plan-untitled-7", null), null, null);
        var blank = new NeedsOperatorItem(NeedsOperatorKind.STALLED, null, null, null,
                            new WorkLocation("plan-untitled-7", "  "), null, null);

        var english = OverviewFormatting.formatNeedsItemSubject(untitled, "en");
        var german = OverviewFormatting.formatNeedsItemSubject(untitled, "de");

        assertEquals(Display.title("en", null, "plan-untitled-7"), english);
        assertEquals(Display.title("de", null, "plan-untitled-7"), german);
        assertEquals(english, OverviewFormatting.formatNeedsItemSubject(blank, "en"));
        assertFalse(english.contains("plan-untitled-7"), english);
        assertEquals("Plan", OverviewFormatting.formatNeedsItemSubject(new NeedsOperatorItem(
                            NeedsOperatorKind.STALLED, null, null, null, new WorkLocation("plan-1", "Plan"), null,
                            null), "en"));
    }
}
