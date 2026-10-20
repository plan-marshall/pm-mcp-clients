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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TuiViewStack")
class TuiViewStackTest {

    private static final TuiLevel ROOT = TuiLevel.root(TuiLevel.Kind.OVERVIEW, "Running work");
    private static final TuiLevel PROJECT = TuiLevel.of(TuiLevel.Kind.PROJECT_BY_PLAN, "p1", "p1");

    private TuiViewStack stack;

    @BeforeEach
    void setUp() {
        stack = new TuiViewStack();
        stack.replaceRoot(ROOT, rows(), null);
    }

    private static List<TuiRow> rows() {
        return List.of(
                            new TuiRow("a", "alpha", "first details", true, true),
                            new TuiRow("b", "beta", null, false, true),
                            new TuiRow("c", "gamma", "third details", false, false));
    }

    @Test
    @DisplayName("opens on its root with the first row selected")
    void opensOnRoot() {
        assertAll(
                            () -> assertEquals(1, stack.depth()),
                            () -> assertEquals(ROOT, stack.level()),
                            () -> assertEquals("a", stack.selected().id()),
                            () -> assertEquals(0, stack.selectedIndex()),
                            () -> assertFalse(stack.canPop())
        );
    }

    @Test
    @DisplayName("pushes a level, keeps the caller's selection, and pops back to it")
    void pushAndPop() {
        stack.move(1);
        stack.filter("gamma");
        assertEquals("c", stack.selected().id());

        stack.push(PROJECT, List.of(TuiRow.leaf("x", "one"), TuiRow.leaf("y", "two")), null);
        assertAll(
                            () -> assertEquals(2, stack.depth()),
                            () -> assertEquals(PROJECT, stack.level()),
                            () -> assertEquals("x", stack.selected().id())
        );

        assertTrue(stack.pop());
        assertAll(
                            () -> assertEquals(ROOT, stack.level()),
                            () -> assertEquals("gamma", stack.filter()),
                            () -> assertEquals("c", stack.selected().id())
        );
    }

    @Test
    @DisplayName("jumpTo opens a level and returns to the level it was opened from")
    void jumpToReturnsToOrigin() {
        stack.jumpTo(new TuiLevel(TuiLevel.Kind.SYSTEM, null, "system"),
                            List.of(TuiRow.leaf("s", "system row")), null);
        assertEquals(TuiLevel.Kind.SYSTEM, stack.level().kind());
        assertTrue(stack.back());
        assertEquals(ROOT, stack.level());
    }

    @Test
    @DisplayName("replaceRoot discards the stack")
    void replaceRoot() {
        stack.push(PROJECT, rows(), "b");
        stack.replaceRoot(ROOT, rows(), "c");
        assertAll(
                            () -> assertEquals(1, stack.depth()),
                            () -> assertEquals("c", stack.selected().id())
        );
    }

    @Test
    @DisplayName("the root is never popped")
    void rootIsNeverPopped() {
        assertFalse(stack.pop());
        assertEquals(1, stack.depth());
    }

    @Test
    @DisplayName("a refresh keeps the selection on its row id, wherever the row moves")
    void refreshAnchorsSelectionById() {
        stack.move(1);
        stack.refresh(List.of(TuiRow.leaf("new", "new first"), TuiRow.leaf("b", "beta"),
                            TuiRow.leaf("a", "alpha")));
        assertEquals("b", stack.selected().id());
        assertEquals(1, stack.selectedIndex());
    }

    @Test
    @DisplayName("a refresh that loses the selected row moves to the nearest row")
    void refreshFallsBackWhenRowIsGone() {
        stack.last();
        stack.refresh(List.of(TuiRow.leaf("a", "alpha"), TuiRow.leaf("b", "beta")));
        assertEquals("b", stack.selected().id());
    }

    @Test
    @DisplayName("the filter matches the line and the details, ignoring case")
    void filterMatchesLineAndDetails() {
        stack.filter("FIRST DETAILS");
        assertAll(
                            () -> assertEquals(1, stack.visibleRows().size()),
                            () -> assertEquals("a", stack.selected().id())
        );
        stack.filter("gamma");
        assertEquals("c", stack.selected().id());
        stack.filter(null);
        assertEquals(3, stack.visibleRows().size());
    }

    @Test
    @DisplayName("Esc closes the help first, then clears the filter, and only then pops")
    void escapeOrder() {
        stack.push(PROJECT, rows(), null);
        stack.toggleHelp();
        stack.filter("beta");
        assertTrue(stack.back());
        assertFalse(stack.helpVisible());
        assertEquals("beta", stack.filter());
        assertTrue(stack.back());
        assertTrue(stack.filter().isEmpty());
        assertEquals(2, stack.depth());
        assertTrue(stack.back());
        assertEquals(1, stack.depth());
        assertFalse(stack.back());
    }

    @Test
    @DisplayName("move, page, first, and last stop at the edges")
    void movementStopsAtEdges() {
        stack.first();
        stack.move(-1);
        assertEquals("a", stack.selected().id());
        stack.last();
        stack.move(1);
        assertEquals("c", stack.selected().id());
        stack.first();
        stack.page(1);
        assertEquals("c", stack.selected().id());
    }

    @Test
    @DisplayName("an empty row list has no selection")
    void emptyRowsHaveNoSelection() {
        stack.replaceRoot(ROOT, List.of(), null);
        assertAll(
                            () -> assertNull(stack.selected()),
                            () -> assertEquals(-1, stack.selectedIndex())
        );
    }

    @Nested
    @DisplayName("while the stack is empty")
    class Empty {

        @Test
        @DisplayName("level, rows, filter, selection, and help are empty and no act throws")
        void emptyStack() {
            var empty = new TuiViewStack();
            assertAll(
                                () -> assertTrue(empty.isEmpty()),
                                () -> assertNull(empty.level()),
                                () -> assertTrue(empty.visibleRows().isEmpty()),
                                () -> assertTrue(empty.rows().isEmpty()),
                                () -> assertTrue(empty.filter().isEmpty()),
                                () -> assertFalse(empty.helpVisible()),
                                () -> assertNull(empty.selected()),
                                () -> assertEquals(-1, empty.selectedIndex())
            );
            empty.move(1);
            empty.first();
            empty.last();
            empty.page(1);
            empty.filter("x");
            empty.toggleHelp();
            empty.closeHelp();
            empty.refresh(List.of());
            assertFalse(empty.back());
            assertNotNull(empty);
        }
    }
}
