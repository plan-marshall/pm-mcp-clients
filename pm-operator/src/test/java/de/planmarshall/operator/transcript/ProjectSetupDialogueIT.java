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
 * The project setup dialogue on a real pseudo-terminal (CLI-CC-S-01, CLI-UC-P-01): {@code q} sends
 * nothing, an unknown input re-asks, and the identity state shows the project's id, name and pilot
 * choice before the setup act is sent.
 */
@DisplayName("pm-operator project setup dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ProjectSetupDialogueIT {

    private static final String PROMPT = "How should the project be named?";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("Record this choice", DialogueHarness.TIMEOUT),
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
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("Record this choice", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            DialogueHarness.type(session, "zzz\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.rawOutput(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the identity state shows id, name and the pilot choice")
    void identityShowsIdNameAndPilot() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("Record this choice", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            var screen = session.screenText();
            assertTrue(screen.contains("Id"), screen);
            assertTrue(screen.contains("cui-parent"), screen);
            assertTrue(screen.contains("Name"), screen);
            assertTrue(screen.contains(
                                "Pilot: this version of PM-MCP runs plans only in pilot projects: no"), screen);

            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("The project is ready"), session.screenText());
            harness.assertAct("POST", "/api/v1/projects/cui-parent/setup");
        }
    }
}
