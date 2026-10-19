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
 * <p>It scans the production sources of the console, the entry, the settings, the devices, and the
 * credentials for a string literal that is printed or used as a prompt. A literal is allowed only
 * when it is not a sentence: an empty string, a single symbol or punctuation mark, a format
 * specifier, an ANSI/OSC sequence, a command name or option, a URL fragment, a JSON field name, or
 * a value the runtime defines (a state id, a mode, a key). Everything the operator reads comes
 * from {@code messages/*.properties}.
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
                        "SettingsCommand.java",
                        "WebCommand.java",
                        "CredentialsCommand.java",
                        "SecretReader.java",
                        "TerminalTitle.java",
                        "OnboardCommand.java");

    /** A call that renders text to the operator. */
    private static final Pattern RENDER_CALL = Pattern.compile(
                        "(?:out|err|writer)\\.(?:print|println|printf|readLine|printAbove)\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** A prompt passed to a line reader. */
    private static final Pattern PROMPT_CALL = Pattern.compile(
                        "readLine\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

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
                var line = lines.get(index);
                collect(line, RENDER_CALL, source, index, violations);
                collect(line, PROMPT_CALL, source, index, violations);
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
    }

    private static void collect(String line, Pattern pattern, Path source, int index, List<String> violations) {
        var matcher = pattern.matcher(line);
        while (matcher.find()) {
            var literal = matcher.group(1);
            if (isUserFacing(literal)) {
                violations.add(source.getFileName() + ":" + (index + 1) + "  \"" + literal + "\"");
            }
        }
    }

    /**
     * @param literal the literal of a rendering call
     * @return {@code true} when the literal carries words the operator would have to read
     */
    static boolean isUserFacing(String literal) {
        if (ALLOWED.contains(literal)) {
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
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> GUARDED.contains(path.getFileName().toString())).toList();
        }
    }
}
