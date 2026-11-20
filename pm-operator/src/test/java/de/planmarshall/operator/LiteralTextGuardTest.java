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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The bilingual text guard (the plan's "no text is written in the client's code"): a test that
 * fails for a literal user-facing string.
 *
 * <p>It scans every command of the operator package — discovered from the sources, not listed — and
 * the console, dialogue and TUI helpers for a string literal that is printed or used as a prompt, in
 * the direct form, in a ternary, inside a {@code String.format}, and across a line break. A literal
 * is allowed only when it is not a sentence: an empty string, a single symbol or punctuation mark, a
 * format specifier, an ANSI/OSC sequence, a command name or option, a URL fragment, a JSON field
 * name, a catalogue key, or a value the runtime defines (a state id, a mode, a key). Everything the
 * operator reads comes from {@code messages/*.properties}.
 */
@DisplayName("bilingual text integrity")
class LiteralTextGuardTest {

    /**
     * The helpers that render what the operator reads and are no command of their own: the console,
     * the entry flow, the dialogues and the full-screen layout. Every {@code *Command.java} of the
     * package is guarded by discovery, listed nowhere.
     */
    private static final List<String> HELPER_SOURCES = List.of(
                        "PmConsole.java",
                        "ConsolePrompt.java",
                        "ConsoleNotice.java",
                        "EntryFlow.java",
                        "ConfigAspectRun.java",
                        "SecretReader.java",
                        "TerminalTitle.java",
                        "StartDialogue.java",
                        "StartPrompt.java",
                        "StartRenderer.java",
                        "ChatDialogue.java",
                        "EpicDecompositionDialogue.java",
                        "WorkActionDialogue.java",
                        "HoldDialogue.java",
                        "ReviewDialogue.java",
                        "WorkChatDialogue.java",
                        "PlanSettingsDialogue.java",
                        "OperatorActions.java",
                        "ChoicePrompt.java",
                        "OverviewFullScreenApp.java",
                        "ProjectWorkLevel.java",
                        "PlanCompletionLevel.java",
                        "PlanViewFormatting.java",
                        "TuiLayout.java");

    /** A call that renders text to the operator: its whole argument list is what the guard reads. */
    private static final Pattern RENDER_CALL = Pattern.compile(
                        "(?:(?:out|err|writer|failures|terminal\\.writer\\(\\)|session\\.out\\(\\))\\.(?:print|println|printf|readLine|printAbove)"
                                            + "|prompt\\.(?:raw|askRaw|askInline))\\s*\\(");

    /** A literal; the argument list is scanned for it. */
    private static final Pattern TEXT_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** A call that resolves a catalogue key; only its key argument is no text the operator reads. */
    private static final Pattern CATALOGUE_CALL = Pattern.compile("(?:MessageCatalogue|ErrorTexts)\\.[a-zA-Z]+\\s*\\(");

    /** Literals that carry no words and therefore need no translation. */
    private static final Set<String> ALLOWED = Set.of(
                        "", " ", "\\n", "\\t", "%n", "%s", "%d", "?", "\\u203a ", "\\u00b7", "\\u2013 ",
                        "\\u2713 ", "\\u25c6 ", "\\u25aa ", "\\u001B]0;", "\\u0007", "\\033]52;c;");

    @Test
    @DisplayName("no user-facing string is written in the guarded sources")
    void noLiteralUserFacingText() throws Exception {
        var violations = new ArrayList<String>();
        for (var source : guardedSources()) {
            collect(Files.readString(source, StandardCharsets.UTF_8), source, violations);
        }
        assertTrue(violations.isEmpty(),
                            "user-facing strings must come from messages/*.properties, not from code:\n"
                                                + String.join("\n", violations));
    }

    @Test
    @DisplayName("the guard itself fails on a sentence and passes on a symbol")
    void guardClassifiesLiterals() {
        assertTrue(isUserFacing("Machine Settings:"));
        assertTrue(isUserFacing("No paired devices."));
        assertTrue(isUserFacing("Needs a choice: --consent scan_harnesses"));
        assertFalse(isUserFacing(""));
        assertFalse(isUserFacing("%s"));
        assertFalse(isUserFacing("\\u203a "));
        assertFalse(isUserFacing("1"));
        assertFalse(isUserFacing("\\b \\b"));
        assertFalse(isUserFacing("config"));
        assertFalse(isUserFacing("--consent"));
        assertFalse(isUserFacing("operator.config.needs_choice"), "a catalogue key is no text");
        assertTrue(isUserFacing("{status} No paired devices."), "a brace alone does not make a literal a JSON fragment");
        assertFalse(isUserFacing("{\"running\":false}"), "a JSON fragment is no text");
    }

    @Test
    @DisplayName("the guard fails on a ternary, inside String.format, and across a line break")
    void guardSeesEveryShapeOfLiteral() {
        var violations = new ArrayList<String>();
        var source = Path.of("Sample.java");
        var sample = """
                            class A {
                                void a() {
                                    out.println(stopped ? "Nothing is due." : other);
                                    out.println(String.format("%d workers are running", count));
                                    out.println(stopped
                                                        ? other
                                                        : "Nothing is due either.");
                                }
                            }
                            """;
        collect(sample, source, violations);
        assertEquals(3, violations.size(), violations.toString());
    }

    @Test
    @DisplayName("a catalogue call's parameter value is scanned, its key is not")
    void parameterValuesAreScanned() {
        var violations = new ArrayList<String>();
        var sample = """
                            class A {
                                void a() {
                                    out.println(MessageCatalogue.format(locale, "operator.console.code_failed",
                                                        Map.of("reason", "The runtime is offline.")));
                                }
                            }
                            """;
        collect(sample, Path.of("Sample.java"), violations);
        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).contains("The runtime is offline."), violations.toString());
    }

    private static void collect(String text, Path source, List<String> violations) {
        var matcher = RENDER_CALL.matcher(text);
        while (matcher.find()) {
            var start = matcher.end();
            var end = matchingParen(text, start);
            var arguments = withoutCatalogueKeys(text.substring(start, end));
            var literal = TEXT_LITERAL.matcher(arguments);
            while (literal.find()) {
                var value = literal.group(1);
                if (isUserFacing(value)) {
                    var line = 1 + (int) text.substring(0, start).chars().filter(c -> c == '\n').count()
                                        + (int) arguments.substring(0, literal.start()).chars()
                                        .filter(c -> c == '\n').count();
                    violations.add(source.getFileName() + ":" + line + "  \"" + value + "\"");
                }
            }
            matcher.region(end, text.length());
        }
    }

    /**
     * @param arguments the argument list of a rendering call
     * @return the list with the key literal of every catalogue call blanked out, so a key is never
     *         read as text while the values the call passes are still scanned
     */
    private static String withoutCatalogueKeys(String arguments) {
        var cleaned = new StringBuilder(arguments);
        var matcher = CATALOGUE_CALL.matcher(cleaned);
        while (matcher.find()) {
            var literal = TEXT_LITERAL.matcher(cleaned);
            if (literal.find(matcher.end())) {
                cleaned.replace(literal.start(1), literal.end(1), " ".repeat(literal.group(1).length()));
            }
        }
        return cleaned.toString();
    }

    private static int matchingParen(String text, int from) {
        int depth = 1;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return text.length();
    }

    /**
     * @param literal the literal of a rendering call
     * @return {@code true} when the literal carries words the operator would have to read
     */
    static boolean isUserFacing(String literal) {
        if (ALLOWED.contains(literal)) {
            return false;
        }
        if (literal.startsWith("{}") || literal.startsWith("{\"") || literal.startsWith("{\\")) {
            // A JSON fragment the client writes for a machine consumer, not text for a screen.
            return false;
        }
        if (isKeyHintList(literal)) {
            return false;
        }
        if (literal.matches("[a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)+")) {
            // A catalogue key, not a text.
            return false;
        }
        // Drop the escapes (a control character, a unicode escape, an octal escape) and the format
        // specifiers; what remains is the words the operator would read.
        var words = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c == '\\') {
                i += escapeLength(literal, i);
            } else if (c == '%') {
                i += formatLength(literal, i);
            } else {
                words.append(c);
            }
        }
        boolean hasLetter = false;
        for (int j = 0; j < words.length(); j++) {
            hasLetter = hasLetter || Character.isLetter(words.charAt(j));
        }
        if (!hasLetter) {
            return false;
        }
        var text = words.toString();
        return text.indexOf(' ') >= 0 || text.indexOf('\t') >= 0 || text.indexOf('.') >= 0
                            || text.indexOf('!') >= 0 || text.indexOf('?') >= 0
                            || text.indexOf(':') >= 0 || text.indexOf(';') >= 0;
    }

    /**
     * @param literal the literal of a rendering call
     * @return {@code true} when the literal is a list of one-letter keys a prompt offers, such as
     *         {@code "y, n"} or {@code ", n, "}
     */
    private static boolean isKeyHintList(String literal) {
        var words = literal.split("[,\\s]+");
        var seen = 0;
        for (var word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.length() != 1) {
                return false;
            }
            seen++;
        }
        return seen > 0;
    }

    private static int escapeLength(String literal, int backslash) {
        if (backslash + 1 >= literal.length()) {
            return 0;
        }
        if (literal.charAt(backslash + 1) == 'u') {
            return Math.min(5, literal.length() - backslash - 1);
        }
        return 1;
    }

    private static int formatLength(String literal, int percent) {
        int i = percent + 1;
        while (i < literal.length() && "-#+ 0,(".indexOf(literal.charAt(i)) >= 0) {
            i++;
        }
        while (i < literal.length() && (Character.isDigit(literal.charAt(i)) || literal.charAt(i) == '.')) {
            i++;
        }
        if (i < literal.length() && Character.isLetter(literal.charAt(i))) {
            i++;
        }
        return i - percent - 1;
    }

    /**
     * @return every command of the package, discovered from the sources, and every helper that
     *         renders to the terminal
     * @throws IOException if the sources cannot be read
     */
    private static List<Path> guardedSources() throws IOException {
        var directory = Path.of("src", "main", "java", "de", "planmarshall", "operator");
        if (!Files.isDirectory(directory)) {
            directory = Path.of("pm-operator", "src", "main", "java", "de", "planmarshall", "operator");
        }
        assertTrue(Files.isDirectory(directory), "the operator sources must be found from " + Path.of("").toAbsolutePath());
        List<Path> commands;
        try (Stream<Path> files = Files.list(directory)) {
            commands = files.filter(Files::isRegularFile)
                                .filter(path -> path.getFileName().toString().endsWith("Command.java"))
                                .toList();
        }
        assertFalse(commands.isEmpty(), "no command source found in " + directory);
        List<Path> tree;
        try (Stream<Path> files = Files.walk(directory)) {
            tree = files.filter(Files::isRegularFile).toList();
        }
        var helpers = new ArrayList<Path>();
        for (var name : HELPER_SOURCES) {
            var helper = tree.stream()
                                .filter(path -> path.getFileName().toString().equals(name))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("guarded source not found: " + name));
            helpers.add(helper);
        }
        var all = new ArrayList<>(commands);
        all.addAll(helpers);
        return all;
    }
}
