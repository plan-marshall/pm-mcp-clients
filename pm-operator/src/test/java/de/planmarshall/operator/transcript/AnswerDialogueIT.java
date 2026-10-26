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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.mock.StandardScenarios;

/**
 * The answer dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing, an unknown
 * input re-asks, and the main path sends the answer.
 */
@DisplayName("pm-operator answer dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Disabled("PLAN-21-cli-answer-dialogue fixes the answer dialogue and its q")
class AnswerDialogueIT {

    private static final String PROMPT = "w) in my own words";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
                            var session = harness.open("answer", "Q-007", "--interactive")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Nothing was sent."), session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("an unknown input re-asks and sends nothing")
    void unknownInputReAsks() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
                            var session = harness.open("answer", "Q-007", "--interactive")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "zzz\nl\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.screenText(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: the chosen option sends the answer")
    void mainPathAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
                            var session = harness.open("answer", "Q-007", "--interactive")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "1\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-007 answered"), session.screenText());
            harness.assertAct("POST", "/api/v1/decisions/Q-007/answer");
        }
    }
}
