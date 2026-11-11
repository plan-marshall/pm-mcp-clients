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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.FindKind;
import de.planmarshall.api.overview.FindMatch;
import de.planmarshall.api.overview.WorkLocation;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Proves {@code pm-operator find} on the {@code running-work} scenario (PLAN-84 D1) and the
 * catalogue of its texts (PLAN-84 D2).
 */
@EnableGeneratorController
@DisplayName("FindCommand")
class FindCommandTest {

    private static final String RUNNING_TITLE = "Fix Relay Transport";

    private static final String ENDED_TITLE = "fix-relay-v1";

    private static final String AREA = "operator";

    @TempDir
    Path tempDir;

    /** What the console's own terminal drew; the console writes there, not to the verb's writer. */
    private ByteArrayOutputStream screen;

    /**
     * @param environment the environment of the mock runtime
     * @param interactive whether the context stands for a terminal that can choose a match
     * @param input       the keys the terminal reads, in order
     * @return a context over the mock, whose terminal prints and opens no full-screen view
     */
    private OperatorContext context(Map<String, String> environment, boolean interactive, String input) {
        screen = new ByteArrayOutputStream();
        var terminal = dumbTerminal(input, screen);
        return new OperatorContext(
                            environment,
                            tempDir,
                            interactive,
                            ClientEnvironment.capture(environment),
                            () -> terminal,
                            null
        );
    }

    private OperatorContext context(Map<String, String> environment) {
        return context(environment, false, "");
    }

