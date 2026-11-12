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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * PTY tests for PLAN-84 D1: the console's {@code /} field lists the matches of
 * {@code running-work} and opens the one selected; {@code Esc} leaves the field with nothing sent
 * (shared/needs-and-find.adoc#terminal).
 */
@DisplayName("pm-operator / lists the matches and opens one on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class FindIT {

    /** The hint of the console's find field ({@code overview.find.hint}). */
    private static final String FIND_HINT = "A part of an id or a title, or a pull request number";

    /** The header of an epic's view, so the case proves the chosen match opened where it belongs. */
    private static final String EPIC_VIEW = "\u00b7 epic \u00b7 running";

    private static final Duration POLL = Duration.ofMillis(25);

    @Test
    @DisplayName("/relay lists the matches and selecting the first opens its view")
    void slashListsAndOpensTheFirstMatch() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open()) {
            awaitPrompt(session);

            DialogueHarness.type(session, "/relay\n");
            awaitText(session, "Go to work by name");
            awaitText(session, "Fix Relay Transport");

            DialogueHarness.type(session, "1\n");
            awaitText(session, EPIC_VIEW);
        }
    }

    @Test
    @DisplayName("Esc leaves the find field with nothing sent")
    void escLeavesTheFieldWithoutARequest() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open()) {
            awaitPrompt(session);

            DialogueHarness.type(session, "/\n");
            awaitText(session, FIND_HINT);
            DialogueHarness.type(session, "\u001b");
            awaitConsolePrompt(session);

            DialogueHarness.type(session, "exit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertFalse(session.rawOutput().contains("Fix Relay Transport"),
                                "no match was listed, so nothing was searched:\n" + session.rawOutput());
            assertFalse(session.rawOutput().contains("Nothing matches"),
                                "no search ran at all, not even one without a match:\n" + session.rawOutput());
        }
    }

    @Test
    @DisplayName("Ctrl+C leaves the find field and finds the prompt again")
    void ctrlCLeavesTheField() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open()) {
            awaitPrompt(session);

            DialogueHarness.type(session, "/\n");
            awaitText(session, FIND_HINT);
            DialogueHarness.type(session, "re");
            // Ctrl+C on a real tty: the field leaves as Esc does, and the console keeps running.
            DialogueHarness.type(session, "\u0003");

            awaitConsolePrompt(session);

            DialogueHarness.type(session, "exit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertFalse(session.rawOutput().contains("Fix Relay Transport"),
                                "no match was listed, so nothing was searched:\n" + session.rawOutput());
        }
    }

    private static void awaitPrompt(PtySession session) throws InterruptedException {
        assertTrue(session.awaitConsolePrompt(DialogueHarness.TIMEOUT),
                            () -> "the console prompt did not appear in:\n" + session.screenText());
    }

    /**
     * Waits until the console has drawn its prompt as the screen's last line again: the view or the
     * field that owned the prompt closed. The console is line-oriented, so what it printed earlier
     * stays in the scrollback; the prompt line is the condition that says it is back.
     *
     * @param session the running session
     * @throws InterruptedException if interrupted
     */
    private static void awaitConsolePrompt(PtySession session) throws InterruptedException {
        var deadline = System.nanoTime() + DialogueHarness.TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (lastLine(session).startsWith("pm ")) {
                return;
            }
            LockSupport.parkNanos(POLL.toNanos());
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        fail("the console prompt did not come back as the last line:\n" + session.screenText());
    }

    private static String lastLine(PtySession session) {
        var lines = session.screenText().lines().toList();
        for (int index = lines.size() - 1; index >= 0; index--) {
            if (!lines.get(index).isBlank()) {
                return lines.get(index).strip();
            }
        }
        return "";
    }

    private static void awaitText(PtySession session, String text) throws InterruptedException {
        assertTrue(session.awaitText(text, DialogueHarness.TIMEOUT),
                            () -> "text '" + text + "' did not appear in:\n" + session.screenText());
    }
}
