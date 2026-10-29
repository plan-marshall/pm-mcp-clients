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
 * The machine onboarding dialogue on a real pseudo-terminal (CLI-CC-S-01, CLI-UC-S-06): {@code q}
 * sends nothing, an unknown input re-asks, the guided path steps through every state to
 * {@code onboard.completed}, and {@code --auto --yes} prints the automatic run to its result table.
 */
@DisplayName("pm-operator onboarding dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OnboardingDialogueIT {

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

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("Begin with step 1", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Nothing was sent."), session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("an unknown input re-asks and sends nothing")
    void unknownInputReAsks() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("Begin with step 1", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            DialogueHarness.type(session, "zzz\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.rawOutput(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the guided path: every state is rendered and the run completes")
    void mainPathCompletes() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("Begin with step 1", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            for (var title : GUIDED_PATH) {
                DialogueHarness.type(session, "\n");
                assertTrue(session.awaitText(title, DialogueHarness.TIMEOUT),
                                    title + " never reached:\n" + session.rawOutput());
            }

            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            harness.assertAct("POST", "/api/v1/onboarding/step");
        }
    }

    @Test
    @DisplayName("the automatic path: --auto --yes prints one line per state to the result table")
    void automaticPathPrintsTheResult() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard", "--auto", "--yes")) {
            assertTrue(session.awaitText("Everything is decided.", DialogueHarness.TIMEOUT),
                                session.rawOutput());
            assertTrue(session.awaitText("Deutsch, models too", DialogueHarness.TIMEOUT),
                                session.rawOutput());
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            harness.assertAct("POST", "/api/v1/onboarding/auto");
        }
    }
}
