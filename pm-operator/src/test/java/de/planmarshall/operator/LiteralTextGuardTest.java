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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.ApiErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import picocli.CommandLine.Command;

/**
 * The bilingual text guard (the plan's "no text is written in the client's code"): a test that
 * fails for a literal user-facing string.
 *
 * <p>It scans every source of the operator tree — discovered from the sources, not listed — for a
 * string literal that reaches the operator: an argument of any {@code print}, {@code println},
 * {@code printf}, {@code printAbove} or {@code readLine} call whatever its receiver, of a prompt, the
 * title of a {@code ChoicePrompt}, a literal a variable holds that such a call prints (assigned
 * before the print, also in a conditional body without braces), what a formatter inserts into the
 * printed text, and the message of {@code new OperatorRefusedException("…")}. It reads the literals
 * as tokens, so a parenthesis or a quote inside a literal does not end a call, an escaped quote does
 * not end a text block, and it skips comments. A literal is allowed only when it carries no word the
 * operator reads: an empty string, a symbol or punctuation mark, a format specifier, an ANSI/OSC
 * sequence, a registered verb name, an option, a URL fragment, a JSON fragment, a catalogue key
 * (blanked only where the key argument of a catalogue call is a literal), a placeholder name of a map
 * passed to a catalogue call, the operand of a comparison (that occurrence only), or a value a
 * declaration defines (a wire value of {@code ApiErrorCode} or a value the catalogue translates). A
 * single word is text like a sentence: a lowercase word stays exempt only by being one of the names
 * above, never by being one word. Everything the operator reads comes from
 * {@code messages/*.properties}, read with the operator's locale.
 *
 * <p>What remains by construction: the protocol spelling of a literal exempts nothing, but a
 * literal that names a declared value is exempt wherever it is printed, since a scan of the source
 * cannot tell a value printed as a value from the same word printed as a label. A literal passed on
 * to a call inside the print (a field name of {@code body.get("title")}, or a constant passed that
 * way) is text only when it is a sentence. A choice prompt is recognised by {@code new ChoicePrompt},
 * a chain on it, or a variable declared as one; a prompt reached through a field of another type is
 * not.
 */
@DisplayName("bilingual text integrity")
class LiteralTextGuardTest {

    /** A call that renders text to the operator: its whole argument list is what the guard reads. */
    private static final Pattern RENDER_CALL = Pattern.compile(
                        "(?:\\.(?:print|println|printf|readLine|printAbove)|\\bprompt\\.raw"
                                            + "|(\\bprompt\\.(?:askRaw|askInline)))\\s*\\(");

    /** A {@code title(…)} call; it is a sink only on a choice prompt, which prints its title. */
    private static final Pattern CHOICE_TITLE = Pattern.compile("\\.title\\s*\\(");

    /** A formatter: the arguments it inserts into its output are read like the printed expression. */
    private static final Pattern FORMATTER = Pattern.compile("\\bString\\.format\\s*\\(|\\.formatted\\s*\\(");

    /** The keywords whose header a body without braces follows. */
    private static final Set<String> CONDITIONAL_KEYWORDS = Set.of("if", "while", "for");

    /** A literal the code compares a value with: {@code "x".equals(…)}, {@code equals("x")} or {@code case "x"}. */
    private static final Pattern COMPARED = Pattern.compile(
                        "\"([^\"\\\\]*)\"\\s*\\.\\s*equals(?:IgnoreCase)?\\s*\\("
                                            + "|\\.equals(?:IgnoreCase)?\\s*\\(\\s*\"([^\"\\\\]*)\"\\s*\\)"
                                            + "|\\bcase\\s+\"([^\"\\\\]*)\"");

    /** The separators of a command line a hint shows: blanks, middle dots, arrows and punctuation. */
    private static final Pattern COMMAND_SEPARATORS = Pattern.compile("[\\s\\u00b7\\u203a·›,;:()<>\\[\\]|]+");

    /** A call that resolves a catalogue key; only a literal key argument is no text the operator reads. */
    private static final Pattern CATALOGUE_CALL = Pattern.compile("\\b(?:MessageCatalogue|ErrorTexts)\\.[a-zA-Z]+\\s*\\(");

    /** A map whose keys are placeholder names of a catalogue call, not text. */
    private static final Pattern PARAMETER_MAP = Pattern.compile("\\bMap\\.(?:of|entry)\\s*\\(");

    /** The message constructor of a refusal: its single argument is what the operator reads. */
    private static final Pattern REFUSAL_MESSAGE = Pattern.compile("\\bnew\\s+OperatorRefusedException\\s*\\(");

    /**
     * An assignment of a {@code String} or {@code var} variable or field, or a reassignment; a
     * literal on its right side is read when the variable is printed within its scope.
     */
    private static final Pattern ASSIGNMENT = Pattern.compile(
                        "(\\b(?:var|String)\\s+)?\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\+?=(?!=)");

    /** A plain name. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** A part of a URL or a path the client sends: no word the operator reads. */
    private static final Pattern URL_FRAGMENT = Pattern.compile("(?:https?://|[/?&#=])\\S*");

    /** An upper-case token the protocol defines: an HTTP method, a header name, an environment variable. */
    private static final Pattern PROTOCOL_TOKEN = Pattern.compile("[A-Z][A-Z0-9_]*|[A-Z][a-z]+(?:-[A-Z][a-z]+)+");

    /** A name in an argument list that is no call and no member. */
    private static final Pattern NAME = Pattern.compile("(?<![.\\w])([A-Za-z_][A-Za-z0-9_]*)\\b(?!\\s*\\()");

    /** A catalogue key. */
    private static final Pattern CATALOGUE_KEY = Pattern.compile("[a-z][a-z0-9_-]*(?:\\.[a-z0-9_-]+)+");

