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
 * The runtime stop dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing, an
 * unknown input re-asks, and {@code 1} sends the stop. The {@code k} case (keep it running) belongs
 * to the confirmation the fixing plan adds, together with the wording of its question.
 */
@DisplayName("pm-operator runtime stop dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Disabled("PLAN-17-cli-runtime-act-confirm confirms the runtime acts")
class RuntimeStopDialogueIT {

    private static final String PROMPT = "1) stop it now";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
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
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "9\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(2, DialogueHarness.times(session.screenText(), PROMPT), session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: 1 sends the stop")
    void mainPathStops() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "1\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("runtime   stopping"), session.screenText());
            harness.assertAct("POST", "/api/v1/runtime/stop");
        }
    }
}
