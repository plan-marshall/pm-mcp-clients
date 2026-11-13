/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jline.terminal.impl.DumbTerminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.display.Display;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.WorkLocation;
import de.planmarshall.operator.overview.OverviewFormatting;
import de.planmarshall.operator.overview.OverviewFullScreenApp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The command of an item of what waits is one derivation: the line {@code needs} prints and the act
 * {@code !} runs in the overview are the same words, the ones the kind table of
 * {@code doc/design/terminal-client/04-console.adoc#pushed} names.
 */
@EnableGeneratorController
@DisplayName("What waits: ! runs the command needs prints")
class NeedsItemCommandsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    /** Where an item of a case belongs. */
    enum Place {
        PLAN, EPIC, PLAN_AND_EPIC, PROJECT, NONE
    }

    /**
     * One row of the design's kind table.
     *
     * @param kind     the item's kind
     * @param place    where the item belongs
     * @param decision whether the item's link names a decision
     * @param expected the command the design names, with {@code {plan}}, {@code {epic}},
     *                 {@code {project}} and {@code {decision}} for the item's ids
     */
    record Case(NeedsOperatorKind kind, Place place, boolean decision, String expected) {

        @Override
        public String toString() {
            return kind.value() + " of " + place + (decision ? " with a decision link" : "") + " -> " + expected;
        }
    }

    static Stream<Case> designTable() {
        return Stream.of(
                            new Case(NeedsOperatorKind.DECISION, Place.PLAN, true, "answer {decision}"),
                            new Case(NeedsOperatorKind.DECISION, Place.PLAN, false, "questions"),
                            new Case(NeedsOperatorKind.WAIVER, Place.PLAN, true, "answer {decision}"),
                            new Case(NeedsOperatorKind.WAIVER, Place.EPIC, false, "questions"),
                            new Case(NeedsOperatorKind.WAITS_FOR_YOU, Place.PLAN, false, "plan {plan}"),
                            new Case(NeedsOperatorKind.WAITS_FOR_YOU, Place.EPIC, false, "epic {epic}"),
                            new Case(NeedsOperatorKind.STALLED, Place.PLAN_AND_EPIC, false, "plan {plan}"),
                            new Case(NeedsOperatorKind.STALLED, Place.EPIC, false, "epic {epic}"),
                            new Case(NeedsOperatorKind.CONDITION, Place.PLAN, false, "plan {plan}"),
                            new Case(NeedsOperatorKind.CONDITION, Place.EPIC, false, "epic {epic}"),
                            new Case(NeedsOperatorKind.INTEGRITY_PAUSE, Place.PLAN, false, "plan {plan}"),
                            new Case(NeedsOperatorKind.HELD, Place.EPIC, false, "epic {epic}"),
                            new Case(NeedsOperatorKind.HELD, Place.PLAN_AND_EPIC, false, "epic {epic}"),
                            new Case(NeedsOperatorKind.HELD, Place.PLAN, false, "plan {plan}"),
                            new Case(NeedsOperatorKind.LANDING_FAILED, Place.EPIC, false, "epic {epic}"),
                            new Case(NeedsOperatorKind.LANDING_FAILED, Place.PLAN, false, "plan {plan}"),
                            new Case(NeedsOperatorKind.LANDING_FAILED, Place.NONE, false, "plans"),
                            new Case(NeedsOperatorKind.FINDINGS, Place.EPIC, false, "epic findings {epic}"),
                            new Case(NeedsOperatorKind.FINDINGS, Place.NONE, false, "reports"),
                            new Case(NeedsOperatorKind.SETUP, Place.PROJECT, false, "project setup {project}"),
                            new Case(NeedsOperatorKind.SETUP, Place.NONE, false, "project setup"),
                            new Case(NeedsOperatorKind.CREDENTIAL, Place.NONE, false, "credentials"),
                            new Case(NeedsOperatorKind.CLEANUP_DUE, Place.NONE, false, "cleanup"),
                            new Case(NeedsOperatorKind.OTHER, Place.PLAN, false, "plans"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("designTable")
    @DisplayName("! on a row runs exactly the command the row prints, as the design names it")
    void bangRunsThePrintedCommand(Case testCase) throws Exception {
        var planId = Generators.letterStrings(4, 12).next();
        var epicId = Generators.letterStrings(4, 12).next();
        var projectId = Generators.letterStrings(4, 12).next();
        var decisionId = Generators.letterStrings(4, 12).next();
        var item = itemOf(testCase, planId, epicId, projectId, decisionId);
        var expected = testCase.expected()
                            .replace("{plan}", planId)
                            .replace("{epic}", epicId)
                            .replace("{project}", projectId)
                            .replace("{decision}", decisionId);
        var app = app();
        var ran = new ArrayList<List<String>>();
        app.verbRunner(args -> {
            ran.add(List.of(args));
            return 0;
        });

        app.openNeedsItemAct(item);

        assertAll(
                            () -> assertEquals(expected, OverviewFormatting.formatNeedsItemCommand(item),
                                                "the printed command follows the design"),
                            () -> assertEquals(1, ran.size(), ran.toString()),
                            () -> assertEquals(expected, String.join(" ", ran.getFirst()),
                                                "! runs what the row prints"),
                            () -> assertEquals(OverviewFormatting.needsItemCommand(item), ran.getFirst())
        );
    }

    @Test
    @DisplayName("the table covers every kind of the closed list")
    void everyKindIsCovered() {
        var covered = EnumSet.noneOf(NeedsOperatorKind.class);
        designTable().forEach(testCase -> covered.add(testCase.kind()));

        assertEquals(EnumSet.allOf(NeedsOperatorKind.class), covered);
    }

    @ParameterizedTest(name = "{0} prints a registered verb")
    @EnumSource(NeedsOperatorKind.class)
    @DisplayName("every kind, in every place, prints a verb the operator registers")
    void everyKindPrintsARegisteredVerb(NeedsOperatorKind kind) {
        var planId = Generators.letterStrings(4, 12).next();
        var epicId = Generators.letterStrings(4, 12).next();
        var projectId = Generators.letterStrings(4, 12).next();
        var decisionId = Generators.letterStrings(4, 12).next();
        var plan = new WorkLocation(planId, null);
        var epic = new WorkLocation(epicId, null);
        var project = new WorkLocation(projectId, null);
        var items = List.of(
                            item(kind, "/api/v1/decisions/" + decisionId, project, epic, plan, "plan"),
                            item(kind, "/api/v1/plans/" + planId, null, null, plan, "plan"),
                            item(kind, "/api/v1/epics/" + epicId, null, epic, null, "epic"),
                            item(kind, null, project, null, null, null),
                            item(kind, null, null, null, null, null));

        for (var item : items) {
            var command = OverviewFormatting.formatNeedsItemCommand(item);
            var verb = command.split(" ", 2)[0];
            assertTrue(OperatorVerbs.registered().contains(verb),
                                kind + " prints " + command + ", and " + verb + " is no registered verb");
            assertFalse(command.contains("hold"), kind + " prints a hold verb: " + command);
        }
    }

    @Test
    @DisplayName("a failed landing opens the epic that owns it, and a plan alone opens that plan")
    void landingFailedOpensTheEpic() {
        var planId = Generators.letterStrings(4, 12).next();
        var epicId = Generators.letterStrings(4, 12).next();
        var plan = new WorkLocation(planId, null);

        assertEquals("epic " + epicId, OverviewFormatting.formatNeedsItemCommand(item(
                            NeedsOperatorKind.LANDING_FAILED, "/api/v1/plans/" + planId, null,
                            new WorkLocation(epicId, null), plan, "plan")));
        assertEquals("plan " + planId, OverviewFormatting.formatNeedsItemCommand(item(
                            NeedsOperatorKind.LANDING_FAILED, "/api/v1/plans/" + planId, null, null, plan, "plan")),
                            "a plan's id names no epic, so the plan alone opens that plan");
    }

    @Test
    @DisplayName("a decision takes its id from the item's own link, never from a store of its own")
    void decisionTakesItsIdFromTheLink() {
        var decisionId = Generators.letterStrings(4, 12).next();
        var item = item(NeedsOperatorKind.DECISION, "/api/v1/decisions/" + decisionId,
                            new WorkLocation(Generators.letterStrings(4, 12).next(), null), null,
                            new WorkLocation(Generators.letterStrings(4, 12).next(), null), "plan");

        assertEquals("answer " + decisionId, OverviewFormatting.formatNeedsItemCommand(item));
    }

    @Test
    @DisplayName("names the subject by its title, never by its raw id")
    void subjectNamesTheTitle() {
        var planId = Generators.letterStrings(4, 12).next();
        var title = Generators.letterStrings(4, 12).next();
        var titled = item(NeedsOperatorKind.DECISION, "/api/v1/decisions/" + planId, null, null,
                            new WorkLocation(planId, title), "plan");
        var untitled = item(NeedsOperatorKind.CONDITION, "/api/v1/plans/" + planId, null, null,
                            new WorkLocation(planId, null), "plan");

        assertEquals(title, OverviewFormatting.formatNeedsItemSubject(titled, "en"));
        var subject = OverviewFormatting.formatNeedsItemSubject(untitled, "de");
        assertEquals(Display.title("de", null, planId), subject,
                            "a place without a title is named as the display layer names it");
        assertFalse(subject.contains(planId), "the bare id reached the operator: " + subject);
    }

    @Test
    @DisplayName("renders what an item blocks in the operator's words, never the wire token")
    void blocksAreCatalogueWords() {
        assertEquals(Map.of("en", "blocks: plan", "de", "blockiert: Plan"),
                            Map.of("en", OverviewFormatting.formatNeedsItemBlocks("plan", "en"),
                                                "de", OverviewFormatting.formatNeedsItemBlocks("plan", "de")));
    }

    @ParameterizedTest(name = "{0} names its kind in both languages")
    @EnumSource(NeedsOperatorKind.class)
    @DisplayName("every kind has its catalogue word in both languages")
    void everyKindHasItsWord(NeedsOperatorKind kind) {
        for (var locale : List.of("en", "de")) {
            var word = OverviewFormatting.formatNeedsItemKind(kind, locale);
            assertFalse(word.isBlank(), locale + " has no word for " + kind);
            assertFalse(word.contains("."), "a catalogue key leaked: " + word);
        }
    }

    @ParameterizedTest(name = "{0} names its place by the place's own title")
    @EnumSource(NeedsOperatorKind.class)
    @DisplayName("every kind names its subject, in both languages")
    void everyKindNamesItsSubject(NeedsOperatorKind kind) {
        var planTitle = Generators.letterStrings(4, 12).next();
        var epicTitle = Generators.letterStrings(4, 12).next();
        var epic = new WorkLocation(Generators.letterStrings(4, 12).next(), epicTitle);
        var withPlan = item(kind, null, null, epic,
                            new WorkLocation(Generators.letterStrings(4, 12).next(), planTitle), "plan");
        assertEquals(planTitle, OverviewFormatting.formatNeedsItemSubject(withPlan, "en"),
                            "the plan's own title is the subject");

        var epicOnly = item(kind, null, null, epic, null, "epic");
        for (var locale : List.of("en", "de")) {
            var subject = OverviewFormatting.formatNeedsItemSubject(epicOnly, locale);
            assertEquals(OverviewFormatting.clean(epicTitle), subject,
                                locale + " names the epic's own title for " + kind);
        }
    }

    private static NeedsOperatorItem item(NeedsOperatorKind kind, @Nullable String link,
                        @Nullable WorkLocation project, @Nullable WorkLocation epic, @Nullable WorkLocation plan,
                        @Nullable String blocks) {
        return new NeedsOperatorItem(kind, link, project, epic, plan, Instant.parse("2026-10-16T15:05:00Z"), blocks);
    }

    private static NeedsOperatorItem itemOf(Case testCase, String planId, String epicId, String projectId,
                        String decisionId) {
        var place = testCase.place();
        var plan = place == Place.PLAN || place == Place.PLAN_AND_EPIC ? new WorkLocation(planId, null) : null;
        var epic = place == Place.EPIC || place == Place.PLAN_AND_EPIC ? new WorkLocation(epicId, null) : null;
        var project = place == Place.PROJECT ? new WorkLocation(projectId, null) : null;
        return new NeedsOperatorItem(testCase.kind(), linkOf(testCase, planId, epicId, decisionId), project, epic,
                            plan, Instant.parse("2026-10-16T15:00:00Z"), null);
    }

    private static @Nullable String linkOf(Case testCase, String planId, String epicId, String decisionId) {
        if (testCase.decision()) {
            return "/api/v1/decisions/" + decisionId;
        }
        // A decision whose link names its place, not the decision: the command is the list of the
        // open questions, never a bare answer the verb refuses.
        return switch (testCase.place()) {
            case PLAN, PLAN_AND_EPIC -> "/api/v1/plans/" + planId;
            case EPIC -> "/api/v1/epics/" + epicId;
            default -> null;
        };
    }

    private OverviewFullScreenApp app() throws IOException {
        var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        var context = new OperatorContext(Map.of(), tempDir, true, ClientEnvironment.capture(Map.of()),
                            () -> terminal, null);
        return new OverviewFullScreenApp(context, terminal, OverviewFullScreenApp.InitialSelection.DEFAULT, CLOCK,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
    }
}
