/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One row of a full-screen view: the text of its line, the details printed below it on a wide
 * terminal (and by {@code ?}), and what the row offers.
 *
 * <p>The text is untrusted until it is drawn: a row may carry a plan title, a file name, or a
 * branch name from the runtime. {@link TuiLayout} sanitizes and truncates it; this record only
 * carries it.
 *
 * @param id            the stable id the selection follows across refreshes
 * @param line          the first line of the row, already composed by the level
 * @param details       the details line, or {@code null} when the row has none
 * @param needsOperator {@code true} when the row carries the marker {@code !}
 * @param openable      {@code true} when {@code Enter} opens a level from this row
 */
public record TuiRow(String id, String line, @Nullable String details, boolean needsOperator, boolean openable) {

    /**
     * @param id            the stable id of the row
     * @param line          the first line of the row
     * @param details       the details line, or {@code null}
     * @param needsOperator whether the row carries the {@code !} marker
     * @param openable      whether {@code Enter} opens a level from this row
     */
    public TuiRow {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(line, "line");
    }

    /**
     * @param id   the stable id of the row
     * @param line the first line of the row
     * @return a leaf row without details, marker, or an opened level
     */
    public static TuiRow leaf(String id, String line) {
        return new TuiRow(id, line, null, false, false);
    }

    /**
     * @param id      the stable id of the row
     * @param line    the first line of the row
     * @param details the details line
     * @return a row that shows details and opens a level
     */
    public static TuiRow openable(String id, String line, @Nullable String details) {
        return new TuiRow(id, line, details, false, true);
    }

    /**
     * @return the text a filter matches against: the line and the details
     */
    public String searchable() {
        return details == null ? line : line + ' ' + details;
    }
}
