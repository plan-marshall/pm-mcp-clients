/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The console's own behaviour beyond the typed-verb transcripts: the bare-number hint, the hint on
 * an empty {@code Ctrl+C}, the questions that stay open on {@code Ctrl+D}, and the desktop
 * notification payload, which must not carry the text of the question.
 */
@DisplayName("PmConsole behaviour")
class PmConsoleBehaviourTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private OperatorContext context(ByteArrayOutputStream rendered, String typed) {
        var input = new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8));
        return new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(input, rendered), null);
    }

    private void onboardedWithQuestion() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0",
                                     "needs_operator":{"question":2},
                                     "web":{"enabled":true,"port":7420}}
                                    """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"local browser\",\"paired_by\":\"open\"}]");
            } else if ("/api/v1/reports".equals(request.target())) {
                response.json(200, "{\"unread_count\":0}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("a bare number is answered with the not-a-command hint")
    void bareNumber() throws Exception {
        onboardedWithQuestion();
        var rendered = new ByteArrayOutputStream();
        var code = new PmConsole(context(rendered, "2\nexit\n")).run();
        assertEquals(0, code);
        assertTrue(rendered.toString(StandardCharsets.UTF_8).contains("2 is not a command here"),
                            rendered.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Ctrl+D names the open questions once, then leaves on the second one")
    void ctrlDKeepsQuestions() throws Exception {
        onboardedWithQuestion();
        var rendered = new ByteArrayOutputStream();
        var code = new PmConsole(context(rendered, "\u0004\u0004")).run();
        assertEquals(0, code);
        var text = rendered.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("2 questions stay open"), text);
        assertTrue(text.contains("exit or Ctrl+D again leaves"), text);
    }

    @Test
    @DisplayName("a notice is held while the view is open and summarized when it leaves")
    void noticesAreHeldWhileTheViewIsOpen() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.enterFullScreen();
            console.applyConsoleEvent("question", Map.of("type", "question", "id", "Q-1",
                                "scope", "plan fix-relay", "summary", "blocks the plan"), reader);
            console.applyConsoleEvent("report", Map.of("type", "report", "id", "r-1",
                                "from", "pm-mcp-clients", "subject", "merged"), reader);

            var printed = new StringWriter();
            console.leaveFullScreen(new PrintWriter(printed));

            assertAll(
                                () -> assertFalse(rendered.toString(StandardCharsets.UTF_8).contains("Question Q-1"),
                                                    rendered.toString(StandardCharsets.UTF_8)),
                                () -> assertTrue(printed.toString().contains(
                                                                        "While the overview was open: 1 decision arrived, 1 new report."),
                                                    printed.toString())
            );
        }
    }

    @Test
    @DisplayName("an envelope decision notifies with the scope the envelope declares")
    void notificationFromEnvelope() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            new PmConsole(context).applyConsoleEvent("decision.opened", Map.of(
                                                    "event", "decision.opened",
                                                    "id", 7,
                                                    "scope", Map.of("kind", "plan", "id", "fix-relay"),
                                                    "data", Map.of("decision", Map.of(
                                                                        "id", "Q-007", "subject", "plan fix-relay", "blocks", "plan"))),
                                reader);
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        final var start = text.indexOf("\u001B]9;");
        final var notification = text.substring(start, text.indexOf('\u0007', start));
        assertAll(
                            () -> assertTrue(notification.contains("plan fix-relay"), notification),
                            () -> assertTrue(notification.contains("decision"), notification),
                            () -> assertFalse(notification.contains("Q-007"), notification)
        );
    }

    @Test
    @DisplayName("the notification payload names the scope and the kind, never the question text")
    void notificationPayload() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            new PmConsole(context).applyConsoleEvent("question", Map.of(
                                "type", "question", "id", "Q-1", "scope", "plan fix-relay",
                                "summary", "a secret summary", "text", "the text of the question"), reader);
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        var notification = text.substring(text.indexOf("\u001B]9;"));
        notification = notification.substring(0, notification.indexOf('\u0007'));
        assertTrue(notification.contains("plan fix-relay"), notification);
        assertTrue(notification.contains("decision"), notification);
        assertFalse(notification.contains("a secret summary"), notification);
        assertFalse(notification.contains("the text of the question"), notification);
    }
}
