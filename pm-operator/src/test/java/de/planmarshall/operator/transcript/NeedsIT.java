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
 * PTY tests for PLAN-36 D2: {@code !} on a waiting row opens its decision in {@code running-work}.
 */
@DisplayName("pm-operator ! on waiting row opens decision on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class NeedsIT {

    private static final Duration POLL = Duration.ofMillis(25);

    @Test
    @DisplayName("! on the waiting row of the overview opens its decision dialogue in running-work")
    void exclamationOnWaitingRowOpensDecision() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plans")) {
            awaitText(session, "Needs you");
            select(session, "Drop the legacy endpoints");

            // Type ! on the waiting row: it must open the decision dialogue directly
            DialogueHarness.type(session, "!");
            awaitText(session, "Delete the published v1 endpoints");

            // Leaving the dialogue with q and exiting the view
            DialogueHarness.type(session, "q\n");
            DialogueHarness.type(session, "q");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
        }
    }

    @Test
    @DisplayName("! on a non-waiting row jumps to the first waiting row of the overview")
    void exclamationOnNonWaitingRowJumpsToWaitingRow() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plans")) {
            awaitText(session, "Needs you");
            // pm-mcp-core runs work and waits for nobody: ! must jump to the next waiting row.
            select(session, "pm-mcp-core");

            DialogueHarness.type(session, "!");
            await(() -> selected(session).contains("Relay Hardening"));
            assertTrue(selected(session).startsWith("\u25b6"), session.screenText());

            DialogueHarness.type(session, "q");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
        }
    }

    private static void select(PtySession session, String title) throws Exception {
        for (int step = 0; step < 12; step++) {
            if (selected(session).contains(title)) {
                return;
            }
            var before = selected(session);
            DialogueHarness.type(session, "j");
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

    private static void awaitText(PtySession session, String text) throws InterruptedException {
        assertTrue(session.awaitText(text, DialogueHarness.TIMEOUT),
                            () -> "text '" + text + "' did not appear in:\n" + session.screenText());
    }

    private static void await(BooleanSupplier condition) {
        var deadline = System.nanoTime() + DialogueHarness.TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            LockSupport.parkNanos(POLL.toNanos());
        }
        fail("condition was not met within " + DialogueHarness.TIMEOUT);
    }
}
