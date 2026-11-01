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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The runtime stop dialogue on a real pseudo-terminal (CLI-CC-S-01, CLI-UC-S-02): {@code q} sends
 * nothing, an unknown input re-asks, {@code k} keeps the runtime running, and {@code 1} — or Enter,
 * the preselection — stops it when what runs is done.
 */
@DisplayName("pm-operator runtime stop dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class RuntimeStopDialogueIT {

    private static final String PROMPT = "1) Stop when done";
    private static final String STOP_NOW = "2) Stop now";
    private static final String KEEP = "k) keep it running";

    @Test
    @DisplayName("q first: the dialogue leaves and sends nothing")
    void qFirstSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("Nothing was sent."), session.screenText());
            assertEquals(3, session.exitCode(), session.rawOutput());
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

            assertEquals(2, DialogueHarness.times(session.rawOutput(), PROMPT), session.screenText());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("k keeps it running and sends nothing")
    void keepRunningSendsNothing() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
            assertTrue(session.awaitText(KEEP, DialogueHarness.TIMEOUT), session.rawOutput());
            var shown = session.rawOutput().length();

            DialogueHarness.type(session, "k\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            assertFalse(session.rawOutput().substring(shown).contains("Nothing was sent."), session.rawOutput());
            harness.assertNoAct();
        }
    }

    @Test
    @DisplayName("1 stops when what runs is done")
    void stopsWhenDone() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
            assertTrue(session.awaitText(STOP_NOW, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "1\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("runtime   draining"), session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertAct("POST", "/api/v1/runtime/stop");
        }
    }

    @Test
    @DisplayName("Enter takes the preselection: the stop when done")
    void enterTakesThePreselection() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.REPORTS_AND_SYSTEM).goTo("system.running");
             var session = harness.open("runtime", "stop")) {
            assertTrue(session.awaitText(PROMPT, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertTrue(session.screenText().contains("runtime   draining"), session.screenText());
            assertEquals(0, session.exitCode(), session.rawOutput());
            harness.assertAct("POST", "/api/v1/runtime/stop");
        }
    }
}
