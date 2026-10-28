/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.dialogue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.StartPrompt;

/**
 * Strict choice prompt for terminal dialogues ({@code CLI-UC-P-02}).
 *
 * <p>Enforces strict selection rules:
 * <ul>
 *   <li>Options can be chosen by key, 1-based number, or range (in multi-choice mode)</li>
 *   <li>Enter takes the preselection only where declared; otherwise it re-asks and never falls through</li>
 *   <li>{@code ?} prints the dialogue explanation and {@code ?n} the explanation of item n, both only
 *       where an explanation was declared; without one the prompt says so and offers no {@code ?}</li>
 *   <li>{@code q} (or German {@code v}) leaves with {@code "Left <state>. Nothing was sent."}</li>
 *   <li>Unknown input re-asks with offered keys; never falls through to a default</li>
 *   <li>Consent and irreversible acts have no Enter default (re-prompts with {@code "Type y or n."})</li>
 *   <li>Options with consequences render in an aligned two-column layout with {@code ←} on preselection</li>
 * </ul>
 */
public final class ChoicePrompt {

    private static final String PRESELECT_ARROW = " \u2190";
    private static final int MAX_ROW = 80;
    private static final int MIN_CONSEQUENCE_COLUMN = 20;

    /**
     * One option in a choice prompt.
     *
     * @param key         the option key or identifier
     * @param label       the human-readable option label
     * @param consequence what happens if this option is chosen (optional)
     * @param explanation the detailed explanation for {@code ?n} (optional)
     * @param preselected whether this option is taken on Enter
     */
    public record Choice(
    String key,
    String label,
    @Nullable String consequence,
    @Nullable String explanation,
    boolean preselected
    ) {
        public Choice {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
        }

        public static Choice of(String key, String label) {
            return new Choice(key, label, null, null, false);
        }
    }

    @FunctionalInterface
    interface LineInput {
        @Nullable
        String readLine(String prompt) throws IOException;
    }

    private final LineInput input;
    private final PrintWriter out;
    private final String locale;
    private final String stateId;
    private final List<Choice> choices = new ArrayList<>();

    private @Nullable String title;
    private @Nullable String explanation;
    private @Nullable String epicTold;
    private boolean noInput;
    private @Nullable String leaveLabel;

    /**
     * Creates a choice prompt over a JLine {@link LineReader}.
     *
     * @param reader  the line reader
     * @param out     the output writer
     * @param locale  the interface locale
     * @param stateId the state identifier for exit messages
     */
    public ChoicePrompt(LineReader reader, PrintWriter out, String locale, String stateId) {
        Objects.requireNonNull(reader, "reader");
        this.input = reader::readLine;
        this.out = Objects.requireNonNull(out, "out");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.stateId = Objects.requireNonNull(stateId, "stateId");
    }

    /**
     * Creates a choice prompt over a {@link BufferedReader} (for headless / stream testing).
     *
     * @param in      the buffered reader
     * @param out     the output writer
     * @param locale  the interface locale
     * @param stateId the state identifier
     */
    public ChoicePrompt(BufferedReader in, PrintWriter out, String locale, String stateId) {
        Objects.requireNonNull(in, "in");
        this.input = prompt -> {
            out.print(prompt);
            out.flush();
            return in.readLine();
        };
        this.out = Objects.requireNonNull(out, "out");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.stateId = Objects.requireNonNull(stateId, "stateId");
    }

    /**
     * Creates a choice prompt over an existing {@link StartPrompt}.
     *
     * @param prompt  the start prompt
     * @param stateId the state identifier
     */
    public ChoicePrompt(StartPrompt prompt, String stateId) {
        this(prompt.reader(), prompt.out(), prompt.locale(), stateId);
        this.noInput = !prompt.asks();
    }

    public ChoicePrompt title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Declares the dialogue explanation behind {@code ?}. Without a dialogue or item explanation the
     * prompt does not offer {@code ?} and answers it by saying that there is nothing to explain.
     *
     * @param explanation the explanation
     * @return this prompt
     */
    public ChoicePrompt explanation(String explanation) {
        this.explanation = explanation;
        return this;
    }

