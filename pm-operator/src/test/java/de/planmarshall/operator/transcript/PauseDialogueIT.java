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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The pause dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing, an unknown
 * input re-asks, and Enter takes the preselection and pauses the plan.
 */
@DisplayName("pm-operator pause dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PauseDialogueIT {

    private static final String PROMPT = "Where does fix-relay hold?";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "pause", "fix-relay")) {
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
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "pause", "fix-relay")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "9\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(2, DialogueHarness.times(session.rawOutput(), PROMPT), session.screenText());
            assertTrue(session.screenText().contains("1) now"), session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: Enter takes the preselection and the pause act arrives")
    void mainPathPauses() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "pause", "fix-relay")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("fix-relay pausing after the task"),
                                session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertAct("POST", "/api/v1/plans/fix-relay/pause");
        }
    }
}
