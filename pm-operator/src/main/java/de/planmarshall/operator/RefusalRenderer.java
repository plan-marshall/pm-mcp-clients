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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

/**
 * Renders refusals into the three-line pattern: title line with ✗, cause line, and fix line with →.
 * <p>
 * A refusal that carries a code prints the code's three texts. A refusal that carries no code says
 * what it is: a local condition prints its own text as the title, and both the cause and the fix are
 * the local ones; a 404 on a route the runtime does not know and a body that is no refusal at all
 * each print their own three lines. Only an unknown runtime refusal that carries a message is shown
 * with the runtime's message, behind the catalogue's prefix.
 */
public final class RefusalRenderer {

    private static final String GLYPH_REFUSED = "✗ ";
    private static final String GLYPH_FIX = "→ ";
    private static final String PREFIX = OperatorRefusedException.PREFIX_KEY;

    /** The catalogue keys this renderer needs in every supported locale. */
    private static final List<String> REQUIRED_KEYS = List.of(
                        PREFIX,
                        "operator.refusal.local.cause",
                        "operator.refusal.local.fix",
                        "operator.refusal.route_missing.title",
                        "operator.refusal.route_missing.cause",
                        "operator.refusal.route_missing.fix",
                        "operator.refusal.unreadable.title",
                        "operator.refusal.unreadable.cause",
                        "operator.refusal.unreadable.fix");

    private RefusalRenderer() {
    }

    /**
     * @return the catalogue keys the renderer needs in every supported locale
     */
    public static List<String> requiredKeys() {
        return REQUIRED_KEYS;
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
     * Formats the three refusal lines of an exception, which knows whether it carries a code, a
     * local text, a missing route or a body this client cannot read.
     *
     * @param locale    the locale (e.g. {@code "en"} or {@code "de"})
     * @param exception the refusal
     * @return the three lines
     */
    public static List<String> linesOf(@Nullable String locale, OperatorRefusedException exception) {
        var kind = exception.kind();
        if (kind == OperatorRefusedException.Kind.RUNTIME) {
            return renderLines(locale, exception.code(), exception.serverMessage(), exception.arguments());
        }
        if (kind == OperatorRefusedException.Kind.LOCAL) {
            var text = exception.serverMessage() != null ? exception.serverMessage() : "";
            return List.of(GLYPH_REFUSED + text,
                                areaText(locale, kind, ".cause"),
                                GLYPH_FIX + areaText(locale, kind, ".fix"));
        }
        return List.of(
                            GLYPH_REFUSED + areaText(locale, kind, ".title"),
                            areaText(locale, kind, ".cause"),
                            GLYPH_FIX + areaText(locale, kind, ".fix"));
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
            return GLYPH_REFUSED + MessageCatalogue.format(locale, OperatorRefusedException.PREFIX_KEY, Map.of())
                                + sanitized;
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
        return GLYPH_FIX + ErrorTexts.fix(locale, code, sanitized(arguments));
    }

    /**
     * @param arguments the arguments the fix line substitutes
     * @return the arguments with every text's control sequences removed, so a body value can never
     *         inject a terminal sequence into the fix line
     */
    private static Map<String, ?> sanitized(Map<String, ?> arguments) {
        var clean = new LinkedHashMap<String, Object>();
        arguments.forEach((key, value) -> clean.put(key,
                            value instanceof String text ? TerminalSanitizer.sanitizeLine(text) : value));
        return clean;
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
     * Renders an {@link OperatorRefusedException} to a writer, with the arguments its texts name.
     */
    public static void render(PrintWriter out, @Nullable String locale, OperatorRefusedException exception) {
        for (var line : linesOf(locale, exception)) {
            out.println(line);
        }
        out.flush();
    }

    private static String areaText(@Nullable String locale, OperatorRefusedException.Kind kind, String part) {
        var area = switch (kind) {
            case LOCAL -> "operator.refusal.local";
            case ROUTE_MISSING -> "operator.refusal.route_missing";
            case UNREADABLE -> "operator.refusal.unreadable";
            case RUNTIME -> null;
        };
        return area != null ? MessageCatalogue.format(locale, area + part, Map.of()) : "";
    }
}
