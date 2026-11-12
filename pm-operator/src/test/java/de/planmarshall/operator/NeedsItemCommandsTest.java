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

import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.WorkLocation;
import de.planmarshall.operator.overview.OverviewFormatting;
import de.planmarshall.operator.overview.OverviewFullScreenApp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
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
