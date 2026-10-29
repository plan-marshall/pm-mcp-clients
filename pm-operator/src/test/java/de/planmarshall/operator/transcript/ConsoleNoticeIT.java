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

import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.PmOperator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * The notices the console prints for the events the runtime pushes, and the prompt badges that
 * follow them (04-console.adoc#pushed, #answered-elsewhere, #prompt; CLI-UC-P-04): the operator runs
 * on a real pseudo-terminal against the mock runtime, and every event is broadcast through
 * {@code POST /mock/broadcast}, exactly as the running runtime pushes it.
 */
@DisplayName("pm-operator console event notices on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ConsoleNoticeIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String SCRIPT = "/usr/bin/script";

    /** The badge of the showroom: three decisions, one integrity pause, one held epic wait. */
    private static final String BADGE = "pm [3 questions · 1 integrity pause · 1 held] \u203a";

    /** The same badge after the web app answered one question. */
    private static final String BADGE_AFTER_ONE_ANSWER = "pm [2 questions · 1 integrity pause · 1 held] \u203a";

    private static final String DECISION_OPENED = """
                        {"event":"decision.opened","data":{"decision":{"id":"Q-007","subject":"plan fix-relay",\
                        "blocks":"plan","description":"The change conflicts with main in RelayServer.java.",\
                        "parts":[{"part_id":"1","question":"How should the conflict be resolved?",\
                        "options":[{"key":"1","label":"Merge main into the branch"},\
                        {"key":"2","label":"Rebase onto main"}],"recommendation":"2"}]}}}""";

    private static final String DECISION_ANSWERED = """
                        {"event":"decision.answered","data":{"id":"Q-007","answer":"Rebase onto main",\
                        "channel":"web","device":"iPad"}}""";

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @Test
    @DisplayName("a decision.opened broadcast prints the question notice above the prompt")
    void decisionOpenedNotice() throws Exception {
        try (var runtime = start();
             var session = open(runtime)) {
            assertTrue(session.awaitText(BADGE, TIMEOUT), session.rawOutput());

            broadcast(runtime, DECISION_OPENED);

            assertTrue(session.awaitText("◆ Question Q-007 · plan fix-relay · blocks the plan", TIMEOUT),
                                session.rawOutput());
            assertTrue(session.awaitText("Recommended: 2) Rebase onto main", TIMEOUT), session.rawOutput());
            assertTrue(aboveThePrompt(session.screenText(), "◆ Question Q-007"), session.screenText());
            assertEquals(1, DialogueHarness.times(session.rawOutput(), "◆ Question Q-007"),
                                "the notice is printed once, not once per redraw");
            leave(session);
        }
    }

    @Test
    @DisplayName("a decision.answered broadcast from the web app prints the notice and drops the badge")
    void answeredElsewhereNotice() throws Exception {
        try (var runtime = start();
             var session = open(runtime)) {
            assertTrue(session.awaitText(BADGE, TIMEOUT), session.rawOutput());

            broadcast(runtime, DECISION_ANSWERED);

            assertTrue(session.awaitText(
                                                    "– Q-007 was answered in the web app (iPad): Rebase onto main. Nothing was sent from here.",
                                                    TIMEOUT),
                                session.rawOutput());
            assertTrue(aboveThePrompt(session.screenText(), "– Q-007 was answered"), session.screenText());
            assertEquals(1, DialogueHarness.times(session.rawOutput(), "– Q-007 was answered"),
                                "the notice is printed once, not once per redraw");
            // The console rebuilds the badge on the event; JLine draws the prompt of a line already
            // being read with the badges it had, so an empty Enter is what shows the new ones.
            session.send("\n");
            assertTrue(session.awaitText(BADGE_AFTER_ONE_ANSWER, TIMEOUT), session.rawOutput());
            leave(session);
        }
    }

    /**
     * @param screen the screen's lines
     * @param notice the notice to place
     * @return {@code true} when the notice stands above the prompt line
     */
    private static boolean aboveThePrompt(String screen, String notice) {
        var lines = List.of(screen.split("\n"));
        var noticeAt = -1;
        var promptAt = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (noticeAt < 0 && lines.get(i).contains(notice)) {
                noticeAt = i;
            }
            if (lines.get(i).contains(" \u203a")) {
                promptAt = i;
            }
        }
        return noticeAt >= 0 && promptAt >= 0 && noticeAt < promptAt;
    }

    private void leave(PtySession session) throws IOException, InterruptedException {
        session.send("exit\n");
        assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
        assertEquals(0, session.exitCode(), session.rawOutput());
    }

    private static void broadcast(MockRuntime runtime, String body) throws IOException {
        var response = runtime.client().send(HttpRequest.json("POST", "/mock/broadcast", body));
        assertEquals(200, response.status(), "POST /mock/broadcast refused: " + body);
    }

    private static MockRuntime start() throws IOException {
        Assumptions.assumeTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not available");
        var runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(StandardScenarios.FULLY_ONBOARDED).build());
        MockMachine.recordLiveRuntime(runtime);
        return runtime;
    }

    private PtySession open(MockRuntime runtime) throws IOException {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "notice-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("XDG_STATE_HOME", runtime.base().resolve("xdg-state").toString());
        return PtySession.start(binary.with(), environment, Path.of("").toAbsolutePath(), 120, 30);
    }
}
