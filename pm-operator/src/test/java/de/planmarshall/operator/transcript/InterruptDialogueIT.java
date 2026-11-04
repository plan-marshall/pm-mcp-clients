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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * {@code Ctrl+C} at a prompt of every verb-reached dialogue ({@code CLI-CC-S-01}): the process
 * leaves with the interrupt status of the exit-code table, {@code 130}, and sends nothing to the
 * runtime. Each case interrupts at the prompt of the handler it names: the pause point, the end
 * reason, the end consent, the review decision, the onboarding state, and the project setup state.
 *
 * <p>Every case waits until the prompt has settled before it types, because {@code Ctrl+C} typed
 * while JLine still reads the terminal in its cooked mode reaches the terminal driver instead of the
 * reader.
 */
@EnableGeneratorController
@DisplayName("Ctrl+C at a verb's dialogue leaves with 130 and sends nothing")
@EnabledOnOs({OS.MAC, OS.LINUX})
class InterruptDialogueIT {

    private static final String INTERRUPT = "\u0003";
    private static final int EXIT_INTERRUPTED = 130;

    private final String reason = Generators.letterStrings(8, 24).next();

    @Test
    @DisplayName("plan pause: the point prompt interrupted leaves with 130")
    void pauseInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "pause", "fix-relay")) {
            awaitPrompt(session, "Where does fix-relay hold?");

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("plan cancel: the reason prompt interrupted leaves with 130")
    void cancelReasonInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "cancel", "fix-relay")) {
            awaitPrompt(session, "Reason");

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("plan cancel: the consent prompt interrupted leaves with 130 after the reason")
    void cancelConsentInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "cancel", "fix-relay")) {
            awaitPrompt(session, "Reason");

            DialogueHarness.type(session, reason + "\n");
            awaitPrompt(session, "y) yes");

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("epic findings: the review prompt interrupted leaves with 130")
    void reviewInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("epic", "findings", "relay-hardening")) {
            awaitPrompt(session, "Reconnect loses the last event");

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("onboard: the first state prompt interrupted leaves with 130")
    void onboardInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            awaitPrompt(session, "Set up this machine for plan-marshall");

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("project setup: the identity prompt interrupted leaves with 130")
    void projectSetupInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
            awaitPrompt(session, "How should the project be named?");

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(EXIT_INTERRUPTED, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    /**
     * Waits until the dialogue has drawn the marker and its prompt's terminator, which is the
     * moment the line reader holds the terminal and a {@code Ctrl+C} reaches the reader rather than
     * the terminal driver. The terminator is the condition {@code StartPrompt} produces; no pause
     * is timed.
     *
     * @param session the running session
     * @param marker  the text the dialogue draws at its prompt
     * @throws InterruptedException if interrupted while waiting
     */
    private static void awaitPrompt(PtySession session, String marker) throws InterruptedException {
        DialogueHarness.awaitPrompt(session, marker);
    }
}
