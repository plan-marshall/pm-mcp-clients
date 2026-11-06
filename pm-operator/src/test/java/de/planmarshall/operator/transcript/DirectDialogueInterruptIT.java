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
 * {@code Ctrl+C} typed at the decision prompt of {@code answer} on a real pseudo-terminal
 * ({@code CLI-CC-S-01}): the handler leaves with the exit-code table's {@code 130} and sends
 * nothing. The prompt exists only in a process with a console, so the case runs here rather than
 * in the in-process handler tests.
 *
 * <p>The key is typed once the prompt is on screen. PLAN-94 D5(g) asked for a reader-ready
 * condition or a reason it cannot race: where JLine falls back to the {@code exec} provider, the
 * prompt a dialogue draws is followed by JLine's {@code stty} child that switches the terminal to raw
 * mode, so Ctrl+C typed in that window can kill the child
 * ({@code java.io.IOError: Error executing '/bin/stty ...'}) rather than reaching the reader.
 * No output marks the end of that switch, and a probe key would change the dialogue's own state; the
 * drawn prompt is therefore the last condition the harness can read. Recorded as a decision.
 */
@DisplayName("Ctrl+C at the answer dialogue leaves with 130 and sends nothing")
@EnabledOnOs({OS.MAC, OS.LINUX})
class DirectDialogueInterruptIT {

    private static final String INTERRUPT = "\u0003";
    private static final String PROMPT = "w) in my own words";

    @Test
    @DisplayName("answer Q-007: the decision prompt interrupted leaves with 130")
    void answerPromptInterrupt() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007", "--interactive")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, INTERRUPT);
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(130, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }
}
