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
 * A plan chat turn end to end against the mock runtime (PLAN-161, D3): {@code plan chat <id>
 * --message} posts a B1 {@code …/chat/turns} body carrying the client's own {@code client_turn_id} —
 * the runtime refuses a body without it, so an old-shape {@code {"message":…}} body draws no reply —
 * and renders the transcript it read while the role's reply was being written.
 */
@DisplayName("pm-operator plan chat turn against the mock runtime")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PlanChatIT {

    /** The role's second reply, which the sent message draws. */
    private static final String REPLY = "Here is what I would change.";
    /** The message this case sends. */
    private static final String MESSAGE = "Can the task list be checked?";

    @Test
    @DisplayName("a message is sent as a B1 turn and the role's reply is rendered")
    void planChatTurn() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.CONTROL_CHAT);
             var session = harness.open("plan", "chat", "fix-relay", "--message", MESSAGE)) {
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertTrue(session.screenText().contains(REPLY), session.screenText());

            var turns = harness.acts().stream()
                                .filter(act -> "POST".equals(act.method()) && act.path().endsWith("/chat/turns"))
                                .toList();
            assertEquals(1, turns.size(), "expected one chat turn, got " + harness.acts());
        }
    }
}
