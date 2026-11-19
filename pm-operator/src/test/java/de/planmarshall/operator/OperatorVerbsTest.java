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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jline.reader.Candidate;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("OperatorVerbs")
class OperatorVerbsTest {

    private static final String STATUS_JSON = """
                        {"status":"ready","onboarded":true,"version":"0.9.0",
                         "needs_operator":{"question":0},
                         "web":{"enabled":false,"port":7420}}
                        """;

    @TempDir
    Path tempDir;

    private PmConsole console(RuntimeFixture fixture) {
        var rendered = new ByteArrayOutputStream();
        var input = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(input, rendered), null);
        return new PmConsole(context);
    }

    @Test
    @DisplayName("the completer carries the registered verbs and the console's own commands, each once")
    void completerCoversRegisteredAndConsoleCommands() {
        var completer = OperatorVerbs.completer();

        assertTrue(completer.containsAll(OperatorVerbs.registered()), completer.toString());
        assertTrue(completer.containsAll(List.of("exit", "quit", "help", "tui")), completer.toString());
        assertEquals(Set.copyOf(completer).size(), completer.size(), "a word appears twice: " + completer);
    }

    @Test
    @DisplayName("the completer the console installs in its reader is OperatorVerbs.completer()")
    void consoleCompleterMatchesOperatorVerbs() throws Exception {
        try (var fixture = RuntimeFixture.create()) {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, STATUS_JSON);
                } else if ("/api/v1/reports".equals(request.target())) {
                    response.json(200, "{\"unread_count\":0}");
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, "[]");
                } else {
                    response.send(404, null, "");
                }
            });
            var console = console(fixture);

            assertEquals(0, console.run());

            var installed = console.installedCompleter();
            assertNotNull(installed, "the console ran without installing a completer");
            var candidates = new ArrayList<Candidate>();
            installed.complete(null, new DefaultParser().parse("", 0), candidates);
            var words = candidates.stream().map(Candidate::value).toList();
            assertEquals(Set.copyOf(OperatorVerbs.completer()), Set.copyOf(words));
        }
    }

    @Test
    @DisplayName("the help sentence names every registered verb, in both languages")
    void helpMoreNamesEveryVerb() {
        for (var locale : List.of("en", "de")) {
            var words = words(OperatorVerbs.helpMore(locale));

            for (var verb : OperatorVerbs.registered()) {
                assertTrue(words.contains(verb), locale + " omits " + verb + ": " + words);
            }
        }
    }

    @Test
    @DisplayName("the help sentence carries no literal placeholder")
    void helpMoreCarriesNoPlaceholder() {
        assertFalse(OperatorVerbs.helpMore("en").contains("{verbs}"), OperatorVerbs.helpMore("en"));
    }

    private static List<String> words(String text) {
        return Arrays.stream(text.split("[^a-z-]+"))
                            .filter(word -> !word.isBlank())
                            .toList();
    }
}
