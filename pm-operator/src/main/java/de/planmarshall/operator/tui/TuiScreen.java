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

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * What one drawing of the full-screen view shows: the breadcrumb, the level's kind, its visible
 * rows and selection, the filter and the help overlay, and the connection state.
 *
 * @param breadcrumb    the header's left side, already localized
 * @param kind          the kind of level, which selects the footer
 * @param rows          the rows the filter leaves visible
 * @param selectedIndex the index of the selected row in {@code rows}, or {@code -1}
 * @param filter        the filter text, empty when none is set
 * @param help          whether the help overlay is shown
 * @param connected     whether the event stream is open
 * @param reportsCount  the number of unread reports
 * @param footerText    the footer to draw instead of the one the key registry generates, or
 *                      {@code null} for the registry's footer
 */
public record TuiScreen(String breadcrumb, TuiLevel.Kind kind, List<TuiRow> rows, int selectedIndex,
String filter, boolean help, boolean connected, int reportsCount, @Nullable String footerText) {

    /**
     * @param breadcrumb    the header's left side
     * @param kind          the kind of level
     * @param rows          the visible rows
     * @param selectedIndex the index of the selected row, or {@code -1}
     * @param filter        the filter text
     * @param help          whether the help overlay is shown
     * @param connected     whether the event stream is open
     * @param reportsCount  the number of unread reports
     */
    public TuiScreen(String breadcrumb, TuiLevel.Kind kind, List<TuiRow> rows, int selectedIndex,
                        String filter, boolean help, boolean connected, int reportsCount) {
        this(breadcrumb, kind, rows, selectedIndex, filter, help, connected, reportsCount, null);
    }

    /**
     * @param breadcrumb    the header's left side
     * @param kind          the kind of level
     * @param rows          the visible rows
     * @param selectedIndex the index of the selected row, or {@code -1}
     * @param filter        the filter text
     * @param help          whether the help overlay is shown
     * @param connected     whether the event stream is open
     * @param reportsCount  the number of unread reports
     * @param footerText    the footer to draw, or {@code null}
     */
    public TuiScreen {
        Objects.requireNonNull(breadcrumb, "breadcrumb");
        Objects.requireNonNull(kind, "kind");
        rows = List.copyOf(rows);
        filter = filter == null ? "" : filter;
    }

    /**
     * @return the selected row, or {@code null} when no row is visible or selected
     */
    public @Nullable TuiRow selected() {
        return selectedIndex >= 0 && selectedIndex < rows.size() ? rows.get(selectedIndex) : null;
    }
}
