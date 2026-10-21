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
 * The modal menu of a plan's or an epic's actions, drawn below the row or the header it belongs to
 * ({@code doc/design/terminal-client/19-actions.adoc#full-screen}).
 *
 * <p>It is modal: while it is open only its mnemonics, the arrows with {@code Enter}, and
 * {@code a} or {@code Esc} act. The menu carries its own selection, so the row selection of the
 * level stays where it was.
 *
 * @param subjectRowId the row the menu is drawn below, or {@code null} to draw it below the
 *                     selected row
 * @param entries      the entries, in the order they are drawn
 * @param selectedIndex the index of the selected entry, or {@code -1}
 */
public record TuiMenu(@Nullable String subjectRowId, List<TuiMenuEntry> entries, int selectedIndex) {

    /**
     * @param subjectRowId  the row the menu belongs to, or {@code null}
     * @param entries       the entries
     * @param selectedIndex the selected entry
     */
    public TuiMenu {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("a menu needs an entry");
        }
    }

    /**
     * @param subjectRowId the row the menu belongs to, or {@code null}
     * @param entries      the entries
     * @return a menu whose first entry is selected
     */
    public static TuiMenu opening(@Nullable String subjectRowId, List<TuiMenuEntry> entries) {
        return new TuiMenu(subjectRowId, entries, 0);
    }

    /**
     * @return the selected entry, or {@code null} when the menu is empty
     */
    public @Nullable TuiMenuEntry selected() {
        return selectedIndex >= 0 && selectedIndex < entries.size() ? entries.get(selectedIndex) : null;
    }

    /**
     * @param delta the number of entries to move, negative for up
     * @return the same menu with the selection moved, stopping at the edges
     */
    public TuiMenu move(int delta) {
        int next = Math.max(0, Math.min(entries.size() - 1, selectedIndex + delta));
        return new TuiMenu(subjectRowId, entries, next);
    }

    /**
     * @param trigger the mnemonic
     * @return the entry with that mnemonic, or {@code null}
     */
    public @Nullable TuiMenuEntry entry(String trigger) {
        for (var entry : entries) {
            if (entry.trigger().equals(trigger)) {
                return entry;
            }
        }
        return null;
    }
}
