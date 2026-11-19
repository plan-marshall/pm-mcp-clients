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

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

/**
 * Renders refusals into the three-line pattern: title line with ✗, cause line, and fix line with →.
 */
public final class RefusalRenderer {

    private static final String GLYPH_REFUSED = "✗ ";
    private static final String GLYPH_FIX = "→ ";

    private RefusalRenderer() {
    }

    /**
     * Formats the three refusal lines for a code.
     *
     * @param locale the locale (e.g. {@code "en"} or {@code "de"})
     * @param code   the API error code, or {@code null} for unknown
     * @return the three lines
     */
    public static List<String> renderLines(@Nullable String locale, @Nullable ApiErrorCode code) {
        return renderLines(locale, code, null, Map.of());
    }

    /**
     * Formats the three refusal lines for a code and optional server message.
     *
     * @param locale        the locale (e.g. {@code "en"} or {@code "de"})
     * @param code          the API error code, or {@code null} for unknown
     * @param serverMessage the server message for unknown refusals
     * @return the three lines
     */
    public static List<String> renderLines(@Nullable String locale, @Nullable ApiErrorCode code,
                        @Nullable String serverMessage) {
        return renderLines(locale, code, serverMessage, Map.of());
    }

    /**
     * Formats the three refusal lines for a code, server message, and fix arguments.
     *
     * @param locale        the locale (e.g. {@code "en"} or {@code "de"})
     * @param code          the API error code, or {@code null} for unknown
     * @param serverMessage the server message for unknown refusals
     * @param arguments     arguments for the fix line
     * @return the three lines
     */
    public static List<String> renderLines(@Nullable String locale, @Nullable ApiErrorCode code,
                        @Nullable String serverMessage, Map<String, ?> arguments) {
        return List.of(
                            titleLine(locale, code, serverMessage),
                            causeLine(locale, code),
                            fixLine(locale, code, arguments)
        );
    }

    /**
     * Builds the title line (line 1), prefixed with {@code ✗ }.
     */
    public static String titleLine(@Nullable String locale, @Nullable ApiErrorCode code,
                        @Nullable String serverMessage) {
        if (code != null) {
            return GLYPH_REFUSED + ErrorTexts.title(locale, code);
        }
        var sanitized = serverMessage != null ? TerminalSanitizer.sanitizeLine(serverMessage).strip() : null;
        if (sanitized != null && !sanitized.isBlank()) {
            boolean de = locale != null && locale.toLowerCase().startsWith("de");
            var prefix = de ? "Die Runtime hat die Anfrage abgelehnt: " : "The runtime refused: ";
            return GLYPH_REFUSED + prefix + sanitized;
        }
        return GLYPH_REFUSED + ErrorTexts.title(locale, (ApiErrorCode) null);
    }

    /**
     * Builds the cause line (line 2).
     */
    public static String causeLine(@Nullable String locale, @Nullable ApiErrorCode code) {
        return ErrorTexts.cause(locale, code);
    }

    /**
     * Builds the fix line (line 3), prefixed with {@code → }.
     */
    public static String fixLine(@Nullable String locale, @Nullable ApiErrorCode code, Map<String, ?> arguments) {
        return GLYPH_FIX + ErrorTexts.fix(locale, code, arguments);
    }

    /**
     * Renders the refusal to a writer.
     */
    public static void render(PrintWriter out, @Nullable String locale, @Nullable ApiErrorCode code) {
        render(out, locale, code, null, Map.of());
    }

    /**
     * Renders the refusal with arguments to a writer.
     */
    public static void render(PrintWriter out, @Nullable String locale, @Nullable ApiErrorCode code,
                        @Nullable String serverMessage, Map<String, ?> arguments) {
        for (var line : renderLines(locale, code, serverMessage, arguments)) {
            out.println(line);
        }
        out.flush();
    }

    /**
     * Renders an {@link OperatorRefusedException} to a writer.
     */
    public static void render(PrintWriter out, @Nullable String locale, OperatorRefusedException exception) {
        render(out, locale, exception.code(), exception.serverMessage(), Map.of());
    }
}
