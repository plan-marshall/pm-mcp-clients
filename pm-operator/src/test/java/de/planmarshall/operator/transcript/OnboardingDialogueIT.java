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

import java.util.List;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The machine onboarding dialogue on a real pseudo-terminal (CLI-CC-S-01, CLI-UC-S-06), entered as
 * the design has it: the bare {@code pm-operator} on a machine that is not set up, answered with
 * {@code h} (here, in the terminal). {@code q} leaves the dialogue with nothing sent, an unknown
 * input re-asks, and the guided path steps through every state to {@code onboard.completed}; the
 * console stays open after each, and {@code exit} leaves it with {@code 0}.
 */
@DisplayName("pm-operator onboarding dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OnboardingDialogueIT {

    private static final String WHERE = "Where do you want to set it up?";

    private static final String PROMPT = "Set up this machine for plan-marshall";

    private static final List<String> GUIDED_PATH = List.of(
                        "Choose your languages",
                        "Look for harnesses on this machine?",
                        "Choose the harnesses to enrol",
                        "Check whether the 2 chosen harnesses are logged in?",
                        "Enrol 2 harnesses",
                        "Ask the harnesses for their models, and a model for advice?",
                        "Choose the models plans may use",
                        "Give every role a harness and a model",
                        "Where should this machine keep its tokens?",
                        "Check the macOS Keychain with a test entry?",
                        "Look for CLI tools and stored tokens?",
                        "Choose what this machine works with",
                        "SonarCloud needs a token",
                        "Check the providers with their tokens?",
                        "How should plans run on this machine?",
                        "Review the setup of this machine",
                        "This machine is ready");

    /**
     * Opens the bare {@code pm-operator} and answers the entry's choice with {@code h}, so the
     * onboarding dialogue runs here.
     *
     * @param session the running session
     * @throws Exception if the entry or the dialogue's first frame is never drawn
     */
    private static void enterHere(PtySession session) throws Exception {
        assertTrue(session.awaitText(WHERE, DialogueHarness.TIMEOUT), session.rawOutput());
        DialogueHarness.type(session, "h\n");
        assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
        assertTrue(session.awaitText("Begin with step 1", DialogueHarness.TIMEOUT), session.rawOutput());
    }

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open()) {
            enterHere(session);

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitText("Nothing was sent.", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "exit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("an unknown input re-asks and sends nothing")
    void unknownInputReAsks() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open()) {
            enterHere(session);

            DialogueHarness.type(session, "zzz\nq\n");
            assertTrue(session.awaitText("Nothing was sent.", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "exit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            // The re-ask is the dialogue's answer to the input it did not offer, drawn once. A
            // title redraw can repeat the frame title; only a re-ask says the input was rejected.
            assertEquals(1, DialogueHarness.times(session.rawOutput(), "zzz is not offered here"),
                                session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the guided path: every state is rendered and the run completes")
    void mainPathCompletes() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open()) {
            enterHere(session);

            for (var title : GUIDED_PATH) {
                DialogueHarness.type(session, "\n");
                assertTrue(session.awaitText(title, DialogueHarness.TIMEOUT),
                                    title + " never reached:\n" + session.rawOutput());
            }

            DialogueHarness.type(session, "exit\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            harness.assertAct("POST", "/api/v1/onboarding/step");
        }
    }

    @Test
    @DisplayName("onboard is no verb: the binary reports it as unknown with exit 2")
    void onboardIsUnknown() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(2, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }
}