    private static MockRuntime runningWork() throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder()
                            .webPort(0)
                            .scenario(StandardScenarios.RUNNING_WORK)
                            .build());
    }

    private static String run(OperatorContext context, String... args) {
        var out = new StringWriter();
        var err = new StringWriter();
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        assertEquals(0, code, "find should exit 0, err: " + err);
        return out.toString();
    }

    private static String runSoftly(OperatorContext context, String... args) {
        var out = new StringWriter();
        var err = new StringWriter();
        PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        return out.toString() + err;
    }

    private static DumbTerminal dumbTerminal(String input) {
        return dumbTerminal(input, new ByteArrayOutputStream());
    }

    private static DumbTerminal dumbTerminal(String input, ByteArrayOutputStream output) {
        try {
            return new DumbTerminal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output);
        } catch (IOException e) {
            throw new IllegalStateException("a dumb terminal always opens", e);
        }
    }

    private String consoleOutput() {
        return screen.toString(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("on the running-work scenario")
    class OnRunningWork {

        @Test
        @DisplayName("matches by id, keeps the runtime's order and marks ended work")
        void matchesById() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", "relay", "--print");

                assertAll(
                                    () -> assertTrue(output.contains("Go to work by name"), output),
                                    () -> assertTrue(output.contains("find relay"), output),
                                    () -> assertTrue(output.contains(RUNNING_TITLE), output),
                                    () -> assertTrue(output.contains(ENDED_TITLE), output),
                                    () -> assertTrue(output.contains("ended"), output),
                                    () -> assertTrue(output.contains("merged"), output),
                                    () -> assertTrue(output.contains("opens with plan fix-relay"), output)
                );
            }
        }

        @Test
        @DisplayName("prints running work first, grouped by kind")
        void runningFirstGroupedByKind() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", "relay", "--print");

                var epic = output.indexOf("Relay Hardening");
                var plan = output.indexOf(RUNNING_TITLE);
                var ended = output.indexOf(ENDED_TITLE);

                assertAll(
                                    () -> assertTrue(epic >= 0 && plan >= 0 && ended >= 0, output),
                                    () -> assertTrue(epic < plan, "the epic stands before the plan: " + output),
                                    () -> assertTrue(plan < ended, "running work stands before ended work: " + output)
                );
            }
        }

        @Test
        @DisplayName("matches by a title")
        void matchesByTitle() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", "Relay Hardening", "--print");

                assertTrue(output.contains("Relay Hardening"), output);
            }
        }

        @Test
        @DisplayName("takes a title typed without quotes")
        void takesAnUnquotedTitle() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", "Relay", "Hardening", "--print");

                assertTrue(output.contains("Relay Hardening"), output);
            }
        }

        @Test
        @DisplayName("matches by a pull request number")
        void matchesByPullRequestNumber() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", "214", "--print");

                assertAll(
                                    () -> assertTrue(output.contains(RUNNING_TITLE), output),
                                    () -> assertTrue(output.contains(ENDED_TITLE), output)
                );
            }
        }

        @Test
        @DisplayName("no match says so and repeats the text typed")
        void noMatchKeepsTheText() throws Exception {
            var text = "absent-" + Generators.letterStrings(8, 14).next();
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", text, "--print");

                assertAll(
                                    () -> assertTrue(output.contains(text), output),
                                    () -> assertTrue(output.contains("Nothing matches"), output)
                );
            }
        }

        @Test
        @DisplayName("the empty text is refused with the catalogue text, not with a usage line")
        void refusedWithoutText() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())), "find");

                assertAll(
                                    () -> assertTrue(output.contains("Type a name to look for."), output),
                                    () -> assertFalse(output.contains("Usage:"), output)
                );
            }
        }

        @Test
        @DisplayName("a text of blanks is refused like an empty one")
        void refusedForBlanks() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString())),
                                    "find", "   ");

                assertTrue(output.contains("Type a name to look for."), output);
            }
        }
    }

    @Nested
    @DisplayName("the rendered page")
    class RenderedPage {

        @Test
        @DisplayName("removes the control sequences of a title")
        void removesControlSequences() throws Exception {
            var title = "\u001b[31mRelay Transport\u001b[0m";
            var output = new StringWriter();

            new FindCommand(context(Map.of())).executeFind(sender(page(title)), new PrintWriter(output), "en", "query");

            var rendered = output.toString();
            assertAll(
                                () -> assertFalse(rendered.contains("\u001b"), "no control sequence reaches the screen"),
                                () -> assertTrue(rendered.contains("Relay Transport"), rendered)
            );
        }

        @Test
        @DisplayName("removes the control sequences of the query the runtime repeats")
        void removesControlSequencesFromTheQuery() throws Exception {
            var output = new StringWriter();
            var page = "{\"page\":1,\"total_pages\":1,\"has_newer\":false,\"has_older\":false,"
                                + "\"query\":\"\\u001b[31mzzz\\u001b[0m\",\"matches\":[]}";

            new FindCommand(context(Map.of())).executeFind(sender(page), new PrintWriter(output), "en", "zzz");

            var rendered = output.toString();
            assertAll(
                                () -> assertFalse(rendered.contains("\u001b"), rendered),
                                () -> assertTrue(rendered.contains("zzz"), rendered)
            );
        }

        @Test
        @DisplayName("removes the control sequences of an outcome")
        void removesControlSequencesFromAnOutcome() {
            var match = new FindMatch(FindKind.PLAN, "id-1", "Title", null, null, null, true,
                                "\u001b[31mmerged\u001b[0m");

            var rendered = FindCommand.endedOf(match, "en");

            assertAll(
                                () -> assertFalse(rendered.contains("\u001b"), rendered),
                                () -> assertTrue(rendered.startsWith("ended"), rendered)
            );
        }

        @Test
        @DisplayName("shows the path of the titles a match belongs to")
        void showsThePath() {
            var match = new FindMatch(FindKind.PLAN, "fix-relay", RUNNING_TITLE,
                                new WorkLocation("pm-mcp-clients", "pm-mcp-clients"),
                                new WorkLocation("relay-hardening", "Relay Hardening"),
                                new WorkLocation("fix-relay", RUNNING_TITLE), false, null);

            assertEquals("pm-mcp-clients \u203a Relay Hardening", FindCommand.locationOf(match, "en"));
        }

        @Test
        @DisplayName("shows no path for a project")
        void projectHasNoPath() {
            var match = new FindMatch(FindKind.PROJECT, "pm-mcp-clients", "pm-mcp-clients",
                                new WorkLocation("pm-mcp-clients", "pm-mcp-clients"), null, null, false, null);

            assertEquals("", FindCommand.locationOf(match, "en"));
        }

        @Test
        @DisplayName("shows nothing for a location without a title the catalogue cannot name")
        void untitledLocationShowsNothing() {
            var match = new FindMatch(FindKind.PLAN, "p-1", "Title",
                                new WorkLocation("pm-mcp-clients", null), null, null, false, null);

            assertEquals("", FindCommand.locationOf(match, "en"));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(FindKind.class)
        @DisplayName("names the command that opens a match")
        void namesTheOpenCommand(FindKind kind) {
            var match = new FindMatch(kind, "id-1", "Title", null, null, null, false, null);

            assertEquals(expectedCommand(kind), FindCommand.openCommand(match));
        }

        @Test
        @DisplayName("marks ended work with its outcome and leaves running work unmarked")
        void marksEndedWork() {
            var ended = new FindMatch(FindKind.PLAN, "id-1", "Title", null, null, null, true, "merged");
            var running = new FindMatch(FindKind.PLAN, "id-2", "Title", null, null, null, false, null);

            assertAll(
                                () -> assertEquals("ended \u00b7 merged", FindCommand.endedOf(ended, "en")),
                                () -> assertEquals("", FindCommand.endedOf(running, "en"))
            );
        }

        private static String expectedCommand(FindKind kind) {
            return switch (kind) {
                case PLAN -> "plan id-1";
                case EPIC -> "epic id-1";
                case DECISION -> "answer id-1";
                case REPORT -> "reports open id-1";
                case PROJECT -> "projects --filter id-1";
            };
        }

        private static FindCommand.RuntimeSender sender(String page) {
            return request -> new HttpResponse(200, "application/json", page.getBytes(StandardCharsets.UTF_8));
        }

        private static String page(String title) {
            var match = new FindMatch(FindKind.PLAN, "p-1", title, null, null, null, false, null);
            return "{\"page\":1,\"total_pages\":1,\"has_newer\":false,\"has_older\":false,"
                                + "\"query\":\"query\",\"matches\":[" + match.toJson() + "]}";
        }
    }

    @Nested
    @DisplayName("the request")
    class Request {

        @Test
        @DisplayName("carries the text as the query value of the find path")
        void carriesTheText() {
            assertEquals(MachineApiPaths.FIND + "?q=relay+hardening", FindCommand.findPath("relay hardening"));
        }

        @Test
        @DisplayName("sends the find path and no other")
        void sendsTheFindPath() throws Exception {
            var seen = new StringWriter();
            FindCommand.RuntimeSender sender = request -> {
                seen.append(request.target());
                return new HttpResponse(200, "application/json",
                                    ("{\"page\":1,\"total_pages\":1,\"has_newer\":false,\"has_older\":false,"
                                                        + "\"query\":\"relay\",\"matches\":[]}")
                                                        .getBytes(StandardCharsets.UTF_8));
            };

            new FindCommand(context(Map.of())).executeFind(sender, new PrintWriter(new StringWriter()), "en", "relay");

            assertEquals("/api/v1/find?q=relay", seen.toString());
        }

        @Test
        @DisplayName("reports a refused read as a refusal of the runtime")
        void refusedRead() {
            var command = new FindCommand(context(Map.of()));
            FindCommand.RuntimeSender sender = request -> new HttpResponse(500, null, new byte[0]);

            assertThrows(OperatorRefusedException.class,
                                () -> command.executeFind(sender, new PrintWriter(new StringWriter()), "en", "relay"));
        }
    }

    @Nested
    @DisplayName("the catalogue")
    class Catalogue {

        @Test
        @DisplayName("carries every find text of the command in both languages")
        void carriesEveryKeyInBothLanguages() {
            var english = keysOf("en");

            assertAll(
                                () -> assertFalse(english.isEmpty(), "the command names no operator.find key"),
                                () -> assertEquals(english, keysOf("de"), "the two languages carry different keys")
            );
        }

        @Test
        @DisplayName("renders every find text without a leftover placeholder in both languages")
        void rendersEveryKeyInBothLanguages() {
            for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
                for (var key : keysOf(locale)) {
                    var rendered = MessageCatalogue.format(locale, key, arguments());
                    assertFalse(rendered.contains("{"), locale + " leaves a placeholder in " + key + ": " + rendered);
                    assertFalse(rendered.isBlank(), locale + " renders nothing for " + key);
                }
            }
        }

        private static Map<String, Object> arguments() {
            var arguments = new LinkedHashMap<String, Object>();
            arguments.put("text", "relay");
            arguments.put("count", 2);
            arguments.put("command", "plan fix-relay");
            return arguments;
        }

        private static Set<String> keysOf(String locale) {
            var keys = new TreeSet<String>();
            for (var key : MessageCatalogue.getMessages(locale, AREA).keySet()) {
                if (key.startsWith("operator.find.")) {
                    keys.add(key);
                }
            }
            return keys;
        }
    }

    @Nested
    @DisplayName("the console field")
    class Field {

        @Test
        @DisplayName("answers Esc by returning nothing")
        void escLeavesTheField() {
            assertNull(FindCommand.readField(readerOf("\u001b"), ""));
        }

        @Test
        @DisplayName("answers Ctrl+C as Esc does")
        void interruptLeavesTheField() {
            assertNull(FindCommand.readField(readerOf("\u0003"), ""));
        }

        @Test
        @DisplayName("answers an ended stream by returning nothing")
        void endLeavesTheField() {
            assertNull(FindCommand.readField(readerOf(""), ""));
        }

        @Test
        @DisplayName("reads the typed text")
        void readsTheTypedText() {
            assertEquals("relay", FindCommand.readField(readerOf("  relay \n"), ""));
        }

        @Test
        @DisplayName("erases the last character on backspace")
        void erasesOnBackspace() {
            assertEquals("rela", FindCommand.readField(readerOf("relay\u007f\r"), ""));
        }

        @Test
        @DisplayName("ignores a backspace that would erase nothing")
        void ignoresBackspaceOnEmptyText() {
            assertEquals("r", FindCommand.readField(readerOf("\u007fr\r"), ""));
        }

        @Test
        @DisplayName("keeps the leave key of a language")
        void leaveKeyFollowsTheLanguage() {
            assertAll(
                                () -> assertEquals("q", FindCommand.leaveKey("en")),
                                () -> assertEquals("v", FindCommand.leaveKey("de_DE.UTF-8"))
            );
        }

        private static LineReader readerOf(String input) {
            return LineReaderBuilder.builder().terminal(dumbTerminal(input)).build();
        }
    }

    @Nested
    @DisplayName("the choice in a terminal")
    class Choice {

        @Test
        @DisplayName("offers the matches and leaves on q without opening one")
        void leavesOnLeaveKey() throws Exception {
            try (var runtime = runningWork()) {
                var output = run(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()), true, "q\n"),
                                    "find", "relay");

                assertAll(
                                    () -> assertTrue(output.contains("Open a match"), output),
                                    () -> assertFalse(output.contains("is not offered here"), output)
                );
            }
        }

        @Test
        @DisplayName("re-asks for a number that is not offered and opens the one selected")
        void opensTheSelectedMatch() throws Exception {
            try (var runtime = runningWork()) {
                var output = runSoftly(context(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()), true,
                                    "99\n2\n"), "find", "relay");

                assertAll(
                                    () -> assertTrue(output.contains("99 is not offered here"), output),
                                    () -> assertTrue(output.contains("Fix Relay Transport"), output)
                );
            }
        }
    }

    @Nested
    @DisplayName("the console's / field")
    class Console {

        @Test
        @DisplayName("/relay lists the matches and q returns to the prompt")
        void slashWithTextListsTheMatches() throws Exception {
            try (var runtime = runningWork()) {
                var output = console(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()),
                                    "/relay\nq\nexit\n");

                assertAll(
                                    () -> assertTrue(output.contains("Go to work by name"), output),
                                    () -> assertTrue(output.contains("Fix Relay Transport"), output)
                );
            }
        }

        @Test
        @DisplayName("a bare / opens the field and Enter lists the matches")
        void bareSlashOpensTheField() throws Exception {
            try (var runtime = runningWork()) {
                var output = console(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()),
                                    "/\nrelay\nq\nexit\n");

                assertTrue(output.contains("Fix Relay Transport"), output);
            }
        }

        @Test
        @DisplayName("an empty field is refused with the catalogue text")
        void emptyFieldIsRefused() throws Exception {
            try (var runtime = runningWork()) {
                var output = console(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()),
                                    "/\n\n\nexit\n");

                assertTrue(output.contains("Type a name to look for."), output);
            }
        }

        @Test
        @DisplayName("Esc leaves the field with nothing sent")
        void escLeavesTheField() throws Exception {
            try (var runtime = runningWork()) {
                var output = console(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()), "/\n\u001b");

                assertFalse(output.contains("Fix Relay Transport"), output);
            }
        }

        @Test
        @DisplayName("a typed name is passed after -- so it is never read as an option")
        void typedNameTravelsAfterTheOptionTerminator() throws Exception {
            try (var runtime = runningWork()) {
                var output = console(Map.of(MachinePaths.ENV_BASE, runtime.base().toString()), "/--print\nexit\n");

                assertAll(
                                    () -> assertTrue(output.contains("Nothing matches \u201c--print\u201d."), output),
                                    () -> assertFalse(output.contains("Usage:"), output)
                );
            }
        }

        /**
         * @param environment the environment of the mock runtime
         * @param keys        the keys the operator types at the console
         * @return what the console drew
         */
        private String console(Map<String, String> environment, String keys) {
            var context = context(environment, true, keys);
            PmOperator.execute(context, new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter()));
            return consoleOutput();
        }
    }
}
