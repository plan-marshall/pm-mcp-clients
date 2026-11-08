/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The cleanup dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} deletes nothing, an
 * unknown input re-asks, and the main path sends the delete.
 */
@DisplayName("pm-operator cleanup dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class CleanupDialogueIT {

    private static final String PROMPT = "cannot be undone";

    @Test
    @DisplayName("q first: the dialogue leaves and deletes nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("cleanup.preview");
             var session = harness.open("cleanup", "--delete")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Nothing was sent."), session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("an unknown input re-asks and deletes nothing")
    void unknownInputReAsks() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("cleanup.preview");
             var session = harness.open("cleanup", "--delete")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "zzz\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.rawOutput(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: the delete act arrives")
    void mainPathDeletes() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("cleanup.preview");
             var session = harness.open("cleanup", "--delete")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "y\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Deleted"), session.screenText());
            var recorded = harness.acts();
            assertTrue(recorded.stream().anyMatch(act -> "POST".equals(act.method())
                                && act.path().startsWith("/api/v1/system/cleanup/")
                                && act.path().endsWith("/delete")), recorded.toString());
        }
    }

    @Test
    @DisplayName("the console bar asks the decision, and q at the confirmation deletes nothing")
    void consoleBarLeavesTheDeleteUnsaved() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("cleanup.preview");
             var session = harness.open()) {
            assertTrue(session.awaitConsolePrompt(DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "cleanup\n");
            assertTrue(session.awaitText("q) leave", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "d\n");
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitText("Nothing was sent.", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "exit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertNoCleanupAct(harness);
        }
    }

    @Test
    @DisplayName("x leaves an item out, and the delete keeps it")
    void consoleLeaveOutMarksTheItem() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("cleanup.preview");
             var session = harness.open()) {
            assertTrue(session.awaitConsolePrompt(DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "cleanup\n");
            assertTrue(session.awaitText("q) leave", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "x\n1\n");
            assertTrue(session.awaitText("kept for good", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "d\n");
            assertTrue(session.awaitText("1 unchecked item is kept for good.", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            DialogueHarness.type(session, "q\nexit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertNoCleanupAct(harness);
        }
    }

    /**
     * The console's own startup act ({@code PUT /api/v1/web}) is not the cleanup's: only a
     * {@code /api/v1/system/cleanup} act proves that a delete or a keep was sent.
     *
     * @param harness the harness whose recorded acts are read
     * @throws IOException if the read fails
     */
    private static void assertNoCleanupAct(DialogueHarness harness) throws IOException {
        var acts = harness.acts().stream().filter(act -> act.path().startsWith("/api/v1/system/cleanup")).toList();
        assertTrue(acts.isEmpty(), "expected no cleanup act, got " + acts);
    }
}
