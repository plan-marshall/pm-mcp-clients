/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.util.Map;

import lombok.experimental.UtilityClass;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * The one set of texts of the local API's error codes: a title that says what happened, a cause that
 * says why, and a fix that names what to do next. The texts live in the catalogue area {@code error}
 * ({@code error.<code>.title}, {@code error.<code>.cause} and {@code error.<code>.fix}).
 * <p>
 * The lookup is renderer-independent, so both clients say the same thing: the terminal prints the
 * three lines through it, and the web app reads the same keys through its display layer. A code the
 * lookup does not know — an unknown wire value or no code at all — falls back to the area's
 * {@code error.unknown.*} texts, never to the bare key and never to an empty line.
 */
@UtilityClass
public class ErrorTexts {

    /** The key prefix of the catalogue area that holds the texts. */
    private static final String AREA = "error.";

    /** The part of a key that names the fallback for a code the client does not know. */
    private static final String UNKNOWN = "unknown";

    private static final String TITLE = ".title";
    private static final String CAUSE = ".cause";
    private static final String FIX = ".fix";

    /**
     * Reads the title of a code in the default language.
     *
     * @param code the code, or {@code null} for the unknown fallback
     * @return the title, one line
     */
    public static String title(ApiErrorCode code) {
        return title(null, code);
    }

    /**
     * Reads the title of a code.
     *
     * @param locale the language ({@code en} or {@code de}); {@code null} takes the default language
     * @param code   the code, or {@code null} for the unknown fallback
     * @return the title, one line
     */
    public static String title(String locale, ApiErrorCode code) {
        return text(locale, code, TITLE);
    }

    /**
     * Reads the title of a wire value.
     *
     * @param locale the language ({@code en} or {@code de}); {@code null} takes the default language
     * @param wire   the wire value of the code, or {@code null} for the unknown fallback
     * @return the title, one line; the unknown fallback when no code carries the wire value
     */
    public static String title(String locale, String wire) {
        return text(locale, ApiErrorCode.fromWireOrNull(wire), TITLE);
    }

    /**
     * Reads the cause of a code in the default language.
     *
     * @param code the code, or {@code null} for the unknown fallback
     * @return the cause, why it happened
     */
    public static String cause(ApiErrorCode code) {
        return cause(null, code);
    }

    /**
     * Reads the cause of a code.
     *
     * @param locale the language ({@code en} or {@code de}); {@code null} takes the default language
     * @param code   the code, or {@code null} for the unknown fallback
     * @return the cause, why it happened
     */
    public static String cause(String locale, ApiErrorCode code) {
        return text(locale, code, CAUSE);
    }

    /**
     * Reads the cause of a wire value.
     *
     * @param locale the language ({@code en} or {@code de}); {@code null} takes the default language
     * @param wire   the wire value of the code, or {@code null} for the unknown fallback
     * @return the cause, why it happened; the unknown fallback when no code carries the wire value
     */
    public static String cause(String locale, String wire) {
        return text(locale, ApiErrorCode.fromWireOrNull(wire), CAUSE);
    }

    /**
     * Reads the fix of a code in the default language.
     *
     * @param code the code, or {@code null} for the unknown fallback
     * @return the fix, what to do next
     */
    public static String fix(ApiErrorCode code) {
        return fix(null, code);
    }

    /**
     * Reads the fix of a code.
     *
     * @param locale the language ({@code en} or {@code de}); {@code null} takes the default language
     * @param code   the code, or {@code null} for the unknown fallback
     * @return the fix, what to do next
     */
    public static String fix(String locale, ApiErrorCode code) {
        return text(locale, code, FIX);
    }

    /**
     * Reads the fix of a wire value.
     *
     * @param locale the language ({@code en} or {@code de}); {@code null} takes the default language
     * @param wire   the wire value of the code, or {@code null} for the unknown fallback
     * @return the fix, what to do next; the unknown fallback when no code carries the wire value
     */
    public static String fix(String locale, String wire) {
        return text(locale, ApiErrorCode.fromWireOrNull(wire), FIX);
    }

    private static String text(String locale, ApiErrorCode code, String part) {
        String name = code == null ? UNKNOWN : code.wire();
        return MessageCatalogue.format(locale, AREA + name + part, Map.of());
    }
}
