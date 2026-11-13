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

import de.cuioss.test.generator.Generators;
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

import de.planmarshall.api.json.JsonNumber;
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
        var key = Generators.letterStrings(4, 12).next();
        var credentialNotice = "Credential " + key + " needs attention";
        var console = console();
        // The first summary of a place is the baseline: what already waited is badged, not announced.
        console.applyConsoleEvent("needs_operator.changed", machineEvent("none", 0), reader);
        console.applyConsoleEvent("needs_operator.changed", machineEvent("credential", 2,
                            item("credential", "/api/v1/credentials/" + key),
                            item("cleanup_due", "/api/v1/system/cleanup")), reader);
        var afterArrival = output();

        console.applyConsoleEvent("needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/" + key)), reader);
        var afterDecrease = output();

        assertAll(
                            () -> assertEquals(1, count(afterArrival, credentialNotice), afterArrival),
                            () -> assertEquals(1, count(afterArrival, "Cleanup is due"), afterArrival),
                            () -> assertEquals(2, count(afterArrival, DISPLAY_MARK), afterArrival),
                            () -> assertEquals(2, count(afterArrival, NOTIFICATION), afterArrival),
                            () -> assertEquals(1, count(afterDecrease, credentialNotice), afterDecrease),
                            () -> assertEquals(1, count(afterDecrease, "Cleanup is due"), afterDecrease),
                            () -> assertEquals(2, count(afterDecrease, DISPLAY_MARK), afterDecrease),
                            () -> assertEquals(2, count(afterDecrease, NOTIFICATION), afterDecrease)
        );
    }

    @Test
    @DisplayName("a plan-level decision change after decision.opened yields exactly one notice")
    void decisionChangeAfterDecisionOpenedYieldsOneNotice() {
        var decision = "Q-" + Generators.integers(100, 999).next();
        var plan = Generators.letterStrings(4, 12).next();
        var console = console();
        console.applyConsoleEvent("decision.opened", Map.of(
                            "event", "decision.opened",
                            "data", Map.of("decision", Map.of(
                                                "id", decision, "subject", "plan " + plan, "blocks", "plan"))), reader);
        console.applyConsoleEvent("needs_operator.changed", Map.of(
                            "plan_id", plan, "kind", "decision", "count", 1), reader);

        var text = output();
        assertAll(
                            () -> assertEquals(1, count(text, "Question " + decision), text),
                            () -> assertEquals(1, count(text, DISPLAY_MARK), text),
                            () -> assertEquals(1, count(text, NOTIFICATION), text)
        );
    }

    @Test
    @DisplayName("a machine summary that adds a credential yields one notice naming the sanitized key")
    void addedCredentialYieldsOneNotice() {
        var head = Generators.letterStrings(2, 8).next();
        var tail = Generators.letterStrings(2, 8).next();
        var console = console();
        console.applyConsoleEvent("needs_operator.changed", machineEvent("none", 0), reader);
        console.applyConsoleEvent("needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/" + head + "\u0007" + tail)), reader);

        var text = output();
        assertAll(
                            () -> assertTrue(text.contains("Credential " + head + tail + " needs attention"), text),
                            () -> assertFalse(text.contains(head + "\u0007" + tail),
                                                "no control character may reach the screen"),
                            () -> assertEquals(1, count(text, DISPLAY_MARK), text),
                            () -> assertEquals(1, count(text, NOTIFICATION), text)
        );
    }

    @Test
    @DisplayName("a credential arriving as the first event after a start whose status read showed none raises one notice")
    void firstCredentialAfterAnEmptyStartIsAnnounced() {
        var key = Generators.letterStrings(4, 12).next();
        var notices = new NeedsOperatorNotices();
        notices.seedFromStatus(Map.of("needs_operator", Map.of()));

        var raised = notices.noticesOf("en", "needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/" + key)));

        assertAll(
                            () -> assertEquals(1, raised.size(), raised.toString()),
                            () -> assertEquals("credential", raised.getFirst().kind()),
                            () -> assertTrue(raised.getFirst().text().contains("Credential " + key + " needs attention"),
                                                raised.getFirst().text())
        );
    }

    @Test
    @DisplayName("an item the status read at start already showed raises no notice")
    void itemTheStartShowedIsNotAnnouncedAgain() {
        var key = Generators.letterStrings(4, 12).next();
        var notices = new NeedsOperatorNotices();
        notices.seedFromStatus(Map.of("needs_operator", Map.of("credential", 1)));

        var raised = notices.noticesOf("en", "needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/" + key)));

        assertTrue(raised.isEmpty(), raised.toString());
    }

    @Test
    @DisplayName("an item beyond what the status read showed is announced, one notice for the one added")
    void itemBeyondTheStartIsAnnounced() {
        var shown = Generators.letterStrings(4, 12).next();
        var added = shown + "-" + Generators.letterStrings(2, 6).next();
        var notices = new NeedsOperatorNotices();
        notices.seedFromStatus(Map.of("needs_operator", Map.of("credential", 1)));

        var raised = notices.noticesOf("en", "needs_operator.changed", machineEvent("credential", 2,
                            item("credential", "/api/v1/credentials/" + shown),
                            item("credential", "/api/v1/credentials/" + added)));

        assertAll(
                            () -> assertEquals(1, raised.size(), raised.toString()),
                            () -> assertEquals(added, raised.getFirst().place())
        );
    }

    @Test
    @DisplayName("a negative count in the status read showed nothing: the first item is announced")
    void negativeStatusCountShowsNothing() {
        var key = Generators.letterStrings(4, 12).next();
        var negative = JsonNumber.of(-(long) Generators.integers(1, 1000).next());
        var notices = new NeedsOperatorNotices();
        notices.seedFromStatus(Map.of("needs_operator", Map.of("credential", negative)));

        var raised = notices.noticesOf("en", "needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/" + key)));

        assertEquals(1, raised.size(), raised.toString());
    }

    @Test
    @DisplayName("a count beyond the int range in the status read is the largest count: nothing is announced")
    void hugeStatusCountIsClampedToTheLargest() {
        var key = Generators.letterStrings(4, 12).next();
        var huge = JsonNumber.of(Integer.MAX_VALUE + (long) Generators.integers(1, 1000).next());
        var notices = new NeedsOperatorNotices();
        notices.seedFromStatus(Map.of("needs_operator", Map.of("credential", huge)));

        var raised = notices.noticesOf("en", "needs_operator.changed", machineEvent("credential", 1,
                            item("credential", "/api/v1/credentials/" + key)));

        assertTrue(raised.isEmpty(), raised.toString());
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
