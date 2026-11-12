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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * What the console does with the events it follows and with the empty prompt beyond the
 * transcripts: a pairing reported once, the badges read again after a change of what waits, a
 * report count summarized after the view, Enter on a badge without an address, and the tunnel
 * {@code web copy ssh} copies.
 */
@EnableGeneratorController
@DisplayName("PmConsole events and prompt")
class PmConsoleEventsTest {

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

    private OperatorContext context(ByteArrayOutputStream rendered) {
        return new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
    }

    @Test
    @DisplayName("a pairing is reported once, with its check mark, whether it arrives named or typed")
    void aPairingIsReportedOnce() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/web/pairings".equals(request.target())) {
                response.json(201, "{\"code\":\"FRESH-CODE\",\"url\":\"http://127.0.0.1:7420/pair#code=FRESH-CODE\"}");
            } else {
                response.send(404, null, "");
            }
        });
        var label = Generators.letterStrings(6, 12).next();
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var console = new PmConsole(context(rendered));
            console.applyConsoleEvent(null, Map.of("type", "paired", "label", label), reader);
            console.applyConsoleEvent("device.paired", Map.of("type", "paired", "label", label), reader);
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        assertAll(
                            () -> assertEquals(2, occurrences(text, "Paired: " + label), text),
                            () -> assertEquals(2, occurrences(text, "✓ Paired: " + label), text),
                            () -> assertTrue(text.contains("FRESH-CODE"), text)
        );
    }

    @Test
    @DisplayName("Enter on a project badge without an address continues nothing")
    void enterOnAnUnaddressedProjectBadgeDoesNothing() throws Exception {
        var checkout = Generators.letterStrings(4, 10).next();
        var addressed = new AtomicBoolean(false);
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/entries") || request.target().startsWith("/api/v1/onboarding")) {
                addressed.set(true);
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true,\"needs_operator\":{\"question\":1}}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[{\"checkout_path\":\"/work/" + checkout
                                    + "\",\"current_step\":\"build\",\"step_number\":2,\"total_steps\":10}]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var console = new PmConsole(context(rendered));
            console.refreshSetupBadge("en", terminal);
            var before = rendered.size();
            console.handleEmptyEnter(new PrintWriter(rendered, true), reader, terminal);
            assertAll(
                                () -> assertTrue(console.currentBadges().contains("setup " + checkout + " 2/10"),
                                                    console.currentBadges().toString()),
                                () -> assertFalse(addressed.get(), "a badge without an address continues nothing"),
                                () -> assertEquals(before, rendered.size(), "Enter prints nothing either")
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"decision.withdrawn", "hold.cleared", "condition.resolved", "needs_operator.changed"})
    @DisplayName("an event that can change what waits reads the badges again, even when it prints no notice")
    void aChangeOfWhatWaitsReadsTheBadges(String eventName) throws Exception {
        var statusReads = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                statusReads.incrementAndGet();
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true,\"needs_operator\":{\"question\":1}}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var console = new PmConsole(context(rendered));
            console.applyConsoleEvent(eventName, Map.of("hold_id", "h-1"), reader);
            console.applyConsoleEvent("task.off_track", Map.of("plan", "fix-relay"), reader);
            assertAll(
                                () -> assertEquals(1, statusReads.get(), "only the change of what waits reads the state"),
                                () -> assertEquals(List.of("1 question"), console.currentBadges())
            );
        }
    }

    @Test
    @DisplayName("reports that arrive as one count while the view is open are summarized with that count")
    void aReportCountIsSummarized() throws Exception {
        var count = Generators.integers(2, 50).next();
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var console = new PmConsole(context(rendered));
            console.enterFullScreen();
            console.applyConsoleEvent("report.sent", Map.of("count", count, "scope", "pm-mcp-clients"), reader);
            var printed = new StringWriter();
            console.leaveFullScreen(new PrintWriter(printed));
            assertTrue(printed.toString().contains(count + " new reports"), printed.toString());
        }
    }

    @Test
    @DisplayName("web copy ssh copies the tunnel to the host the environment names")
    void webCopySshNamesTheHost() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                    """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"local browser\",\"paired_by\":\"open\"}]");
            } else {
                response.send(404, null, "");
            }
        });
        var host = Generators.letterStrings(4, 12).next();
        var user = Generators.letterStrings(4, 12).next();
        var environment = new HashMap<>(fixture.environment());
        environment.put("HOSTNAME", host);
        environment.put("USER", user);
        var rendered = new ByteArrayOutputStream();
        var input = new ByteArrayInputStream("web copy ssh\nexit\n".getBytes(StandardCharsets.UTF_8));
        var context = new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(input, rendered), null);

        assertEquals(0, new PmConsole(context).run());

        var text = rendered.toString(StandardCharsets.UTF_8);
        var marker = "\u001B]52;c;";
        var start = text.indexOf(marker) + marker.length();
        var copied = new String(Base64.getDecoder().decode(text.substring(start, text.indexOf('\u0007', start))),
                            StandardCharsets.UTF_8);
        assertEquals("ssh -L 7420:127.0.0.1:7420 " + user + "@" + host, copied);
    }

    private static int occurrences(String text, String part) {
        var count = 0;
        for (var at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            count++;
        }
        return count;
    }
}