    public ChoicePrompt epicTold(String epicTold) {
        this.epicTold = epicTold;
        return this;
    }

    /**
     * Names the dialogue in the leave message; without it the state identifier is named. A caller
     * that shows the operator a screen passes a human label, never a state identifier.
     *
     * @param leaveLabel the human label of the dialogue
     * @return this prompt
     */
    public ChoicePrompt leaveLabel(String leaveLabel) {
        this.leaveLabel = Objects.requireNonNull(leaveLabel, "leaveLabel");
        return this;
    }

    public ChoicePrompt addChoice(Choice choice) {
        choices.add(Objects.requireNonNull(choice, "choice"));
        return this;
    }

    public ChoicePrompt addChoice(String key, String label) {
        return addChoice(Choice.of(key, label));
    }

    public ChoicePrompt addChoice(String key, String label, @Nullable String consequence,
                        @Nullable String explanation, boolean preselected) {
        return addChoice(new Choice(key, label, consequence, explanation, preselected));
    }

    /**
     * Runs the choice prompt and returns the selected choice.
     *
     * @return the selected choice
     * @throws StartPrompt.StartLeftException    if the operator entered {@code q}/{@code v}
     * @throws StartPrompt.MissingInputException if {@code --no-input} is active
     */
    public Choice ask() {
        if (noInput) {
            var need = choices.stream().filter(Choice::preselected).findFirst()
                                .map(Choice::label).orElse("choice");
            throw new StartPrompt.MissingInputException(stateId, need, null);
        }

        while (true) {
            renderFrame();
            var line = readLine();
            if (line == null) {
                leave();
            }
            line = line.trim();

            if (isQuit(line)) {
                leave();
            }

            if ("?".equals(line)) {
                printExplanation();
                continue;
            }

            if (line.startsWith("?") && line.length() > 1) {
                var target = line.substring(1).trim();
                printItemExplanation(target);
                continue;
            }

            if (line.isEmpty()) {
                var preselected = choices.stream().filter(Choice::preselected).findFirst().orElse(null);
                if (preselected != null) {
                    return echoed(preselected);
                }
                printLine("operator.dialogue.enter_no_default", Map.of());
                continue;
            }

            // Check number selection
            try {
                var num = Integer.parseInt(line);
                if (num >= 1 && num <= choices.size()) {
                    return echoed(choices.get(num - 1));
                }
            } catch (NumberFormatException _) {
                // Not an integer, fall through to key check
            }

            // Check key selection
            for (var choice : choices) {
                if (choice.key().equalsIgnoreCase(line)) {
                    return echoed(choice);
                }
            }

            printNotOffered(line);
        }
    }

    private Choice echoed(Choice choice) {
        out.println(MessageCatalogue.format(locale, "operator.dialogue.answer", Map.of("answer", choice.label())));
        out.flush();
        return choice;
    }

    /**
     * Runs a consent prompt ({@code y/n}, German {@code j/n}).
     *
     * @return {@code true} when consent was granted
     */
    public boolean askConsent() {
        if (noInput) {
            throw new StartPrompt.MissingInputException(stateId, "consent", null);
        }

        while (true) {
            if (title != null) {
                out.println(title);
                out.flush();
            }
            var promptLine = renderConsentPrompt();
            var line = readLineWithPrompt(promptLine);
            if (line == null) {
                leave();
            }
            line = line.trim();

            if (isQuit(line)) {
                leave();
            }

            if ("?".equals(line)) {
                printExplanation();
                continue;
            }

            if (line.isEmpty()) {
                printLine("operator.dialogue.consent.type_yn", Map.of());
                continue;
            }

            if ("y".equalsIgnoreCase(line) || "j".equalsIgnoreCase(line)) {
                return true;
            }
            if ("n".equalsIgnoreCase(line)) {
                return false;
            }

            printNotOffered(line, consentOffered());
        }
    }

