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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.operator.PmOperator;

import io.nayuki.qrcodegen.QrCode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * End-to-end PTY integration tests for the terminal chat dialogue ({@code PLAN-138} D5) against the
 * mock runtime: unseeded chat opening, streamed reply and stop, leaving and returning to an open
 * proposal, {@code new}, proposal accept and reject, an overtaken proposal, and ended-work
 * follow-up opening.
 */
@EnableGeneratorController
@DisplayName("pm-operator chat dialogue on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ChatDialogueIT {

    private static final String PLAN_SEEDED = "fix-relay";
    private static final String PLAN_UNSEEDED = "relay-auth-v2";
    private static final String PLAN_ENDED = "docs-typo";
    private static final String SCRIPT = "/usr/bin/script";

    @Test
    @DisplayName("opens unseeded plan chat, streams reply, confirms new, and leaves on q")
    void planChatStreamAndNew() throws Exception {
        var question = "Question-" + Generators.letterStrings(5, 8).next();
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plan", "chat", PLAN_UNSEEDED)) {
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT),
                                session.rawOutput());

            DialogueHarness.type(session, question + "\nsecond line\n\n");
            assertTrue(session.awaitText("replan answers", DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
            var chatBeforeNew = harness.send(HttpRequest.get("/api/v1/plans/" + PLAN_UNSEEDED + "/chat"));

            DialogueHarness.type(session, "new\n");
            assertTrue(session.awaitText("Start a new chat?", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "n\n");
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "new\n");
            assertTrue(session.awaitText("Start a new chat?", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "y\n");
            assertTrue(session.awaitText("The earlier chat is kept with " + PLAN_UNSEEDED + ".",
                                DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertAll(
                                () -> assertEquals(0, session.exitCode(), session.rawOutput()),
                                () -> assertTrue(session.rawOutput().contains(question)
                                                    && session.rawOutput().contains("second line"),
                                                    session.rawOutput()),
                                () -> assertTrue(chatBeforeNew.bodyText().contains(question + "\\nsecond line"),
                                                    chatBeforeNew.bodyText()),
                                () -> assertTrue(session.screenText().contains(
                                                    "Left the chat; it stays with " + PLAN_UNSEEDED + "."),
                                                    session.screenText()),
                                () -> harness.assertAct("POST", "/api/v1/plans/" + PLAN_UNSEEDED + "/chat"),
                                () -> harness.assertAct("POST", "/api/v1/plans/" + PLAN_UNSEEDED + "/chat/turns"),
                                () -> harness.assertAct("POST", "/api/v1/plans/" + PLAN_UNSEEDED + "/chat/end"));
        }
    }

    @Test
    @DisplayName("s during streamed reply stops the reply and keeps the text so far")
    void streamedReplyStopKeepsPartialText() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not available");
        var question = "Slow-" + Generators.letterStrings(5, 8).next();
        var binary = BinaryUnderTest.locate("pm-operator", PmOperator.class, MachinePaths.class,
                            CommandLine.class, JsonFactory.class, Terminal.class, LineReader.class, QrCode.class);
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                            .scenario(StandardScenarios.RUNNING_WORK).recordActBodies(true)
                            .timing(MockTiming.REALISTIC).build())) {
            MockMachine.recordLiveRuntime(runtime);
            var env = new HashMap<>(runtime.environment());
            env.put("USER", "operator");
            env.put("HOSTNAME", "dialogue-host");
            env.put("LANG", "en_US.UTF-8");
            env.put("TERM", "xterm-256color");
            env.put("PATH", "/usr/bin:/bin");
            env.put("XDG_STATE_HOME", runtime.base().resolve("xdg-state").toString());
            try (var session = PtySession.start(binary.with("plan", "chat", PLAN_UNSEEDED), env,
                                Path.of("").toAbsolutePath(), 120, 30)) {
                assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT),
                                    session.rawOutput());

                session.send(question + "\n\n");
                assertTrue(session.awaitText("s) stop the reply", DialogueHarness.TIMEOUT), session.rawOutput());
                session.send("s\n");
                assertTrue(session.awaitText("Stopped; the reply so far is kept.", DialogueHarness.TIMEOUT),
                                    session.rawOutput());
                assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
                session.send("q\n");
                assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

                var actsResp = runtime.client().send(HttpRequest.get("/mock/acts"));
                var acts = DialogueHarness.actsOf(actsResp.bodyText());
                assertAll(
                                    () -> assertEquals(0, session.exitCode(), session.rawOutput()),
                                    () -> assertTrue(session.screenText().contains("Stopped; the reply so far is kept."),
                                                        session.screenText()),
                                    () -> assertTrue(acts.stream().anyMatch(
                                                        act -> "POST".equals(act.method()) && act.path().endsWith("/stop")),
                                                        acts.toString()));
            }
        }
    }

    @Test
    @DisplayName("leaving an open proposal does not end chat; returning shows proposal again and r rejects")
    void leaveAndReturnAndRejectProposal() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK)) {
            try (var first = harness.open("plan", "chat", PLAN_SEEDED)) {
                assertTrue(first.awaitText("Accept?", DialogueHarness.TIMEOUT), first.rawOutput());
                DialogueHarness.type(first, "q\n");
                assertTrue(first.awaitExit(DialogueHarness.TIMEOUT), first.rawOutput());
                assertAll(
                                    () -> assertEquals(0, first.exitCode(), first.rawOutput()),
                                    () -> assertTrue(first.screenText().contains(
                                                        "Left the chat; it stays with " + PLAN_SEEDED + "."),
                                                        first.screenText()),
                                    () -> assertFalse(harness.acts().stream().anyMatch(
                                                        act -> act.path().endsWith("/chat/end")),
                                                        harness.acts().toString()));
            }
            try (var second = harness.open("plan", "chat", PLAN_SEEDED)) {
                assertTrue(second.awaitText("Accept?", DialogueHarness.TIMEOUT), second.rawOutput());
                DialogueHarness.type(second, "r\n");
                assertTrue(second.awaitText("you  ›", DialogueHarness.TIMEOUT), second.rawOutput());
                DialogueHarness.type(second, "q\n");
                assertTrue(second.awaitExit(DialogueHarness.TIMEOUT), second.rawOutput());
                assertAll(
                                    () -> assertEquals(0, second.exitCode(), second.rawOutput()),
                                    () -> harness.assertAct("POST",
                                                        "/api/v1/plans/" + PLAN_SEEDED + "/chat/proposal/reject"));
            }
        }
    }

    @Test
    @DisplayName("Enter accepts open revision proposal and prints accepted boundary")
    void acceptOpenProposal() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plan", "chat", PLAN_SEEDED)) {
            assertTrue(session.awaitText("Accept?", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertAll(
                                () -> assertEquals(0, session.exitCode(), session.rawOutput()),
                                () -> assertTrue(session.screenText().contains("accepted: applies after the current task"),
                                                    session.screenText()),
                                () -> harness.assertAct("POST",
                                                    "/api/v1/plans/" + PLAN_SEEDED + "/chat/proposal/accept"));
        }
    }

    @Test
    @DisplayName("overtaken proposal prints changed_since with version and offers r) reject only")
    void overtakenProposalChangedSince() throws Exception {
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plan", "chat", PLAN_SEEDED)) {
            assertTrue(session.awaitText("Accept?", DialogueHarness.TIMEOUT), session.rawOutput());
            var broadcast = harness.send(HttpRequest.json("POST", "/mock/broadcast",
                                "{\"event\":\"instance.changed\",\"data\":{\"plan_id\":\"" + PLAN_SEEDED
                                                    + "\",\"instance_version\":9,\"cause\":\"replan\"}}"));
            assertEquals(200, broadcast.status(), broadcast.bodyText());

            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitText("not applied: " + PLAN_SEEDED + " changed since (version 9)",
                                DialogueHarness.TIMEOUT), session.rawOutput());
            assertTrue(session.awaitText("r) reject", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "r\n");
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertAll(
                                () -> assertEquals(0, session.exitCode(), session.rawOutput()),
                                () -> harness.assertAct("POST",
                                                    "/api/v1/plans/" + PLAN_SEEDED + "/chat/proposal/reject"));
        }
    }

    @Test
    @DisplayName("ended-work follow-up accept prints control.chat.follow_up.opened and leaves without ending chat")
    void endedWorkFollowUpOpened() throws Exception {
        var prompt = "FollowUp-" + Generators.letterStrings(4, 7).next();
        try (var harness = DialogueHarness.start(StandardScenarios.RUNNING_WORK);
             var session = harness.open("plan", "chat", PLAN_ENDED)) {
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT),
                                session.rawOutput());
            DialogueHarness.type(session, prompt + "\n\n");
            assertTrue(session.awaitText("Start a plan with it?", DialogueHarness.TIMEOUT), session.rawOutput());
            DialogueHarness.type(session, "\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());

            assertAll(
                                () -> assertEquals(0, session.exitCode(), session.rawOutput()),
                                () -> assertTrue(session.screenText().contains(
                                                    "A new plan is being started in pm-mcp-clients with this description; go on with its sources."),
                                                    session.screenText()),
                                () -> assertTrue(session.screenText().contains(
                                                    "Left the chat; it stays with " + PLAN_ENDED + "."),
                                                    session.screenText()),
                                () -> harness.assertAct("POST",
                                                    "/api/v1/plans/" + PLAN_ENDED + "/chat/proposal/accept"),
                                () -> assertFalse(harness.acts().stream().anyMatch(
                                                    act -> act.path().endsWith("/chat/end")),
                                                    harness.acts().toString()));
        }
    }
}
