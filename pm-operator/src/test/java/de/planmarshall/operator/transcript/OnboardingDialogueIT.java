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

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The machine onboarding dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing,
 * an unknown input re-asks, and the guided path steps to {@code onboard.completed}.
 */
@DisplayName("pm-operator onboarding dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Disabled("PLAN-22-cli-state-dialogue-onboarding makes the onboarding one state dialogue")
class OnboardingDialogueIT {

    private static final String PROMPT = "Machine onboarding";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

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

            DialogueHarness.type(session, "zzz\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.screenText(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: the guided run steps to the completed state")
    void mainPathCompletes() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.ONBOARDING);
             var session = harness.open("onboard")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "h\n\n\n\n\n\n\n\n");
            assertTrue(session.awaitText("The machine is ready.", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            harness.assertAct("POST", "/api/v1/onboarding/step");
        }
    }
}