    private String consentOffered() {
        var offered = new StringBuilder(isGerman() ? "j" : "y").append(", n, ").append(isGerman() ? "v" : "q");
        if (hasExplanation()) {
            offered.append(", ?");
        }
        return offered.toString();
    }

    /**
     * Runs a multi-selection prompt with toggles and ranges.
     *
     * @return the list of chosen choices
     */
    public List<Choice> askMulti() {
        if (noInput) {
            throw new StartPrompt.MissingInputException(stateId, "multi-selection", null);
        }

        var selected = new LinkedHashSet<Integer>();
        // Initialize with preselected
        for (var i = 0; i < choices.size(); i++) {
            if (choices.get(i).preselected()) {
                selected.add(i);
            }
        }

        while (true) {
            renderMultiFrame(selected);
            var line = readLine();
            if (line == null) {
                leave();
            }
            line = line.trim();

            if (isQuit(line)) {
                leave();
            }

            if ("?".equals(line)) {
                printExplanation();
                continue;
            }

            if (line.isEmpty() || "Enter".equalsIgnoreCase(line)) {
                return selected.stream().map(choices::get).toList();
            }

            // Parse toggle numbers or ranges e.g. "1-2" or "1 3"
            var parts = line.split("[\\s,]+");
            var valid = false;
            for (var part : parts) {
                if (part.contains("-")) {
                    var range = part.split("-");
                    if (range.length == 2) {
                        try {
                            var start = Integer.parseInt(range[0].trim());
                            var end = Integer.parseInt(range[1].trim());
                            for (var k = start; k <= end; k++) {
                                if (k >= 1 && k <= choices.size()) {
                                    toggle(selected, k - 1);
                                    valid = true;
                                }
                            }
                        } catch (NumberFormatException _) {
                            // invalid range
                        }
                    }
                } else {
                    try {
                        var num = Integer.parseInt(part.trim());
                        if (num >= 1 && num <= choices.size()) {
                            toggle(selected, num - 1);
                            valid = true;
                        }
                    } catch (NumberFormatException _) {
                        // try key match
                        for (var i = 0; i < choices.size(); i++) {
                            if (choices.get(i).key().equalsIgnoreCase(part)) {
                                toggle(selected, i);
                                valid = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!valid) {
                printNotOffered(line);
            }
        }
    }

    private static void toggle(Set<Integer> set, int index) {
        if (!set.add(index)) {
            set.remove(index);
        }
    }

    private void renderFrame() {
        if (title != null) {
            out.println(title);
        }

        var hasConsequences = choices.stream().anyMatch(c -> c.consequence() != null);
        var labelWidth = 0;
        if (hasConsequences) {
            for (var i = 0; i < choices.size(); i++) {
                var c = choices.get(i);
                var arrow = c.preselected() ? PRESELECT_ARROW : "";
                var width = ("  " + (i + 1) + ") " + c.label() + arrow).length();
                if (width > labelWidth) {
                    labelWidth = width;
                }
            }
            labelWidth = Math.max(labelWidth + 2, 30);
        }

        for (var i = 0; i < choices.size(); i++) {
            var c = choices.get(i);
            var arrow = c.preselected() ? PRESELECT_ARROW : "";
            var left = "  " + (i + 1) + ") " + c.label() + arrow;

            if (hasConsequences && c.consequence() != null) {
                printTwoColumn(left, c.consequence(), labelWidth);
            } else {
                out.println(left);
            }
        }

        if (epicTold != null) {
            out.println("  " + MessageCatalogue.format(locale, "operator.actions.end.its_epic", Map.of())
                                + "   " + epicTold);
        }
        out.flush();
    }

    /**
     * Draws one option with its consequence in the second column and wraps the consequence into the
     * column rather than past {@link #MAX_ROW}, so a narrow terminal never breaks a word.
     *
     * @param left        the rendered option
     * @param consequence the consequence
     * @param labelWidth  the width of the first column
     */
    private void printTwoColumn(String left, String consequence, int labelWidth) {
        var lines = wrap(consequence, Math.max(MAX_ROW - labelWidth, MIN_CONSEQUENCE_COLUMN));
        out.printf("%-" + labelWidth + "s%s%n", left, lines.getFirst());
        for (var line : lines.subList(1, lines.size())) {
            out.printf("%-" + labelWidth + "s%s%n", "", line);
        }
    }

    private static List<String> wrap(String text, int width) {
        var lines = new ArrayList<String>();
        var current = new StringBuilder();
        for (var word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        lines.add(current.toString());
        return lines;
    }

    private void renderMultiFrame(Set<Integer> selected) {
        if (title != null) {
            out.println(title);
        }
        for (var i = 0; i < choices.size(); i++) {
            var c = choices.get(i);
            var mark = selected.contains(i) ? "[\u2713] " : "[ ] ";
            out.println("  " + (i + 1) + ") " + mark + c.label());
        }
        out.flush();
    }

    private String renderConsentPrompt() {
        var question = MessageCatalogue.format(locale, "operator.dialogue.consent.prompt_yn", Map.of());
        if (hasExplanation()) {
            question = question + "   ?";
        }
        return question + "   \u203a ";
    }

    private @Nullable String readLine() {
        return readLineWithPrompt(StartPrompt.SUFFIX);
    }

    private @Nullable String readLineWithPrompt(String prompt) {
        try {
            return input.readLine(prompt);
        } catch (EndOfFileException | UserInterruptException _) {
            leave();
            return null;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read input: " + e.getMessage(), e);
        }
    }

    private boolean isQuit(String line) {
        return isLeave(locale, line);
    }

    /**
     * @param locale the interface locale
     * @param line   the operator's line
     * @return whether the line leaves the dialogue: {@code q}, and {@code v} in German
     */
    public static boolean isLeave(String locale, String line) {
        if ("q".equalsIgnoreCase(line)) {
            return true;
        }
        return locale.toLowerCase(Locale.ROOT).startsWith("de") && "v".equalsIgnoreCase(line);
    }

    private boolean isGerman() {
        return locale.toLowerCase(Locale.ROOT).startsWith("de");
    }

    private void leave() {
        out.println(MessageCatalogue.format(locale, "operator.dialogue.left",
                            Map.of("state", leaveLabel != null ? leaveLabel : stateId)));
        out.flush();
        throw new StartPrompt.StartLeftException();
    }

    private boolean hasExplanation() {
        return explanation != null || choices.stream().anyMatch(choice -> choice.explanation() != null);
    }

    private void printExplanation() {
        if (explanation != null) {
            out.println("  " + explanation);
        } else {
            printLine("operator.dialogue.no_explanation", Map.of());
        }
        out.flush();
    }

    private void printItemExplanation(String target) {
        Choice matched = null;
        try {
            var num = Integer.parseInt(target);
            if (num >= 1 && num <= choices.size()) {
                matched = choices.get(num - 1);
            }
        } catch (NumberFormatException _) {
            for (var c : choices) {
                if (c.key().equalsIgnoreCase(target)) {
                    matched = c;
                    break;
                }
            }
        }

        if (matched != null && matched.explanation() != null) {
            out.println("  " + matched.explanation());
        } else {
            printLine("operator.dialogue.no_explanation", Map.of());
        }
        out.flush();
    }

    private void printLine(String key, Map<String, ?> args) {
        out.println(MessageCatalogue.format(locale, key, args));
        out.flush();
    }

    private void printNotOffered(String line) {
        printNotOffered(line, formatOffered());
    }

    private void printNotOffered(String line, String offered) {
        out.println(MessageCatalogue.format(locale, "operator.dialogue.not_offered",
                            Map.of("input", line, "offered", offered)));
        out.flush();
    }

    private String formatOffered() {
        var sb = new StringBuilder();
        if (!choices.isEmpty()) {
            if (choices.size() == 1) {
                sb.append("1");
            } else {
                sb.append("1-").append(choices.size());
            }
        }
        var leaveKey = isGerman() ? "v" : "q";
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(leaveKey);
        if (hasExplanation()) {
            sb.append(", ?");
        }
        return sb.toString();
    }
}
