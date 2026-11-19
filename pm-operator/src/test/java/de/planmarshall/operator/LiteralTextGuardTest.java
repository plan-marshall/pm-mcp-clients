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
 * <p>It scans the production sources of the console, the entry, the settings, the devices, the
 * credentials and every command that prints a screen for a string literal that is printed or used
 * as a prompt, in the direct form, in a ternary, and inside a {@code String.format}. A literal is
 * allowed only when it is not a sentence: an empty string, a single symbol or punctuation mark, a
 * format specifier, an ANSI/OSC sequence, a command name or option, a URL fragment, a JSON field
 * name, a catalogue key, or a value the runtime defines (a state id, a mode, a key). Everything the
 * operator reads comes from {@code messages/*.properties}.
 */
@DisplayName("bilingual text integrity")
class LiteralTextGuardTest {

    /** The files that render what the operator reads. */
    private static final List<String> GUARDED = List.of(
                        "PmConsole.java",
                        "ConsolePrompt.java",
                        "ConsoleNotice.java",
                        "EntryFlow.java",
                        "ConfigCommand.java",
                        "WebCommand.java",
                        "CredentialsCommand.java",
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
                        "ProjectCommand.java",
                        "FindCommand.java",
                        "AnswerCommand.java",
                        "CleanupCommand.java",
                        "RuntimeCommand.java",
                        "AspectRunSource.java",
                        "ChoicePrompt.java",
                        "OverviewFullScreenApp.java",
                        "ProjectWorkLevel.java",
                        "PlanCompletionLevel.java",
                        "PlanViewFormatting.java",
                        "TuiLayout.java",
                        "PlanCommand.java",
                        "EpicCommand.java",
                        "StatusCommand.java",
                        "RolesCommand.java",
                        "DecomposeCommand.java",
                        "ConfigCleanupCommand.java",
                        "DrainCommand.java");

    /** A call that renders text to the operator: its argument list is what the guard reads. */
    private static final Pattern RENDER_CALL = Pattern.compile(
                        "(?:(?:out|err|writer|terminal\\.writer\\(\\)|session\\.out\\(\\))\\.(?:print|println|printf|readLine|printAbove)"
                                            + "|prompt\\.(?:raw|askRaw|askInline))\\s*\\(");

    /** A literal; the argument list is scanned for it. */
    private static final Pattern TEXT_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** A call that resolves a catalogue key; its arguments are never text the operator reads. */
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
            var lines = Files.readAllLines(source, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                collect(lines.get(index), source, index, violations);
            }
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
    }

    @Test
    @DisplayName("the guard fails on a literal in a ternary and inside String.format")
    void guardSeesTernariesAndFormats() {
        var violations = new ArrayList<String>();
        collect("        out.println(stopped ? \"Nothing is due.\" : other);", Path.of("Sample.java"), 0,
                            violations);
        collect("        out.println(String.format(\"%d workers are running\", count));", Path.of("Sample.java"), 0,
                            violations);
        assertEquals(2, violations.size(), violations.toString());
    }

    private static void collect(String line, Path source, int index, List<String> violations) {
        var matcher = RENDER_CALL.matcher(line);
        while (matcher.find()) {
            var arguments = withoutCatalogueCalls(line.substring(matcher.end()));
            var literal = TEXT_LITERAL.matcher(arguments);
            while (literal.find()) {
                var text = literal.group(1);
                if (isUserFacing(text)) {
                    violations.add(source.getFileName() + ":" + (index + 1) + "  \"" + text + "\"");
                }
            }
        }
    }

    /**
     * @param arguments the argument list of a rendering call
     * @return the list with every catalogue call and its arguments removed, so that a key is never
     *         read as text
     */
    private static String withoutCatalogueCalls(String arguments) {
        var remaining = arguments;
        var matcher = CATALOGUE_CALL.matcher(remaining);
        while (matcher.find()) {
            var end = matchingParen(remaining, matcher.end());
            remaining = remaining.substring(0, matcher.start()) + remaining.substring(end);
            matcher = CATALOGUE_CALL.matcher(remaining);
        }
        return remaining;
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
                    return i + 1;
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
        if (literal.startsWith("{")) {
            // A JSON fragment the client writes for a machine consumer, not text for a screen.
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

    private static List<Path> guardedSources() throws IOException {
        var directory = Path.of("src", "main", "java", "de", "planmarshall", "operator");
        if (!Files.isDirectory(directory)) {
            directory = Path.of("pm-operator", "src", "main", "java", "de", "planmarshall", "operator");
        }
        assertTrue(Files.isDirectory(directory), "the operator sources must be found from " + Path.of("").toAbsolutePath());
        try (Stream<Path> files = Files.walk(directory)) {
            var found = files.filter(Files::isRegularFile)
                                .filter(path -> GUARDED.contains(path.getFileName().toString()))
                                .toList();
            var names = found.stream().map(path -> path.getFileName().toString()).toList();
            var missing = GUARDED.stream().filter(name -> !names.contains(name)).toList();
            assertTrue(missing.isEmpty(), "guarded sources not found: " + missing);
            return found;
        }
    }
}
