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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The model of the full-screen view: a stack of levels, each with its rows, its selection by row
 * id, its filter, and its help overlay.
 *
 * <p>It is pure Java and knows nothing about JLine or the terminal. That separation is what lets
 * the drawing change without the navigation, and what lets the tests exercise a push, a pop, a
 * filter, and a refresh without a terminal.
 *
 * <p>The selection is an id, never an index: when the runtime's snapshot moves a row, the
 * selection follows the row. A row that disappeared moves the selection to the nearest row.
 */
public final class TuiViewStack {

    private static final int PAGE = 10;

    private final List<Frame> frames = new ArrayList<>();

    private static final class Frame {
        private final TuiLevel level;
        private List<TuiRow> rows;
        private List<TuiRow> visible;
        private @Nullable String selectedId;
        private String filter = "";
        private boolean help;

        private Frame(TuiLevel level, List<TuiRow> rows, @Nullable String selectedId) {
            this.level = level;
            this.rows = List.copyOf(rows);
            this.selectedId = selectedId != null ? selectedId : firstId(rows);
            applyFilter();
        }

        private static @Nullable String firstId(List<TuiRow> rows) {
            return rows.isEmpty() ? null : rows.getFirst().id();
        }

        private void applyFilter() {
            applyFilter(-1);
        }

        private int selectedIndex() {
            if (visible.isEmpty() || selectedId == null) {
                return -1;
            }
            for (int index = 0; index < visible.size(); index++) {
                if (visible.get(index).id().equals(selectedId)) {
                    return index;
                }
            }
            return 0;
        }

        private void applyFilter(int fallbackIndex) {
            if (filter.isEmpty()) {
                visible = rows;
            } else {
                var needle = filter.toLowerCase(Locale.ROOT);
                var matching = new ArrayList<TuiRow>();
                for (var row : rows) {
                    if (row.searchable().toLowerCase(Locale.ROOT).contains(needle)) {
                        matching.add(row);
                    }
                }
                visible = List.copyOf(matching);
            }
            if (visible.isEmpty()) {
                selectedId = null;
            } else if (selectedId == null || visible.stream().noneMatch(row -> row.id().equals(selectedId))) {
                int nearest = fallbackIndex < 0 ? 0 : Math.min(fallbackIndex, visible.size() - 1);
                selectedId = visible.get(nearest).id();
            }
        }
    }

    private Frame current() {
        return frames.getLast();
    }

    /**
     * @param level       the level to push
     * @param rows        the rows of the level
     * @param selectedId  the row to select, or {@code null} for the first
     */
    public void push(TuiLevel level, List<TuiRow> rows, @Nullable String selectedId) {
        frames.add(new Frame(Objects.requireNonNull(level, "level"),
                            Objects.requireNonNull(rows, "rows"), selectedId));
    }

    /**
     * Opens a level from the current one without replacing it, so that {@code Esc} returns to the
     * level the operator came from. It is the same act as {@link #push(TuiLevel, List, String)};
     * the name says why the caller opens it.
     *
     * @param level      the level to open
     * @param rows       the rows of the level
     * @param selectedId the row to select, or {@code null} for the first
     */
    public void jumpTo(TuiLevel level, List<TuiRow> rows, @Nullable String selectedId) {
        push(level, rows, selectedId);
    }

    /**
     * Replaces the whole stack with one root level.
     *
     * @param level      the new root
     * @param rows       the rows of the root
     * @param selectedId the row to select, or {@code null} for the first
     */
    public void replaceRoot(TuiLevel level, List<TuiRow> rows, @Nullable String selectedId) {
        frames.clear();
        push(level, rows, selectedId);
    }

    /**
     * @return {@code true} when there is a level below the current one to return to
     */
    public boolean canPop() {
        return frames.size() > 1;
    }

    /**
     * Pops one level. The root is never popped.
     *
     * @return {@code true} when a level was popped
     */
    public boolean pop() {
        if (frames.size() <= 1) {
            return false;
        }
        frames.removeLast();
        return true;
    }

    /** @return the current level, or {@code null} when the stack is empty */
    public @Nullable TuiLevel level() {
        return frames.isEmpty() ? null : current().level;
    }

    /** @return the number of levels in the stack */
    public int depth() {
        return frames.size();
    }

    /**
     * @param index the zero-based index from the root
     * @return the level at that index, or {@code null}
     */
    public @Nullable TuiLevel levelAt(int index) {
        return index >= 0 && index < frames.size() ? frames.get(index).level : null;
    }

    /** @return {@code true} while the stack is empty */
    public boolean isEmpty() {
        return frames.isEmpty();
    }

