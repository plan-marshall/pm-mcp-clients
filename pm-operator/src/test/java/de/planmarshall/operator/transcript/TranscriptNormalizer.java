/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * Removes what a transcript must not pin from a rendered terminal buffer: ANSI and OSC escapes,
 * clock times and instants, process ids, and the throw-away machine root a fixture test uses.
 * Everything a reader would compare, the texts, the layout, and the order, stays untouched.
 */
public final class TranscriptNormalizer {

    /** The catalogue row that carries the "last used" label in front of its value. */
    private static final String LAST_USED_ROW = "operator.devices.row";

    /** A value no locale renders, so the label in front of it can be read back. */
    private static final String LAST_USED_VALUE = "\u0000last-used\u0000";

    private static final Pattern OSC = Pattern.compile("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)");
    private static final Pattern CSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern OTHER_ESCAPE = Pattern.compile("\u001B[@-Z\\\\-_]");
    private static final Pattern ISO_INSTANT = Pattern.compile(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?");
    private static final Pattern CLOCK_TIME = Pattern.compile("(?<!\\d)\\d{2}:\\d{2}(?!\\d)");
    private static final Pattern PROCESS_ID = Pattern.compile("\\bpid\\s+\\d+");
    private static final Pattern DURATION = Pattern.compile(
                        "\\b\\d+ (?:s|min|h|d|T|day|days|week|weeks)(?: \\d+ (?:h|min))?\\b");
    private static final Pattern AGO = Pattern.compile(
                        "\\b(?:just now|gerade eben|last month|letzten Monat|last year|letztes Jahr"
                                            + "|\\d+ (?:s|min|h|d|T|day|days|week|weeks|month|months|year|years) ago"
                                            + "|vor \\d+ (?:min|h|T|Tag|Tagen|Monat|Monaten|Jahr|Jahren))\\b");
    private static final Pattern ABSOLUTE_LAST_USED = absoluteLastUsed();
    private static final Pattern TRAILING_SPACE = Pattern.compile("(?m)[ \t]+$");

    private final Map<String, String> literals = new LinkedHashMap<>();

    /**
     * @param literal     a literal that changes between runs, such as a machine root
     * @param placeholder the text that stands for it in the transcript
     * @return this normalizer
     */
    public TranscriptNormalizer replacing(String literal, String placeholder) {
        if (literal != null && !literal.isEmpty()) {
            literals.put(literal, placeholder);
        }
        return this;
    }

    /**
     * @param path        a path that changes between runs, such as a machine root
     * @param placeholder the text that stands for it in the transcript
     * @return this normalizer
     */
    public TranscriptNormalizer replacing(Path path, String placeholder) {
        return replacing(path == null ? null : path.toString(), placeholder);
    }

    /**
     * @param buffer the rendered terminal buffer
     * @return the buffer with the transient parts replaced
     */
    public String normalize(String buffer) {
        var text = buffer.replace("\r\n", "\n").replace('\r', '\n');
        text = OSC.matcher(text).replaceAll("");
        text = CSI.matcher(text).replaceAll("");
        text = OTHER_ESCAPE.matcher(text).replaceAll("");
        for (var entry : literals.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        text = ISO_INSTANT.matcher(text).replaceAll("<timestamp>");
        text = PROCESS_ID.matcher(text).replaceAll("pid <pid>");
        text = AGO.matcher(text).replaceAll("<ago>");
        text = DURATION.matcher(text).replaceAll("<duration>");
        text = CLOCK_TIME.matcher(text).replaceAll("<time>");
        text = ABSOLUTE_LAST_USED.matcher(text).replaceAll("<ago>");
        text = TRAILING_SPACE.matcher(text).replaceAll("");
        return text;
    }

    /**
     * Builds the pattern for an absolute "last used" date from the catalogue words of every
     * supported locale, so a wording change in the catalogue never leaves a transcript pinned to
     * a label the console no longer prints.
     *
     * @return the pattern for an absolute last-used value, its label read from the catalogue
     */
    private static Pattern absoluteLastUsed() {
        var labels = lastUsedLabels();
        if (labels.isEmpty()) {
            return Pattern.compile("\\b\\B");
        }
        var alternation = String.join("|", labels.stream().map(Pattern::quote).toList());
        return Pattern.compile("(?<=(?:" + alternation + ") )\\d{1,2} [A-Z][a-z]{2} <time>");
    }

    /**
     * @return the "last used" label of every supported locale, read from the catalogue row that
     *         prints it in front of its value
     */
    static List<String> lastUsedLabels() {
        var labels = new LinkedHashSet<String>();
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            var row = MessageCatalogue.format(locale, LAST_USED_ROW, Map.of(
                                "index", 1, "label", "label", "date", "date", "from", "from",
                                "last_used", LAST_USED_VALUE));
            var value = row.indexOf(LAST_USED_VALUE);
            if (value > 0) {
                var label = trailingWords(row.substring(0, value));
                if (!label.isEmpty()) {
                    labels.add(label);
                }
            }
        }
        return List.copyOf(labels);
    }

    /**
     * @param prefix the text in front of the value placeholder
     * @return the words that label the value, that is those after the last run of spaces
     */
    private static String trailingWords(String prefix) {
        var stripped = prefix.strip();
        var gap = stripped.lastIndexOf("  ");
        return gap >= 0 ? stripped.substring(gap + 2).strip() : stripped;
    }

    /**
     * Splits a normalized buffer into lines, without a trailing empty line.
     *
     * @param buffer the buffer
     * @return the lines
     */
    static List<String> lines(String buffer) {
        if (buffer.isEmpty()) {
            return List.of();
        }
        var withoutTrailingBreak = buffer.endsWith("\n") ? buffer.substring(0, buffer.length() - 1) : buffer;
        return List.of(withoutTrailingBreak.split("\n", -1));
    }
}
