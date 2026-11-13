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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * PTY tests for PLAN-36 D2: {@code !} on a waiting row opens its decision in {@code running-work},
 * from a project's level and from the overview's own group of what waits.
 */
@DisplayName("pm-operator ! on waiting row opens decision on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class NeedsIT {

    private static final Duration POLL = Duration.ofMillis(25);

    @Test
    @DisplayName("! on the waiting row of a plan opens its decision dialogue in running-work")
    void exclamationOnWaitingRowOpensDecision() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plans", "--project", "pm-mcp-clients")) {
            awaitText(session, "by plan");
            selectByTitle(session, "Drop the legacy endpoints");

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
    @DisplayName("! on a non-waiting row jumps to the next waiting row in running-work")
    void exclamationOnNonWaitingRowJumpsToWaitingRow() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plans", "--project", "pm-mcp-clients")) {
            awaitText(session, "by plan");
            selectByTitle(session, "Fix Relay Transport");

            // Fix Relay Transport has no wait; pressing ! must jump to the next waiting row
            DialogueHarness.type(session, "!");
            await(() -> selected(session).contains("landing CI failure")
                                || selected(session).contains("Drop the legacy"));

            DialogueHarness.type(session, "q");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
        }
    }

    @Test
    @DisplayName("! on an item of the overview's Needs you group runs what its row prints")
    void exclamationOnTheOverviewOpensTheItem() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plans")) {
            // The overview itself, not a project's level: the group of what waits leads it.
            awaitText(session, "Needs you");
            // The group lists the plan three times (landing failed, questions, answer Q-009);
            // the row meant is the decision whose own command is "answer Q-009".
            selectByTitle(session, "Drop the legacy endpoints", "answer Q-009");

            // ! on that item runs the command its row prints: the decision opens.
            DialogueHarness.type(session, "!");
            awaitText(session, "Delete the published v1 endpoints");

            DialogueHarness.type(session, "q\n");
            DialogueHarness.type(session, "q");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
        }
    }

    /**
     * Moves the selection down to the first row whose line holds the title and every one of the
     * given markers.
     */
    private static void selectByTitle(PtySession session, String title, String... markers) throws Exception {
        var wanted = new ArrayList<String>();
        wanted.add(title);
        wanted.addAll(List.of(markers));
        for (int step = 0; step < 40; step++) {
            var line = selected(session);
            if (wanted.stream().allMatch(line::contains)) {
                return;
            }
            var before = line;
            DialogueHarness.type(session, "j");
            await(() -> !selected(session).equals(before));
        }
        fail("the row " + wanted + " was not reached: " + session.screenText());
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
