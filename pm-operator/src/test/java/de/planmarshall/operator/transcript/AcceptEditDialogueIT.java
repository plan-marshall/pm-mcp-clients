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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The resolution of a tampered enrolment file on a real pseudo-terminal against the mock runtime
 * (02-operator-channel.adoc#operator-accept-edit): the project is refused {@code enrolment_tampered}
 * with the command that resolves it, the command shows the file and accepts it after a typed
 * confirmation, and the project is served again.
 */
@DisplayName("pm-operator accept-edit enrolment on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class AcceptEditDialogueIT {

    private static final String PROJECT = "pm-mcp-clients";
    private static final String PROJECT_PATH = "/api/v1/projects/" + PROJECT;
    private static final String CONFIRM = "Confirm edit acceptance? [y/N]";

    @Test
    @DisplayName("from the enrolment_tampered refusal through the named command to the project served again")
    void resolvesATamperedEnrolment() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START).goTo("enrol.enrolment-tampered")) {
            var refusal = refusal(harness);
            assertEquals("pm-operator accept-edit enrolment " + PROJECT, refusal.get("command"), refusal.toString());

            var command = String.valueOf(refusal.get("command")).split(" ");
            try (var session = harness.open(Arrays.copyOfRange(command, 1, command.length))) {
                assertTrue(session.awaitText(CONFIRM, DialogueHarness.TIMEOUT), session.rawOutput());
                assertTrue(session.screenText().contains("enrolments/" + PROJECT + ".json"), session.screenText());

                DialogueHarness.type(session, "y\n");
                assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

                assertTrue(session.screenText().contains("Edit accepted"), session.screenText());
                assertEquals(0, session.exitCode(), session.rawOutput());
            }
            harness.assertAct("POST", PROJECT_PATH + "/enrolment/accept-edit");

            var served = harness.send(HttpRequest.get(PROJECT_PATH));
            assertEquals(200, served.status(), served.bodyText());
        }
    }

    @Test
    @DisplayName("a declined confirmation sends nothing and the project stays refused")
    void declinedKeepsTheRefusal() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.PROJECTS_AND_START).goTo("enrol.enrolment-tampered");
             var session = harness.open("accept-edit", "enrolment", PROJECT)) {
            assertTrue(session.awaitText(CONFIRM, DialogueHarness.TIMEOUT), session.rawOutput());

            DialogueHarness.type(session, "n\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertEquals(1, session.exitCode(), session.rawOutput());
            assertTrue(session.screenText().contains("nothing was sent"), session.screenText());
            harness.assertNoAct();
            assertEquals("enrolment_tampered", refusal(harness).get("error"));
        }
    }

    private static Map<String, Object> refusal(DialogueHarness harness) throws IOException {
        var response = harness.send(HttpRequest.get(PROJECT_PATH));
        assertEquals(409, response.status(), response.bodyText());
        var body = JsonTree.asObject(JsonTree.parse(response.bodyText()));
        assertNotNull(body, response.bodyText());
        return body;
    }
}
