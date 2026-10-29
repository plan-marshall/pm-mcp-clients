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
 * The cancel dialogue on a real pseudo-terminal (CLI-CC-S-01): {@code q} sends nothing, an unknown
 * input re-asks, and the main path sends the cancel with its reason.
 */
@DisplayName("pm-operator cancel dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class CancelDialogueIT {

    private static final String PROMPT = "Reason";
    private static final String CONSENT = "y) yes";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "cancel", "fix-relay")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Nothing was sent."), session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("9 at the consent re-asks with the offered keys and sends nothing")
    void unknownInputReAsks() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "cancel", "fix-relay")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "because\n9\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("9 is not offered here: y, n, q"),
                                session.screenText());
            assertEquals(2, DialogueHarness.times(session.rawOutput(), CONSENT), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("Enter at the consent has no default: it asks for a key and sends nothing")
    void enterAtConsentReAsks() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "cancel", "fix-relay")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "because\n\nq\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Type y or n."), session.screenText());
            assertTrue(session.screenText().contains("Nothing was sent."), session.screenText());
            assertEquals(2, DialogueHarness.times(session.rawOutput(), CONSENT), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("the main path: the reason and the confirmation send the cancel")
    void mainPathCancels() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.FULLY_ONBOARDED);
             var session = harness.open("plan", "cancel", "fix-relay")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "because\ny\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("fix-relay cancelled and archived"),
                                session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertAct("POST", "/api/v1/plans/fix-relay/cancel");
        }
    }
}
