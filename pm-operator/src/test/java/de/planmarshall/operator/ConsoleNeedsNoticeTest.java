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

import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The console's notice rule for {@code needs_operator.changed} (04-console.adoc#pushed): a notice is
 * raised for each item a change ADDS of a kind no other event announces — never for a decrease, and
 * never a second notice for an item {@code decision.opened}, {@code hold.opened} or
 * {@code condition.raised} already announced.
 */
@EnableGeneratorController
@DisplayName("The console's notices for what a change adds")
class ConsoleNeedsNoticeTest {

    private static final String DISPLAY_MARK = "\u25c6";
    private static final String NOTIFICATION = "\u001B]9;";

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private ByteArrayOutputStream rendered;
    private Terminal terminal;
    private LineReader reader;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0",
                                     "needs_operator":{},
                                     "web":{"enabled":false,"port":7420}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });
        rendered = new ByteArrayOutputStream();
        terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered);
        reader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (terminal != null) {
            terminal.close();
        }
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("a decrease raises no notice and no bell")
    void decreaseRaisesNoNotice() {
        var console = console();
        // The first summary of a place is the baseline: what already waited is badged, not announced.
        console.applyConsoleEvent("needs_operator.changed", machineEvent("none", 0), reader);
        console.applyConsoleEvent("needs_operator.changed", machineEvent("credential", 2,
                            item("credential", "/api/v1/credentials/github"),
                            item("cleanup_due", "/api/v1/system/cleanup")), reader);
        var afterArrival = output();

        console.applyConsoleEvent("needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/github")), reader);
        var afterDecrease = output();

        assertAll(
                            () -> assertEquals(1, count(afterArrival, "Credential github needs attention"), afterArrival),
                            () -> assertEquals(1, count(afterArrival, "Cleanup is due"), afterArrival),
                            () -> assertEquals(2, count(afterArrival, DISPLAY_MARK), afterArrival),
                            () -> assertEquals(2, count(afterArrival, NOTIFICATION), afterArrival),
                            () -> assertEquals(1, count(afterDecrease, "Credential github needs attention"), afterDecrease),
                            () -> assertEquals(1, count(afterDecrease, "Cleanup is due"), afterDecrease),
                            () -> assertEquals(2, count(afterDecrease, DISPLAY_MARK), afterDecrease),
                            () -> assertEquals(2, count(afterDecrease, NOTIFICATION), afterDecrease)
        );
    }

    @Test
    @DisplayName("a plan-level decision change after decision.opened yields exactly one notice")
    void decisionChangeAfterDecisionOpenedYieldsOneNotice() {
        var console = console();
        console.applyConsoleEvent("decision.opened", Map.of(
                            "event", "decision.opened",
                            "data", Map.of("decision", Map.of(
                                                "id", "Q-007", "subject", "plan fix-relay", "blocks", "plan"))), reader);
        console.applyConsoleEvent("needs_operator.changed", Map.of(
                            "plan_id", "fix-relay", "kind", "decision", "count", 1), reader);

        var text = output();
        assertAll(
                            () -> assertEquals(1, count(text, "Question Q-007"), text),
                            () -> assertEquals(1, count(text, DISPLAY_MARK), text),
                            () -> assertEquals(1, count(text, NOTIFICATION), text)
        );
    }

    @Test
    @DisplayName("a machine summary that adds a credential yields one notice naming the sanitized key")
    void addedCredentialYieldsOneNotice() {
        var console = console();
        console.applyConsoleEvent("needs_operator.changed", machineEvent("none", 0), reader);
        console.applyConsoleEvent("needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/git\u0007hub")), reader);

        var text = output();
        assertAll(
                            () -> assertTrue(text.contains("Credential github needs attention"), text),
                            () -> assertFalse(text.contains("git\u0007hub"), "no control character may reach the screen"),
                            () -> assertEquals(1, count(text, DISPLAY_MARK), text),
                            () -> assertEquals(1, count(text, NOTIFICATION), text)
        );
    }

    private PmConsole console() {
        var environment = fixture.environment();
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment),
                            () -> terminal, null);
        return new PmConsole(context);
    }

    private String output() {
        terminal.writer().flush();
        return rendered.toString(StandardCharsets.UTF_8);
    }

    private static int count(String text, String needle) {
        int found = 0;
        int at = text.indexOf(needle);
        while (at >= 0) {
            found++;
            at = text.indexOf(needle, at + needle.length());
        }
        return found;
    }

    private static Map<String, Object> item(String kind, String link) {
        return Map.of("kind", kind, "link", link);
    }

    private static Map<String, Object> machineEvent(String kind, int count, Map<String, Object>... items) {
        var summary = new ArrayList<Map<String, Object>>(List.of(items));
        return Map.of("kind", kind, "count", count,
                            "needs_operator", Map.of("count", summary.size(), "items", summary));
    }
}
