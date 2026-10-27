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
 * The project setup dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing, an
 * unknown input re-asks, and the main path sends the setup.
 */
@DisplayName("pm-operator project setup dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Disabled("PLAN-22-cli-state-dialogue-onboarding gives project setup one state dialogue")
class ProjectSetupDialogueIT {

    private static final String PROMPT = "cui-parent";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
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
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "zzz\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.rawOutput(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: the setup act arrives")
    void mainPathStartsSetup() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START)
                                 .goTo("projects.being-added");
             var session = harness.open("project", "setup", "cui-parent")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Project setup for cui-parent"), session.screenText());
            harness.assertAct("POST", "/api/v1/projects/cui-parent/setup");
        }
    }
}
