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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The modal actions menu as the screen draws it ({@code doc/design/terminal-client/19-actions.adoc#full-screen}):
 * its own selection stops at the edges, its mnemonics find their entries, and a menu or an entry
 * the screen could not draw is refused when it is built.
 */
@DisplayName("TuiMenu")
@EnableGeneratorController
class TuiMenuTest {

    private static TuiMenuEntry entry(String trigger) {
        return TuiMenuEntry.enabled(trigger, Generators.letterStrings(4, 8).next(),
                            Generators.letterStrings(4, 12).next());
    }

    @Nested
    @DisplayName("the menu")
    class Menu {

        @Test
        @DisplayName("opens on its first entry, and its selection stops at both edges")
        void selectionStopsAtTheEdges() {
            var first = entry("a");
            var last = entry("b");
            var menu = TuiMenu.opening(null, List.of(first, last));

            assertSame(first, menu.selected());
            assertSame(first, menu.move(-1).selected());
            assertSame(last, menu.move(1).selected());
            assertSame(last, menu.move(5).selected());
        }

        @Test
        @DisplayName("a menu drawn with no entry selected has no selected entry")
        void noSelection() {
            var menu = new TuiMenu(Generators.letterStrings(4, 8).next(), List.of(entry("a")), -1);

            assertNull(menu.selected());
        }

        @Test
        @DisplayName("a mnemonic finds its entry, and one no entry carries finds none")
        void mnemonics() {
            var first = entry("a");
            var second = entry("b");
            var menu = TuiMenu.opening(null, List.of(first, second));

            assertSame(second, menu.entry("b"));
            assertNull(menu.entry("z"));
        }

        @Test
        @DisplayName("a menu without an entry is refused")
        void emptyMenu() {
            List<TuiMenuEntry> none = List.of();

            assertThrows(IllegalArgumentException.class, () -> TuiMenu.opening(null, none));
        }
    }

    @Nested
    @DisplayName("an entry")
    class Entry {

        @Test
        @DisplayName("a disabled entry keeps the reason it names")
        void disabledKeepsItsReason() {
            var reason = Generators.letterStrings(8, 20).next();

            var entry = TuiMenuEntry.disabled("p", "pause", Generators.letterStrings(4, 12).next(), reason);

            assertFalse(entry.enabled());
            assertEquals(reason, entry.reason());
        }

        @Test
        @DisplayName("an entry without a mnemonic is refused")
        void emptyMnemonic() {
            var label = Generators.letterStrings(4, 12).next();

            assertThrows(IllegalArgumentException.class, () -> TuiMenuEntry.enabled("", "pause", label));
        }
    }
}
