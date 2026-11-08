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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.planmarshall.api.json.JsonTree;
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
    @DisplayName("the main path: the screen shows the votes and dissent, and the chosen option is posted")
    void mainPathAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "?\n1\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            var screen = session.screenText();
            assertTrue(screen.contains("Evidence details:"), screen);
            assertTrue(screen.contains("2 of 3 votes"), screen);
            assertTrue(screen.contains("Against it (quorum-b)"), screen);
            assertTrue(screen.contains("Q-007 answered"), screen);
            var sent = JsonTree.asObject(JsonTree.parse(lastBody(harness, "/api/v1/decisions/Q-007/answer")));
            assertNotNull(sent);
            assertEquals("1", sent.get("option"), String.valueOf(sent));
        }
    }

    @Test
    @DisplayName("in own words: the interpreted words are posted as the answer")
    void ownWordsAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "w\nrebase onto main\n\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-007 answered"), session.screenText());
            var sent = JsonTree.asObject(JsonTree.parse(lastBody(harness, "/api/v1/decisions/Q-007/answer")));
            assertNotNull(sent);
            assertEquals("Rebase onto main", sent.get("words"), String.valueOf(sent));
        }
    }

    @Test
    @DisplayName("waiver request: the waiver and its reason are posted")
    void waiverAnswers() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK).goTo("decisions.open");
             var session = harness.open("answer", "WR-2")) {
            assertTrue(session.awaitText("Waive the gate", DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "y\ncovered by CI\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Waived ci_check:integration-tests for fix-relay"), session.screenText());
            var sent = JsonTree.asObject(JsonTree.parse(lastBody(harness, "/api/v1/decisions/WR-2/answer")));
            assertNotNull(sent);
            assertEquals("waive", sent.get("action"), String.valueOf(sent));
            assertEquals("covered by CI", sent.get("reason"), String.valueOf(sent));
        }
    }

    @Test
    @DisplayName("a direct later posts the later act instead of printing success blind")
    void directLaterPostsTheAct() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007", "later")) {
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Q-007 stays open"), session.screenText());
            var sent = JsonTree.asObject(JsonTree.parse(lastBody(harness, "/api/v1/decisions/Q-007/answer")));
            assertNotNull(sent);
            assertEquals("later", sent.get("action"), String.valueOf(sent));
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
            var sent = JsonTree.asObject(JsonTree.parse(lastBody(harness, "/api/v1/decisions/Q-012/answer")));
            assertNotNull(sent);
            var answers = JsonTree.asObject(sent.get("part_answers"));
            assertNotNull(answers, String.valueOf(sent));
            assertEquals(3, answers.size(), String.valueOf(sent));
        }
    }

    @Test
    @DisplayName("direct answer sends without dialogue, prints the confirmation and posts the option")
    void directAnswerSendsWithoutDialogue() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("answer", "Q-007", "2")) {
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("✓ Q-007 answered: Rebase onto main"), session.screenText());
            var sent = JsonTree.asObject(JsonTree.parse(lastBody(harness, "/api/v1/decisions/Q-007/answer")));
            assertNotNull(sent);
            assertEquals("2", sent.get("option"), String.valueOf(sent));
        }
    }

    private static String lastBody(DialogueHarness harness, String path) throws Exception {
        var act = harness.acts().stream()
                            .filter(recorded -> path.equals(recorded.path()))
                            .reduce((first, second) -> second)
                            .orElseThrow(() -> new AssertionError("no act reached " + path));
        return act.body();
    }
}
