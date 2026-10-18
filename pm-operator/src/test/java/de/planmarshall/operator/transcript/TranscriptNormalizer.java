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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Removes what a transcript must not pin from a rendered terminal buffer: ANSI and OSC escapes,
 * clock times and instants, process ids, and the throw-away machine root a fixture test uses.
 * Everything a reader would compare, the texts, the layout, and the order, stays untouched.
 */
public final class TranscriptNormalizer {

    private static final Pattern OSC = Pattern.compile("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)");
    private static final Pattern CSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern OTHER_ESCAPE = Pattern.compile("\u001B[@-Z\\\\-_]");
    private static final Pattern ISO_INSTANT = Pattern.compile(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?");
    private static final Pattern CLOCK_TIME = Pattern.compile("(?<!\\d)\\d{2}:\\d{2}(?!\\d)");
    private static final Pattern PROCESS_ID = Pattern.compile("\\bpid\\s+\\d+");
    private static final Pattern HOURS_MINUTES = Pattern.compile("\\b\\d+ h(?: \\d+ min)?\\b");
    private static final Pattern MINUTES = Pattern.compile("\\b\\d+ min\\b");
    private static final Pattern AGO = Pattern.compile("\\b(?:just now|\\d+ (?:s|min|h|d|day|days|week|weeks|month|months) ago)\\b");
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
        text = HOURS_MINUTES.matcher(text).replaceAll("<duration>");
        text = MINUTES.matcher(text).replaceAll("<duration>");
        text = CLOCK_TIME.matcher(text).replaceAll("<time>");
        text = TRAILING_SPACE.matcher(text).replaceAll("");
        return text;
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
