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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The shared ANSI parser: the same subset the full-screen view emits, parsed identically for the
 * in-memory terminal and the real pseudo-terminal.
 */
@DisplayName("AnsiScreen")
class AnsiScreenTest {

    @Test
    @DisplayName("writes plain text line by line and wraps a line longer than the screen")
    void writesAndWraps() {
        var screen = AnsiScreen.parse("abc\r\ndef", 4, 3);

        assertAll(
                            () -> assertEquals("abc", screen.lines().get(0)),
                            () -> assertEquals("def", screen.lines().get(1)),
                            () -> assertEquals("abcd", AnsiScreen.parse("abcdef", 4, 2).lines().get(0)),
                            () -> assertEquals("ef", AnsiScreen.parse("abcdef", 4, 2).lines().get(1))
        );
    }

    @Test
    @DisplayName("addresses a cursor, erases the line and the display, and marks inverse rows")
    void addressesErasesAndInverts() {
        var screen = AnsiScreen.parse("\u001b[2;3HX\u001b[1;1Hleft\u001b[7mY\u001b[0m", 10, 3);

        assertAll(
                            () -> assertEquals("leftY", screen.lines().get(0)),
                            () -> assertEquals("  X", screen.lines().get(1)),
                            () -> assertEquals(Set.of(0), screen.invertedRows()),
                            () -> assertEquals("", AnsiScreen.parse("abc\u001b[2K", 10, 2).lines().get(0)),
                            () -> assertEquals("", AnsiScreen.parse("abc\u001b[2J", 10, 2).lines().get(0))
        );
    }

    @Test
    @DisplayName("records bold, dim, reverse and the colour index per cell")
    void recordsCellAttributes() {
        var screen = AnsiScreen.parse("\u001b[1;2;7;31mA\u001b[0mB\u001b[94mC", 4, 1);

        var row = screen.cellStyles().getFirst();

        assertAll(
                            () -> assertEquals("bdrc1", row.get(0).text()),
                            () -> assertEquals(AnsiScreen.CellStyle.PLAIN.text(), row.get(1).text()),
                            () -> assertEquals("c12", row.get(2).text())
        );
    }

    @Test
    @DisplayName("skips an extended colour sequence whole instead of reading its operands as styles")
    void skipsExtendedColour() {
        var screen = AnsiScreen.parse("\u001b[1mA\u001b[0m\u001b[38;5;1mB", 4, 1);

        var row = screen.cellStyles().getFirst();

        assertAll(
                            () -> assertEquals("b", row.get(0).text()),
                            () -> assertEquals("-", row.get(1).text())
        );
    }

    @Test
    @DisplayName("counts alternate-screen entries and exits and tracks the cursor")
    void countsAlternationAndCursor() {
        var active = AnsiScreen.parse("\u001b[?1049h\u001b[?25lRunning work", 40, 4);
        var left = AnsiScreen.parse("\u001b[?1049hRunning work\u001b[?1049l\u001b[?25h", 40, 4);

        assertAll(
                            () -> assertTrue(active.alternateScreenActive()),
                            () -> assertFalse(active.cursorVisible()),
                            () -> assertEquals(1, active.alternation().entries()),
                            () -> assertEquals(0, active.alternation().exits()),
                            () -> assertFalse(left.alternateScreenActive()),
                            () -> assertTrue(left.cursorVisible()),
                            () -> assertEquals(1, left.alternation().exits())
        );
    }

    @Test
    @DisplayName("keeps a wide glyph in one column and drops the continuation cell")
    void keepsWideGlyphs() {
        var screen = AnsiScreen.parse("\u4e2d\u6587 ok", 10, 2);

        assertEquals("\u4e2d\u6587 ok", screen.lines().get(0));
    }

    @Test
    @DisplayName("skips OSC and string introducers")
    void skipsOscAndStrings() {
        var screen = AnsiScreen.parse("\u001b]0;a title\u0007ok\u001b]9;notify\u0007!\u001bPignored\u001b\\?", 10, 2);

        assertEquals("ok!?", screen.lines().get(0));
    }

    @Test
    @DisplayName("moves the cursor up, down, forward, back, and to an absolute column and row")
    void movesTheCursor() {
        var screen = AnsiScreen.parse("abcd\u001b[2D\u001b[s", 10, 2);

        assertEquals("abcd", screen.lines().get(0));
        assertEquals("ab", AnsiScreen.parse("abcd\u001b[2D\u001b[K", 10, 2).lines().get(0));
        assertEquals("x", AnsiScreen.parse("   \u001b[3Gx", 10, 2).lines().get(0).stripLeading());
        assertEquals("m", AnsiScreen.parse("\u001b[2d\u001b[2Gm", 10, 3).lines().get(1).strip());
    }
}