    /** A value the runtime defines: a lowercase identifier joined by an underscore or a dash. */
    private static final Pattern WIRE_VALUE = Pattern.compile("[a-z][a-z0-9]*(?:[_-][a-z0-9]+)+");

    /** Literals that carry no words and therefore need no translation. */
    private static final Set<String> ALLOWED = Set.of(
                        "", " ", "\\n", "\\t", "%n", "%s", "%d", "?", "\\u203a ", "\\u00b7", "\\u2013 ",
                        "\\u2713 ", "\\u25c6 ", "\\u25aa ", "\\u001B]0;", "\\u0007", "\\033]52;c;");

    /** Every verb and subcommand name the command line registers, with its aliases. */
    private static final Set<String> VERB_NAMES = registeredNames();

    /**
     * The values the runtime defines that the catalogue translates: the last segment of every
     * catalogue key ({@code overview.state.running} makes {@code running} a wire value) and the
     * supported locales.
     */
    private static final Set<String> WIRE_VALUES = wireValues();

    /**
     * The values a declaration of the runtime or the protocol defines: the wire values of
     * {@link ApiErrorCode} and the values the catalogue translates ({@link #WIRE_VALUES}).
     */
    private static final Set<String> PROTOCOL_VALUES = protocolValues();

    @Test
    @DisplayName("no user-facing string is written in the operator sources")
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
        assertFalse(isUserFacing("config"), "a registered verb name is no text");
        assertFalse(isUserFacing("--consent"));
        assertFalse(isUserFacing("operator.config.needs_choice"), "a catalogue key is no text");
        assertFalse(isUserFacing("scan_harnesses"), "a wire value is no text");
        assertFalse(isUserFacing("system --by role"), "a command line of verbs and options is no text");
        assertTrue(isUserFacing("Projects"), "a single word is text");
        assertTrue(isUserFacing("browser"), "a lowercase word that is no verb, option, key or wire value is text");
        assertTrue(isUserFacing("{status} No paired devices."), "a brace alone does not make a literal a JSON fragment");
        assertFalse(isUserFacing("{\"running\":false}"), "a JSON fragment is no text");
    }

    @Test
    @DisplayName("the guard fails on a ternary, inside String.format, and across a line break")
    void guardSeesEveryShapeOfLiteral() {
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
        assertEquals(3, scan(sample).size(), scan(sample).toString());
    }

    @Test
    @DisplayName("a catalogue call's parameter value is scanned, its key and placeholder names are not")
    void parameterValuesAreScanned() {
        var sample = """
                            class A {
                                void a() {
                                    out.println(MessageCatalogue.format(locale, "operator.console.code_failed",
                                                        Map.of("reason", "The runtime is offline.")));
                                }
                            }
                            """;
        var violations = scan(sample);
        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).contains("The runtime is offline."), violations.toString());
    }

    @Test
    @DisplayName("a literal inside a comment is not read")
    void commentsAreSkipped() {
        var sample = """
                            class A {
                                void a() {
                                    // out.println("Nothing is due.");
                                    /* out.println("Nothing is due either."); */
                                    out.println(MessageCatalogue.get(locale, "operator.plans.none"));
                                }
                            }
                            """;
        assertEquals(List.of(), scan(sample));
    }

    /** One case per blind spot of the guard before it was widened; each fails against the old guard. */
    @Nested
    @DisplayName("the guard sees what it was built to see")
    class BlindSpots {

        @Test
        @DisplayName("a literal printed through any receiver, termOut among them")
        void everyReceiver() {
            var sample = """
                                class A {
                                    void a() {
                                        termOut.println("Paired devices:");
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"Paired devices:\""), scan(sample));
        }

        @Test
        @DisplayName("a parenthesis inside a literal does not end the call")
        void parenthesisInsideLiteral() {
            var sample = """
                                class A {
                                    void a() {
                                        out.println(count + ") workers are running");
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \") workers are running\""), scan(sample));
        }

        @Test
        @DisplayName("a literal after a catalogue call with a variable key is read")
        void variableKey() {
            var sample = """
                                class A {
                                    void a() {
                                        out.println(MessageCatalogue.get(locale, key) + " is not available.");
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \" is not available.\""), scan(sample));
        }

        @Test
        @DisplayName("a single-word label is text")
        void singleWordLabel() {
            var sample = """
                                class A {
                                    void a() {
                                        out.println("Devices");
                                        out.println("quietly");
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"Devices\"", "Sample.java:4  \"quietly\""), scan(sample));
        }

        @Test
        @DisplayName("a literal a printed variable holds is read")
        void literalInVariable() {
            var sample = """
                                class A {
                                    void a() {
                                        var origin = remote ? name : "This machine";
                                        out.println(origin);
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"This machine\""), scan(sample));
        }

        @Test
        @DisplayName("the message of a refusal is read, the verb of a refused response is not")
        void exceptionMessage() {
            var sample = """
                                class A {
                                    void a() throws IOException {
                                        if (bad) {
                                            throw new OperatorRefusedException("the value does not fit");
                                        }
                                        throw new OperatorRefusedException("plan settings", response);
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:4  \"the value does not fit\""), scan(sample));
        }
    }

    /** One case per precision gap of the scanner a review found; each fails against the scanner before it. */
    @Nested
    @DisplayName("the scanner reads what a print reads, and no more")
    class PrecisionGaps {

        @Test
        @DisplayName("an assignment in a conditional body without braces is read, an annotation attribute is not")
        void unbracedConditionalAssignment() {
            var sample = """
                                class A {
                                    void a() {
                                        String label = translated;
                                        if (offline) label = "Working offline";
                                        out.println(label);
                                    }
                                    @Foo(label = "Annotation text")
                                    void b() {
                                        out.println(label);
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:4  \"Working offline\""), scan(sample));
        }

        @Test
        @DisplayName("what a formatter inserts is read like a printed label, a field name it looks up is not")
        void formatterArgumentsAreText() {
            var sample = """
                                class A {
                                    void a() {
                                        out.println(String.format("%s", "quietly"));
                                        out.println(String.format("%s", body.get("title")));
                                        out.println("%s".formatted("loudly"));
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"quietly\"", "Sample.java:5  \"loudly\""), scan(sample));
        }

        @Test
        @DisplayName("an escaped triple quote does not end a text block, so a print after the block is read")
        void escapedQuotesInTextBlock() {
            var sample = String.join("\n",
                                "class A {",
                                "    void a() {",
                                "        var help = \"\"\"",
                                "            say \\\"\"\" here",
                                "            \"\"\";",
                                "        out.println(\"No paired devices.\");",
                                "    }",
                                "}");
            assertEquals(List.of("Sample.java:6  \"No paired devices.\""), scan(sample));
        }

        @Test
        @DisplayName("a protocol spelling alone is no protocol value; a declared one and a command hint stay exempt")
        void protocolSpellingIsNotExempt() {
            var sample = """
                                class A {
                                    void a() {
                                        out.println("WARNING");
                                        out.println("not-ready");
                                        out.println("internal_fault");
                                        out.println("system --by role");
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"WARNING\"", "Sample.java:4  \"not-ready\""), scan(sample));
        }

        @Test
        @DisplayName("the keys of a map that is no catalogue parameter are read")
        void mapKeysOutsideACatalogueCall() {
            var sample = """
                                class A {
                                    void a() {
                                        out.println(Map.of("No paired devices.", 0).keySet());
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"No paired devices.\""), scan(sample));
        }

        @Test
        @DisplayName("a comparison exempts its own operand, not a print of the same text elsewhere")
        void comparisonExemptsItsOperandOnly() {
            var sample = """
                                class A {
                                    boolean a(String state) {
                                        return "Nothing is due".equals(state);
                                    }
                                    void b() {
                                        out.println("Nothing is due".equals(state) ? first : second);
                                        out.println("Nothing is due");
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:7  \"Nothing is due\""), scan(sample));
        }

        @Test
        @DisplayName("a constant the print passes to a lookup is a name, a constant it prints is a label")
        void constantPassedToALookupIsAName() {
            var sample = """
                                class A {
                                    private static final String FIELD = "title";
                                    private static final String LABEL = "Devices";
                                    void a() {
                                        out.println(body.get(FIELD));
                                        out.println(LABEL);
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:3  \"Devices\""), scan(sample));
        }

        @Test
        @DisplayName("a print reads the value assigned before it, not one assigned after")
        void assignmentAfterThePrintIsNotRead() {
            var sample = """
                                class A {
                                    void a() {
                                        String label = translated;
                                        out.println(label);
                                        label = "Internal log message";
                                        log.debug(label);
                                    }
                                }
                                """;
            assertEquals(List.of(), scan(sample));
        }

        @Test
        @DisplayName("the title of a choice prompt is read, the title of Display or ErrorTexts is not")
        void choicePromptTitleIsASink() {
            var sample = """
                                class A {
                                    void a() {
                                        var choice = new ChoicePrompt(prompt, "runtime stop")
                                                            .title("Stop the runtime?");
                                        var other = new ChoicePrompt(prompt, "runtime stop");
                                        other.title("Stop it now?");
                                        var heading = Display.title("Shown elsewhere");
                                        var error = ErrorTexts.title(locale, code);
                                    }
                                }
                                """;
            assertEquals(List.of("Sample.java:4  \"Stop the runtime?\"", "Sample.java:6  \"Stop it now?\""),
                                scan(sample));
        }

        @Test
        @DisplayName("the sentence test reads as the expressions it replaced did")
        void sentenceShapeIsPinned() {
            assertAll(
                                () -> assertTrue(readsAsSentence("No paired devices.")),
                                () -> assertTrue(readsAsSentence("ab c"), "a word, a blank, a letter"),
                                () -> assertTrue(readsAsSentence("a bc"), "a letter, a blank, a word"),
                                () -> assertTrue(readsAsSentence("ab."), "a word before a full stop"),
                                () -> assertTrue(readsAsSentence("ab\nc"), "the first shape reads across a line break"),
                                () -> assertTrue(readsAsSentence("a\nbc"), "a line break inside the blank"),
                                () -> assertTrue(readsAsSentence("ab\n."), "a line break before the full stop"),
                                () -> assertFalse(readsAsSentence("abc"), "one word"),
                                () -> assertFalse(readsAsSentence("a b"), "two letters"),
                                () -> assertFalse(readsAsSentence("x."), "one letter before a full stop"),
                                () -> assertFalse(readsAsSentence("a bc"), "a line separator is no blank"),
                                () -> assertFalse(readsAsSentence("x ab."), "the full stop shape reads one line"));
        }
    }

    private static List<String> scan(String sample) {
        var violations = new ArrayList<String>();
        collect(sample, Path.of("Sample.java"), violations);
        return violations;
    }

    private static void collect(String raw, Path source, List<String> violations) {
        var code = Code.of(raw);
        var assignments = code.assignments();
        // literal index -> whether it stands directly in the printed expression (not inside a call)
        var found = new TreeMap<Integer, Boolean>();
        var sinks = new ArrayList<int[]>();
        var matcher = RENDER_CALL.matcher(code.text());
        while (matcher.find()) {
            if (code.inLiteral(matcher.start())) {
                continue;
            }
            var start = matcher.end();
            var end = code.matchingParen(start);
            if (matcher.group(1) != null) {
                // A question: only its text is read; the state id and the field name that follow are not.
                var arguments = code.arguments(start, end);
                end = arguments.isEmpty() ? end : arguments.get(0)[1];
            }
            sinks.add(new int[]{start, end});
        }
        sinks.addAll(code.choiceTitles());
        for (var sink : sinks) {
            var start = sink[0];
            var end = sink[1];
            code.read(start, end, found);
            for (var name : code.namesIn(start, end).entrySet()) {
                for (var assignment : assignments.getOrDefault(name.getKey(), List.of())) {
                    // The print reads the value assigned before it, never one assigned after.
                    if (assignment.scopeStart() <= start && start <= assignment.scopeEnd()
                                        && assignment.start() <= start) {
                        code.read(assignment.start(), assignment.end(), found, name.getValue());
                    }
                }
            }
        }
        var refusal = REFUSAL_MESSAGE.matcher(code.text());
        while (refusal.find()) {
            if (code.inLiteral(refusal.start())) {
                continue;
            }
            var start = refusal.end();
            var arguments = code.arguments(start, code.matchingParen(start));
            if (arguments.size() == 1) {
                code.read(arguments.get(0)[0], arguments.get(0)[1], found);
            }
        }
        var compared = code.comparedLiterals();
        found.forEach((index, direct) -> {
            var literal = code.literals().get(index);
            var value = code.masked().substring(literal.start(), literal.end());
            if (compared.contains(literal.start())) {
                // The operand of a comparison with a runtime field: a wire value, not a label.
                return;
            }
            if (direct ? isUserFacing(value) : isSentence(value)) {
                var line = 1 + (int) raw.substring(0, literal.start()).chars().filter(c -> c == '\n').count();
                violations.add(source.getFileName() + ":" + line + "  \"" + value + "\"");
            }
        });
    }

    /**
     * A string literal of a source.
     *
     * @param start the first character of its content
     * @param end   the end of its content
     * @param outer the opening quote
     * @param after the character after the closing quote
     */
    private record Literal(int start, int end, int outer, int after) {
    }

    /**
     * The right side of an assignment and the part of the source in which its variable is the
     * assigned one.
     *
     * @param start      the start of the right side
     * @param end        its end
     * @param scopeStart the declaration of the variable
     * @param scopeEnd   the end of the block that declares it
     */
    private record Assignment(int start, int end, int scopeStart, int scopeEnd) {
    }

    /**
     * A source read as tokens.
     *
     * @param text      the source with its comments blanked
     * @param masked    the source with its comments, catalogue keys and placeholder names blanked
     * @param literals  the string literals, in source order
     * @param literal   for each character, whether it belongs to a string or a character literal
     */
    private record Code(String text, String masked, List<Literal> literals, boolean[] literal) {

        static Code of(String raw) {
            var text = new StringBuilder(raw);
            var literals = new ArrayList<Literal>();
            var inLiteral = new boolean[raw.length()];
            int n = raw.length();
            int i = 0;
            while (i < n) {
                char c = raw.charAt(i);
                if (c == '/' && i + 1 < n && raw.charAt(i + 1) == '/') {
                    int end = raw.indexOf('\n', i);
                    end = end < 0 ? n : end;
                    blank(text, i, end);
                    i = end;
                } else if (c == '/' && i + 1 < n && raw.charAt(i + 1) == '*') {
                    int end = raw.indexOf("*/", i + 2);
                    end = end < 0 ? n : end + 2;
                    blank(text, i, end);
                    i = end;
                } else if (raw.startsWith("\"\"\"", i)) {
                    int end = textBlockEnd(raw, i + 3);
                    int after = Math.min(n, end + 3);
                    literals.add(new Literal(i + 3, end, i, after));
                    mark(inLiteral, i, after);
                    i = after;
                } else if (c == '"' || c == '\'') {
                    int j = i + 1;
                    while (j < n && raw.charAt(j) != c && raw.charAt(j) != '\n') {
                        j += raw.charAt(j) == '\\' ? 2 : 1;
                    }
                    int end = Math.min(j, n);
                    int after = Math.min(n, end + 1);
                    if (c == '"') {
                        literals.add(new Literal(i + 1, end, i, after));
                    }
                    mark(inLiteral, i, after);
                    i = after;
                } else {
                    i++;
                }
            }
            var code = new Code(text.toString(), text.toString(), List.copyOf(literals), inLiteral);
            return new Code(code.text(), code.maskKeys(), code.literals(), inLiteral);
        }

        /**
         * @param raw  the source
         * @param from the first character of a text block's content
         * @return the start of the block's closing delimiter: the next three quotes no backslash
         *         escapes, or the end of the source
         */
        private static int textBlockEnd(String raw, int from) {
            int j = from;
            while (j < raw.length()) {
                if (raw.charAt(j) == '\\') {
                    j += 2;
                } else if (raw.startsWith("\"\"\"", j)) {
                    return j;
                } else {
                    j++;
                }
            }
            return raw.length();
        }

        private static void blank(StringBuilder text, int from, int to) {
            for (int k = from; k < to; k++) {
                if (text.charAt(k) != '\n') {
                    text.setCharAt(k, ' ');
                }
            }
        }

        private static void mark(boolean[] marks, int from, int to) {
            for (int k = from; k < to; k++) {
                marks[k] = true;
            }
        }

        boolean inLiteral(int index) {
            return literal[index];
        }

        /**
         * Reads the literals of a printed range: each with whether it stands directly in the printed
         * expression, and every literal a formatter inserts into its output as direct as well, so
         * {@code String.format("%s", "quietly")} is a label like {@code "quietly"}, while an argument
         * nested deeper, such as the field name of {@code body.get("title")}, stays a name.
         *
         * @param from  the start of the range
         * @param to    its end
         * @param found the literals read so far, by index, with whether one of their reads is direct
         */
        void read(int from, int to, Map<Integer, Boolean> found) {
            read(from, to, found, true);
        }

        /**
         * @param from     the start of the range
         * @param to       its end
         * @param found    the literals read so far
         * @param printed  whether the range is printed as it stands; {@code false} for the value of a
         *                 variable the print passes on to a call, whose literals are names unless
         *                 they are sentences
         */
        void read(int from, int to, Map<Integer, Boolean> found, boolean printed) {
            literalsIn(from, to).forEach((index, direct) -> found.merge(index, printed && direct, Boolean::logicalOr));
            for (var argument : formatterArguments(from, to)) {
                literalsIn(argument[0], argument[1]).forEach((index, direct) -> {
                    if (printed && direct) {
                        found.put(index, true);
                    }
                });
            }
        }

        /**
         * @param from the start of a range
         * @param to   its end
         * @return the top-level argument ranges of every formatter call in the range
         */
        private List<int[]> formatterArguments(int from, int to) {
            var ranges = new ArrayList<int[]>();
            var formatter = FORMATTER.matcher(text);
            formatter.region(from, to);
            while (formatter.find()) {
                if (!literal[formatter.start()]) {
                    var open = formatter.end();
                    ranges.addAll(arguments(open, Math.min(matchingParen(open), to)));
                }
            }
            return ranges;
        }

        /**
         * @param from the start of a range
         * @param at   a position in it
         * @return the bracket depth of the position relative to the range's start, literals not counted
         */
        private int depth(int from, int at) {
            int depth = 0;
            for (int i = from; i < at; i++) {
                if (literal[i]) {
                    continue;
                }
                char c = text.charAt(i);
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                }
            }
            return depth;
        }

        /**
         * @return the argument ranges of every {@code title(…)} call on a {@code ChoicePrompt},
         *         which prints its title; a {@code title} of another receiver, such as
         *         {@code Display.title} or {@code ErrorTexts.title}, is no sink
         */
        List<int[]> choiceTitles() {
            var ranges = new ArrayList<int[]>();
            var matcher = CHOICE_TITLE.matcher(text);
            while (matcher.find()) {
                if (!literal[matcher.start()] && isChoicePrompt(matcher.start())) {
                    ranges.add(new int[]{matcher.end(), matchingParen(matcher.end())});
                }
            }
            return ranges;
        }

        /**
         * @param dot the dot before a method name
         * @return {@code true} when the receiver is a choice prompt: a {@code new ChoicePrompt(…)}, a
         *         call chained on one, or a variable declared as one
         */
        private boolean isChoicePrompt(int dot) {
            int k = blanksBack(dot - 1);
            if (k < 0) {
                return false;
            }
            if (text.charAt(k) == ')') {
                int open = matchingOpen(k);
                if (open < 0) {
                    return false;
                }
                int nameEnd = blanksBack(open - 1) + 1;
                int nameStart = identifierStart(nameEnd);
                int before = blanksBack(nameStart - 1);
                if ("ChoicePrompt".equals(text.substring(nameStart, nameEnd))) {
                    return before >= 2 && text.startsWith("new", before - 2);
                }
                // A call in a chain: the chain's receiver is the receiver.
                return before >= 0 && text.charAt(before) == '.' && isChoicePrompt(before);
            }
            int start = identifierStart(k + 1);
            if (start == k + 1) {
                return false;
            }
            var name = Pattern.quote(text.substring(start, k + 1));
            return Pattern.compile("\\bChoicePrompt\\s+" + name + "\\b|\\bvar\\s+" + name
                                + "\\s*=\\s*new\\s+ChoicePrompt\\b").matcher(text).find();
        }

        /**
         * @param from a position in the source
         * @return the last position at or before it that is no blank, or {@code -1}
         */
        private int blanksBack(int from) {
            int k = from;
            while (k >= 0 && Character.isWhitespace(text.charAt(k))) {
                k--;
            }
            return k;
        }

        /**
         * @param end the end of a name
         * @return the start of the name that ends there
         */
        private int identifierStart(int end) {
            int start = end;
            while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
                start--;
            }
            return start;
        }

        /**
         * @param close a closing parenthesis
         * @return the opening parenthesis that matches it, literals not counted, or {@code -1}
         */
        private int matchingOpen(int close) {
            int depth = 0;
            for (int i = close; i >= 0; i--) {
                if (literal[i]) {
                    continue;
                }
                char c = text.charAt(i);
                if (c == ')') {
                    depth++;
                } else if (c == '(') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
            return -1;
        }

        /**
         * @return the content starts of the literals that are an operand of a comparison: the
         *         exemption belongs to that occurrence, never to another literal of the same text
         */
        Set<Integer> comparedLiterals() {
            var operands = new HashSet<Integer>();
            var matcher = COMPARED.matcher(text);
            while (matcher.find()) {
                for (int group = 1; group <= 3; group++) {
                    if (matcher.group(group) != null) {
                        operands.add(matcher.start(group));
                    }
                }
            }
            return operands;
        }

        /**
         * @return the source with the key literal of every catalogue call and the placeholder names
         *         of every parameter map passed to a catalogue call blanked, so neither is read as text
         *         while the values are; a map anywhere else keeps its keys
         */
        private String maskKeys() {
            var cleaned = new StringBuilder(text);
            var catalogueArguments = new ArrayList<int[]>();
            var catalogue = CATALOGUE_CALL.matcher(text);
            while (catalogue.find()) {
                if (inLiteral(catalogue.start())) {
                    continue;
                }
                var close = matchingParen(catalogue.end());
                catalogueArguments.add(new int[]{catalogue.end(), close});
                var arguments = arguments(catalogue.end(), close);
                var key = arguments.isEmpty() ? null : pureLiteral(arguments.get(0));
                if (key == null && arguments.size() > 1) {
                    var second = pureLiteral(arguments.get(1));
                    if (second != null && CATALOGUE_KEY.matcher(text.substring(second.start(), second.end())).matches()) {
                        key = second;
                    }
                }
                if (key != null) {
                    blank(cleaned, key.start(), key.end());
                }
            }
            var parameters = PARAMETER_MAP.matcher(text);
            while (parameters.find()) {
                var at = parameters.start();
                if (inLiteral(at) || catalogueArguments.stream().noneMatch(r -> r[0] <= at && at < r[1])) {
                    continue;
                }
                var arguments = arguments(parameters.end(), matchingParen(parameters.end()));
                for (int a = 0; a < arguments.size(); a += 2) {
                    var range = arguments.get(a);
                    var name = pureLiteral(range);
                    if (name != null) {
                        blank(cleaned, name.start(), name.end());
                    } else if (IDENTIFIER.matcher(text.substring(range[0], range[1])).matches()) {
                        // A constant that names a placeholder.
                        blank(cleaned, range[0], range[1]);
                    }
                }
            }
            return cleaned.toString();
        }

        /**
         * @param from the first character after an opening parenthesis
         * @return the index of the matching closing parenthesis, literals not counted
         */
        int matchingParen(int from) {
            int depth = 1;
            for (int i = from; i < text.length(); i++) {
                if (literal[i]) {
                    continue;
                }
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
         * @param from the start of an argument list
         * @param to   its closing parenthesis
         * @return the ranges of its top-level arguments, without surrounding blanks
         */
        List<int[]> arguments(int from, int to) {
            var ranges = new ArrayList<int[]>();
            int depth = 0;
            int begin = from;
            for (int i = from; i <= to; i++) {
                if (i < to && literal[i]) {
                    continue;
                }
                char c = i < to ? text.charAt(i) : ',';
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    var range = trim(begin, i);
                    if (range[0] < range[1]) {
                        ranges.add(range);
                    }
                    begin = i + 1;
                }
            }
            return ranges;
        }

        private int[] trim(int from, int to) {
            int a = from;
            int b = to;
            while (a < b && Character.isWhitespace(text.charAt(a))) {
                a++;
            }
            while (b > a && Character.isWhitespace(text.charAt(b - 1))) {
                b--;
            }
            return new int[]{a, b};
        }

        /**
         * @param range an argument range
         * @return the literal the argument consists of, or {@code null} when it is more than a literal
         */
        private Literal pureLiteral(int[] range) {
            for (var candidate : literals) {
                if (candidate.outer() == range[0] && candidate.after() == range[1]) {
                    return candidate;
                }
            }
            return null;
        }

        /**
         * @param from the start of a range
         * @param to   its end
         * @return the indexes of the literals inside the range
         */
        Map<Integer, Boolean> literalsIn(int from, int to) {
            var inside = new LinkedHashMap<Integer, Boolean>();
            int depth = 0;
            int position = from;
            for (int k = 0; k < literals.size(); k++) {
                var candidate = literals.get(k);
                if (candidate.outer() < from || candidate.after() > to) {
                    continue;
                }
                for (; position < candidate.outer(); position++) {
                    if (literal[position]) {
                        continue;
                    }
                    char c = text.charAt(position);
                    if (c == '(' || c == '[' || c == '{') {
                        depth++;
                    } else if (c == ')' || c == ']' || c == '}') {
                        depth--;
                    }
                }
                inside.put(k, depth <= 0);
            }
            return inside;
        }

        /**
         * @param from the start of a range
         * @param to   its end
         * @return the plain names used in the range outside its literals, each with whether it is
         *         printed as it stands (in the range itself or in what a formatter inserts) rather
         *         than passed to a call, such as the field name of {@code map.get(FIELD)}
         */
        Map<String, Boolean> namesIn(int from, int to) {
            var names = new HashMap<String, Boolean>();
            var printedRanges = new ArrayList<int[]>();
            printedRanges.add(new int[]{from, to});
            printedRanges.addAll(formatterArguments(from, to));
            var matcher = NAME.matcher(masked);
            matcher.region(from, to);
            while (matcher.find()) {
                var at = matcher.start();
                if (!literal[at]) {
                    var printed = printedRanges.stream().anyMatch(r -> r[0] <= at && at < r[1] && depth(r[0], at) <= 0);
                    names.merge(matcher.group(1), printed, Boolean::logicalOr);
                }
            }
            return names;
        }

        /**
         * @return for every assigned name, the ranges of the right sides that hold a literal
         */
        Map<String, List<Assignment>> assignments() {
            var result = new HashMap<String, List<Assignment>>();
            var matcher = ASSIGNMENT.matcher(text);
            while (matcher.find()) {
                if (literal[matcher.start()]) {
                    continue;
                }
                var name = matcher.group(2);
                int scopeStart;
                if (matcher.group(1) != null) {
                    scopeStart = matcher.start();
                } else if (startsStatement(matcher.start(2))) {
                    scopeStart = declarationOf(name, matcher.start(2));
                } else {
                    // An annotation attribute or a named argument, no variable.
                    continue;
                }
                var start = matcher.end();
                var end = statementEnd(start);
                if (!literalsIn(start, end).isEmpty()) {
                    result.computeIfAbsent(name, _ -> new ArrayList<>())
                                        .add(new Assignment(start, end, scopeStart, blockEnd(scopeStart)));
                }
            }
            return result;
        }

        /**
         * @param nameStart the start of an assigned name without a declared type
         * @return {@code true} when the name begins a statement, so it is a reassignment
         */
        private boolean startsStatement(int nameStart) {
            int k = nameStart - 1;
            while (k >= 0 && Character.isWhitespace(text.charAt(k))) {
                k--;
            }
            if (k < 0 || ";{}".indexOf(text.charAt(k)) >= 0) {
                return true;
            }
            if (text.charAt(k) == '>' && k > 0 && text.charAt(k - 1) == '-') {
                return true;
            }
            if (text.charAt(k) == ')') {
                // The body of a conditional without braces: if (offline) label = "…";
                int open = matchingOpen(k);
                if (open < 0) {
                    return false;
                }
                int keywordEnd = blanksBack(open - 1) + 1;
                return CONDITIONAL_KEYWORDS.contains(text.substring(identifierStart(keywordEnd), keywordEnd));
            }
            return k >= 3 && text.startsWith("else", k - 3);
        }

        /**
         * @param name   an assigned name
         * @param before the position of its reassignment
         * @return the position of the nearest declaration of the name before it, or the start of the
         *         source when it is declared nowhere before
         */
        private int declarationOf(String name, int before) {
            var declaration = Pattern.compile("\\b(?:var|String)\\s+" + Pattern.quote(name) + "\\b").matcher(text);
            int found = 0;
            while (declaration.find() && declaration.start() < before) {
                if (!literal[declaration.start()]) {
                    found = declaration.start();
                }
            }
            return found;
        }

        /**
         * @param position a position in the source
         * @return the end of the innermost block that holds the position
         */
        private int blockEnd(int position) {
            int depth = 0;
            for (int i = position; i < text.length(); i++) {
                if (literal[i]) {
                    continue;
                }
                char c = text.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth < 0) {
                        return i;
                    }
                }
            }
            return text.length();
        }

        /**
         * @param from the start of a right side
         * @return the end of the expression: a {@code ;} or a {@code ,} at its own depth, or the
         *         parenthesis that closes the list it sits in
         */
        private int statementEnd(int from) {
            int depth = 0;
            for (int i = from; i < text.length(); i++) {
                if (literal[i]) {
                    continue;
                }
                char c = text.charAt(i);
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                    if (depth < 0) {
                        return i;
                    }
                } else if ((c == ';' || c == ',') && depth == 0) {
                    return i;
                }
            }
            return text.length();
        }
    }

    /**
     * @param literal the literal of a rendering call
     * @return {@code true} when the literal carries words the operator would have to read
     */
    static boolean isUserFacing(String literal) {
        if (isNoText(literal)) {
            return false;
        }
        var words = words(literal);
        if (words.indexOf(' ') < 0 && WIRE_VALUES.contains(words.strip())) {
            return false;
        }
        // A word of two letters or more is read, alone like in a sentence.
        return hasWord(words);
    }

    /**
     * The test for a literal that is an argument of a call inside the printed expression, such as a
     * field name of {@code body.get("title")}: it is text only when it is a sentence, so a single
     * word there is a name the code passes, not a label.
     *
     * @param literal a literal nested in a call of a rendering call
     * @return {@code true} when the literal is a sentence the operator would read
     */
    static boolean isSentence(String literal) {
        if (!isUserFacing(literal)) {
            return false;
        }
        return readsAsSentence(words(literal).strip());
    }

    /**
     * The shape of a sentence, read in one pass instead of by a backtracking expression: a word of
     * two letters with a blank and a letter after it (across line breaks), a letter with a blank and
     * a word of two letters after it, or a word of two letters that ends the text before a closing
     * {@code .}, {@code !} or {@code ?}. The last two read on one line only, apart from the blank
     * between their parts, as the expressions they replace did. A blank is {@code [ \t\n\x0B\f\r]}.
     *
     * @param text the words of a literal
     * @return {@code true} when the words read as a sentence
     */
    static boolean readsAsSentence(String text) {
        return wordThenLetter(text) || letterThenWord(text) || wordThenStop(text);
    }

    /** A word of two letters, a blank after it and a letter after the blank; line breaks are read. */
    private static boolean wordThenLetter(String text) {
        int pair = pairStart(text, 0);
        if (pair < 0) {
            return false;
        }
        int blank = pair + 2;
        while (blank < text.length() && !isBlank(text.charAt(blank))) {
            blank++;
        }
        return letterFrom(text, blank + 1) >= 0;
    }

    /**
     * A letter, a run of blanks after it and a word of two letters after the run, everything but the
     * run on one line: every line break must stand in the one run of blanks.
     */
    private static boolean letterThenWord(String text) {
        int first = -1;
        int last = -1;
        for (int i = 0; i < text.length(); i++) {
            if (isLineBreak(text.charAt(i))) {
                first = first < 0 ? i : first;
                last = i;
            }
        }
        if (first < 0) {
            int letter = letterFrom(text, 0);
            if (letter < 0) {
                return false;
            }
            int blank = letter + 1;
            while (blank < text.length() && !isBlank(text.charAt(blank))) {
                blank++;
            }
            return blank < text.length() && pairStart(text, blank + 1) >= 0;
        }
        for (int i = first; i <= last; i++) {
            if (!isBlank(text.charAt(i))) {
                return false;
            }
        }
        int letter = letterFrom(text, 0);
        return letter >= 0 && letter < first && pairStart(text, last + 1) >= 0;
    }

    /** A word of two letters, blanks, then a closing {@code .}, {@code !} or {@code ?} that ends the text, on one line. */
    private static boolean wordThenStop(String text) {
        int end = text.length() - 1;
        if (end < 0 || ".!?".indexOf(text.charAt(end)) < 0) {
            return false;
        }
        int k = end - 1;
        while (k >= 0 && isBlank(text.charAt(k))) {
            k--;
        }
        if (k < 1 || !Character.isLetter(text.charAt(k)) || !Character.isLetter(text.charAt(k - 1))) {
            return false;
        }
        for (int i = 0; i < k - 1; i++) {
            if (isLineBreak(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** @return the start of the first two letters in a row at or after {@code from}, or {@code -1} */
    private static int pairStart(String text, int from) {
        for (int i = Math.max(0, from); i + 1 < text.length(); i++) {
            if (Character.isLetter(text.charAt(i)) && Character.isLetter(text.charAt(i + 1))) {
                return i;
            }
        }
        return -1;
    }

    /** @return the first letter at or after {@code from}, or {@code -1} */
    private static int letterFrom(String text, int from) {
        for (int i = Math.max(0, from); i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /** @return whether the character is a blank of a regular expression's {@code \s} */
    private static boolean isBlank(char c) {
        return " \t\n\u000B\f\r".indexOf(c) >= 0;
    }

    /** @return whether the character ends a line for a regular expression's {@code .} */
    private static boolean isLineBreak(char c) {
        return "\n\r\u0085  ".indexOf(c) >= 0;
    }

    /**
     * @param literal a literal
     * @return {@code true} when the literal is one of the forms that carry no word at all
     */
    private static boolean isNoText(String literal) {
        if (ALLOWED.contains(literal)) {
            return true;
        }
        if (literal.startsWith("{}") || literal.startsWith("{\"") || literal.startsWith("{\\")) {
            // A JSON fragment the client writes for a machine consumer, not text for a screen.
            return true;
        }
        if (isKeyHintList(literal)) {
            return true;
        }
        if (CATALOGUE_KEY.matcher(literal).matches() || literal.matches("[a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)*\\.")) {
            // A catalogue key, or the prefix a key is built from, not a text.
            return true;
        }
        if (URL_FRAGMENT.matcher(literal).matches()) {
            // A part of a URL or a path the client sends.
            return true;
        }
        if ((WIRE_VALUE.matcher(literal).matches() || PROTOCOL_TOKEN.matcher(literal).matches())
                            && PROTOCOL_VALUES.contains(literal)) {
            // A value a declaration of the runtime or the protocol defines; the spelling alone
            // exempts nothing, so "WARNING" or "not-ready" printed as such is text.
            return true;
        }
        // An option, a verb name, or a command line made of them (a hint such as "system --by role").
        return isCommandLine(literal);
    }

    /**
     * @param literal a literal
     * @return {@code true} when every word of the literal is a registered verb name, an option or the
     *         value right after an option, so it shows a command the operator types, not a text
     */
    private static boolean isCommandLine(String literal) {
        var tokens = COMMAND_SEPARATORS.split(words(literal).strip());
        var seen = 0;
        var afterOption = false;
        for (var token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.matches("--?[a-z][a-z0-9-]*=?")) {
                afterOption = true;
            } else if (VERB_NAMES.contains(token) || afterOption && token.matches("[a-z0-9][a-z0-9_-]*")) {
                afterOption = false;
            } else {
                return false;
            }
            seen++;
        }
        return seen > 0;
    }

    /**
     * @param literal a literal
     * @return the literal without its escapes (a control character, a unicode escape, an octal
     *         escape) and its format specifiers: the words the operator would read
     */
    private static String words(String literal) {
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
        return words.toString();
    }

    /**
     * @param words the words of a literal
     * @return {@code true} when a run of two letters or more stands in it; a lone letter is the rest
     *         of a format specifier split across a concatenation or a key a prompt offers
     */
    private static boolean hasWord(String words) {
        int run = 0;
        for (int i = 0; i < words.length(); i++) {
            run = Character.isLetter(words.charAt(i)) ? run + 1 : 0;
            if (run >= 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the last segment of every catalogue key and the supported locales
     */
    private static Set<String> wireValues() {
        var values = new HashSet<String>();
        for (var key : MessageCatalogue.getMessages("en", "all").keySet()) {
            values.add(key.substring(key.lastIndexOf('.') + 1));
        }
        for (var locale : (List<?>) MessageCatalogue.getAvailableLocalesAndAreas().get("locales")) {
            values.add(String.valueOf(locale));
        }
        return Set.copyOf(values);
    }

    /**
     * @return the wire values of the error codes and the values the catalogue translates
     */
    private static Set<String> protocolValues() {
        var values = new HashSet<String>(ApiErrorCode.wireValues());
        values.addAll(WIRE_VALUES);
        return Set.copyOf(values);
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
     * @return every verb and subcommand name of the command line, with its aliases
     */
    private static Set<String> registeredNames() {
        var names = new HashSet<String>();
        addNames(PmOperator.class, names, new HashSet<>());
        return Set.copyOf(names);
    }

    private static void addNames(Class<?> type, Set<String> names, Set<Class<?>> seen) {
        if (!seen.add(type)) {
            return;
        }
        var command = type.getAnnotation(Command.class);
        if (command == null) {
            return;
        }
        for (var subcommand : command.subcommands()) {
            var annotation = subcommand.getAnnotation(Command.class);
            if (annotation != null) {
                names.add(annotation.name());
                names.addAll(List.of(annotation.aliases()));
            }
            addNames(subcommand, names, seen);
        }
        for (var method : type.getDeclaredMethods()) {
            var annotation = method.getAnnotation(Command.class);
            if (annotation != null) {
                names.add(annotation.name());
                names.addAll(List.of(annotation.aliases()));
            }
        }
    }

    /**
     * @return every Java source of the operator tree, discovered, listed nowhere
     * @throws IOException if the sources cannot be read
     */
    private static List<Path> guardedSources() throws IOException {
        var directory = Path.of("src", "main", "java", "de", "planmarshall", "operator");
        if (!Files.isDirectory(directory)) {
            directory = Path.of("pm-operator", "src", "main", "java", "de", "planmarshall", "operator");
        }
        assertTrue(Files.isDirectory(directory), "the operator sources must be found from " + Path.of("").toAbsolutePath());
        List<Path> sources;
        try (Stream<Path> files = Files.walk(directory)) {
            sources = files.filter(Files::isRegularFile)
                                .filter(path -> path.getFileName().toString().endsWith(".java"))
                                .filter(path -> !"package-info.java".equals(path.getFileName().toString()))
                                .sorted()
                                .toList();
        }
        assertFalse(sources.stream().noneMatch(path -> path.getFileName().toString().endsWith("Command.java")),
                            "no command source found in " + directory);
        return sources;
    }
}
