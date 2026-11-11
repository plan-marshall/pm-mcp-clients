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

import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.operator.tui.TuiRow;

/**
 * What a full-screen level says when its own read was refused: the refusal's own text, never the
 * level's empty-state text ({@code doc/design/terminal-client/15-overview.adoc}).
 *
 * @param title what happened
 * @param cause why the runtime refused
 * @param fix     what to do next
 */
public record LoadRefusal(String title, String cause, String fix) {

    /** Row id of a level's refusal. */
    public static final String ROW_ID = "__refusal__";

    /**
     * Reads the refusal out of the runtime's answer.
     *
     * @param locale   the interface locale
     * @param response the refused response
     * @return the refusal's three lines
     */
    public static LoadRefusal of(String locale, HttpResponse response) {
        var code = codeOf(response);
        return new LoadRefusal(
                            clean(ErrorTexts.title(locale, code)),
                            clean(ErrorTexts.cause(locale, code)),
                            clean(ErrorTexts.fix(locale, code)));
    }

    /**
     * Reads the refusal out of a failed read.
     *
     * @param locale  the interface locale
     * @param reason  the reason the read failed, or {@code null}
     * @return the refusal's three lines
     */
    public static LoadRefusal of(String locale, @Nullable String reason) {
        return new LoadRefusal(
                            clean(ErrorTexts.title(locale, (String) null)),
                            clean(reason != null && !reason.isBlank() ? reason
                                                : ErrorTexts.cause(locale, (String) null)),
                            clean(ErrorTexts.fix(locale, (String) null)));
    }

    /**
     * Reads the refusal out of a failed read.
     *
     * @param locale  the interface locale
     * @param failure the failure
     * @return the refusal's three lines
     */
    public static LoadRefusal of(String locale, IOException failure) {
        return of(locale, failure.getMessage());
    }

    /**
     * @return the rows the level shows for the refusal
     */
    public List<TuiRow> rows() {
        return List.of(
                            TuiRow.leaf(ROW_ID, "  " + title),
                            TuiRow.leaf(ROW_ID + ".cause", "  " + cause),
                            TuiRow.leaf(ROW_ID + ".fix", "  " + fix));
    }

    /**
     * @param response the refused response
     * @return the error code the body carries, or {@code null} when it carries none
     */
    private static @Nullable String codeOf(HttpResponse response) {
        try {
            var body = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            return body != null ? JsonTree.string(body, "error") : null;
        } catch (IOException _) {
            return null;
        }
    }

    private static String clean(String text) {
        return OutputSafety.stripControlSequences(text).replace('\n', ' ').trim();
    }
}
