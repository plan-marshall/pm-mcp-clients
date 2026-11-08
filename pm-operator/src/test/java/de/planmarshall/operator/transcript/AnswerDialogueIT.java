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

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The answer dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing, an unknown
 * input re-asks, and the main path sends the answer.
 */
@DisplayName("pm-operator answer dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class AnswerDialogueIT {

    private static final String PROMPT = "w) in my own words";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007")) {
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
             var session = harness.open("answer", "Q-007")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "zzz\nl\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(DialogueHarness.times(session.rawOutput(), PROMPT) >= 2, session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: the chosen option sends the answer")
    void mainPathAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "1\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-007 answered"), session.screenText());
            harness.assertAct("POST", "/api/v1/decisions/Q-007/answer");
        }
    }

    @Test
    @DisplayName("in own words: answering in operator's own words sends the interpreted answer")
    void ownWordsAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "w\nrebase onto main\n\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-007 answered"), session.screenText());
            harness.assertAct("POST", "/api/v1/decisions/Q-007/answer");
        }
    }

    @Test
    @DisplayName("waiver request: granting waiver with reason sends answer")
    void waiverAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK).goTo("decisions.open");
             var session = harness.open("answer", "WR-2")) {
            assertTrue(session.awaitText("Waive the gate", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "y\ncovered by CI\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Waived ci_check:integration-tests for fix-relay"), session.screenText());
            harness.assertAct("POST", "/api/v1/decisions/WR-2/answer");
        }
    }

    @Test
    @DisplayName("later leaves the decision open without sending an act")
    void laterLeavesOpen() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "l\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-007 stays open"), session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the mock's three-part Q-012 asks every part with its question")
    void threePartDecisionAsksEveryPart() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK).goTo("decisions.open");
             var session = harness.open("answer", "Q-012")) {
            assertTrue(session.awaitText("Where does the data live?", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "1\n");
            assertTrue(session.awaitText("How is it migrated?", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "2\n");
            assertTrue(session.awaitText("Which format?", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "1\n");
            assertTrue(session.awaitText("Your decision", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-012 answered"), session.screenText());
            harness.assertAct("POST", "/api/v1/decisions/Q-012/answer");
        }
    }

    @Test
    @DisplayName("direct answer sends without dialogue and prints confirmation")
    void directAnswerSendsWithoutDialogue() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007", "2")) {
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("✓ Q-007 answered: Rebase onto main"), session.screenText());
            harness.assertAct("POST", "/api/v1/decisions/Q-007/answer");
        }
    }
}
