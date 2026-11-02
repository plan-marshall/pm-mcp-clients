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
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * {@code Ctrl+C} typed into a dialogue a full-screen view opened, on a direct verb rather than the
 * console ({@code CLI-CC-S-01}): the view suspends, the dialogue reads the same terminal, the typed
 * key reaches the reader, and the process leaves with the interrupt status of the exit-code table,
 * {@code 130}, having sent nothing to the runtime.
 *
 * <p>Every case drives the real view on a real pseudo-terminal: {@code plans} (or, for the hold, the
 * plan view of the held plan) is opened as a direct verb, the row is selected with the view's own
 * keys where the level has rows, the actions menu opens on {@code a}, and the dialogue is the entry
 * the menu runs. The interrupt is typed as {@code \u0003}, never raised as a signal, so a dialogue
 * that reads a second terminal cannot pass by accident.
 */
@DisplayName("Ctrl+C typed into a dialogue a view opened leaves with 130 and sends nothing")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ViewDialogueInterruptIT {

    private static final String INTERRUPT = "\u0003";
    private static final int EXIT_INTERRUPTED = 130;
    private static final Duration POLL = Duration.ofMillis(25);

    @Test
    @DisplayName("work action: the pause of a plan opened from the plans view leaves with 130")
    void workActionInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plans", "--project", "pm-mcp-clients")) {
            awaitText(session, "by plan");
            select(session, "Fix Relay Transport");
            type(session, "a");
            awaitText(session, "Pause\u2026");
            type(session, "p");
            awaitText(session, "Where does fix-relay hold?");

            type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("hold: the change confirmation of a held plan opened from the plan view leaves with 130")
    void holdInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "relay-cursor-order")) {
            awaitText(session, "relay-cursor-order");
            type(session, "a");
            awaitText(session, "Accept the change\u2026");
            type(session, "c");
            awaitText(session, "y) yes");

            type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("review: the findings review of an epic opened from the plans view leaves with 130")
    void reviewInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plans", "--project", "pm-mcp-clients", "--by", "epic")) {
            awaitText(session, "by epic");
            select(session, "Relay Hardening");
            type(session, "a");
            awaitText(session, "Findings review");
            type(session, "f");
            awaitText(session, "Reconnect loses the last event");

            type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("runtime act: the stop confirmation opened from the plans view's system window leaves with 130")
    void runtimeActInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("plans", "--project", "pm-mcp-clients")) {
            awaitText(session, "by plan");
            type(session, "S");
            awaitText(session, "Runtime");
            type(session, "a");
            awaitText(session, "Stop\u2026");
            type(session, "s");
            awaitText(session, "Stop the runtime?");

            type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    private static void type(PtySession session, String keys) throws Exception {
        DialogueHarness.type(session, keys);
    }

    private static void awaitText(PtySession session, String marker) throws Exception {
        assertTrue(session.awaitText(marker, DialogueHarness.TIMEOUT),
                            marker + " never drawn:\n" + session.screenText());
    }

    /**
     * Moves the view's selection onto the row whose title the text holds: the case keeps pressing
     * the view's own down key until the cursor row carries the title, and waits on the row the view
     * draws rather than on a fixed pause.
     *
     * @param session the running view
     * @param title   the row title to reach
     * @throws Exception when the row is not reached
     */
    private static void select(PtySession session, String title) throws Exception {
        for (int step = 0; step < 12; step++) {
            if (selected(session).contains(title)) {
                return;
            }
            var before = selected(session);
            type(session, "j");
            await(() -> !selected(session).equals(before));
        }
        fail("the row " + title + " was not reached: " + session.screenText());
    }

    private static String selected(PtySession session) {
        return session.screen().lines().stream()
                            .filter(line -> line.startsWith("\u25b6"))
                            .findFirst()
                            .orElse("");
    }

    private static void await(BooleanSupplier condition) {
        var deadline = System.nanoTime() + DialogueHarness.TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            LockSupport.parkNanos(POLL.toNanos());
        }
        assertTrue(condition.getAsBoolean(), "the condition did not settle");
    }
}