    /** @return the rows of the current level that the filter leaves visible */
    public List<TuiRow> visibleRows() {
        return frames.isEmpty() ? List.of() : List.copyOf(current().visible);
    }

    /** @return all rows of the current level, including those the filter hides */
    public List<TuiRow> rows() {
        return frames.isEmpty() ? List.of() : current().rows;
    }

    /** @return the filter of the current level, empty when no filter is set */
    public String filter() {
        return frames.isEmpty() ? "" : current().filter;
    }

    /** @return {@code true} while the help overlay is shown */
    public boolean helpVisible() {
        return !frames.isEmpty() && current().help;
    }

    /** @return the selected row, or {@code null} when no row is visible */
    public @Nullable TuiRow selected() {
        if (frames.isEmpty()) {
            return null;
        }
        var frame = current();
        if (frame.visible.isEmpty()) {
            return null;
        }
        return frame.visible.get(selectedIndex());
    }

    /** @return the index of the selection in {@link #visibleRows()}, or {@code -1} */
    public int selectedIndex() {
        if (frames.isEmpty()) {
            return -1;
        }
        var frame = current();
        if (frame.visible.isEmpty()) {
            return -1;
        }
        if (frame.selectedId != null) {
            for (int i = 0; i < frame.visible.size(); i++) {
                if (frame.visible.get(i).id().equals(frame.selectedId)) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * Moves the selection by {@code delta} rows, stopping at the edges.
     *
     * @param delta the number of rows, negative for up
     */
    public void move(int delta) {
        if (frames.isEmpty()) {
            return;
        }
        var frame = current();
        if (frame.visible.isEmpty()) {
            return;
        }
        int next = Math.max(0, Math.min(frame.visible.size() - 1, selectedIndex() + delta));
        frame.selectedId = frame.visible.get(next).id();
    }

    /** Selects the first visible row. */
    public void first() {
        move(-Integer.MAX_VALUE);
    }

    /** Selects the last visible row. */
    public void last() {
        move(Integer.MAX_VALUE);
    }

    /**
     * @param direction {@code -1} for a page up, {@code 1} for a page down
     */
    public void page(int direction) {
        move(direction * PAGE);
    }

    /**
     * Selects the visible row with the given id, if it is visible.
     *
     * @param id the row id to select, or {@code null} to leave the selection unchanged
     * @return {@code true} when the row was found and selected
     */
    public boolean select(@Nullable String id) {
        if (frames.isEmpty() || id == null) {
            return false;
        }
        var frame = current();
        if (frame.visible.stream().noneMatch(row -> row.id().equals(id))) {
            return false;
        }
        frame.selectedId = id;
        return true;
    }

    /**
     * @return the id of the last visible row, or {@code null} when no row is visible
     */
    public @Nullable String lastVisibleId() {
        if (frames.isEmpty() || current().visible.isEmpty()) {
            return null;
        }
        return current().visible.getLast().id();
    }

    /**
     * @return the number of visible rows of the current level
     */
    public int visibleRowCount() {
        return frames.isEmpty() ? 0 : current().visible.size();
    }

    /**
     * Replaces the rows of the current level, keeping the selection on its row id.
     *
     * @param rows the new rows
     */
    public void refresh(List<TuiRow> rows) {
        if (frames.isEmpty()) {
            return;
        }
        var frame = current();
        int previousIndex = frame.selectedIndex();
        frame.rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        frame.applyFilter(previousIndex);
    }

    /**
     * Sets the substring filter of the current level. A row matches when its line or details
     * contain the text, ignoring case.
     *
     * @param filter the filter text, {@code null} or blank clears it
     */
    public void filter(@Nullable String filter) {
        if (frames.isEmpty()) {
            return;
        }
        var frame = current();
        frame.filter = filter == null ? "" : filter.strip();
        frame.applyFilter();
    }

    /** Toggles the help overlay of the current level. */
    public void toggleHelp() {
        if (!frames.isEmpty()) {
            var frame = current();
            frame.help = !frame.help;
        }
    }

    /** Closes the help overlay of the current level, if it is open. */
    public void closeHelp() {
        if (!frames.isEmpty()) {
            current().help = false;
        }
    }

    /**
     * The act of {@code Esc}: it closes an open help overlay, then clears a filter, and only then
     * pops the level, so that a first {@code Esc} never loses the operator's place.
     *
     * @return {@code true} when the view is still open afterwards
     */
    public boolean back() {
        if (frames.isEmpty()) {
            return false;
        }
        var frame = current();
        if (frame.help) {
            frame.help = false;
            return true;
        }
        if (!frame.filter.isEmpty()) {
            frame.filter = "";
            frame.applyFilter();
            return true;
        }
        return pop();
    }
}
