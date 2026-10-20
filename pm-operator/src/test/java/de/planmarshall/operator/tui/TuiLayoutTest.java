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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TuiLayout")
class TuiLayoutTest {

    private final TuiLayout layout = new TuiLayout("en");

    private static TuiScreen screen(int selected, int rows) {
        var list = new ArrayList<TuiRow>();
        for (int index = 0; index < rows; index++) {
            list.add(new TuiRow("row-" + index, "row number " + index + " title",
                                "details of row " + index, index == 0, true));
        }
        return new TuiScreen("Running work \u00b7 projects", TuiLevel.Kind.OVERVIEW, list, selected, "",
                            false, true, 3);
    }

    @Test
    @DisplayName("draws exactly the terminal's height in lines")
    void drawsExactHeight() {
        var lines = layout.render(screen(0, 3), 120, 30);
        assertAll(
                            () -> assertEquals(30, lines.size()),
                            () -> assertEquals(4, layout.render(screen(0, 0), 120, 1).size())
        );
    }

    @Test
    @DisplayName("the header carries the breadcrumb and the connected badge")
    void headerAndBadge() {
        var header = layout.render(screen(0, 3), 120, 10).getFirst().toString();
        assertAll(
                            () -> assertTrue(header.startsWith("Running work \u00b7 projects"), header),
                            () -> assertTrue(header.endsWith("\u25cf connected"), header)
        );
    }

    @Test
    @DisplayName("a disconnected view says reconnecting instead")
    void disconnectedBadge() {
        var disconnected = new TuiScreen("Running work", TuiLevel.Kind.OVERVIEW,
                            List.of(TuiRow.leaf("a", "alpha")), 0, "", false, false, 0);
        var header = layout.render(disconnected, 120, 10).getFirst().toString();
        assertTrue(header.endsWith("\u25cb reconnecting"), header);
    }

    @Test
    @DisplayName("the selected row is marked and drawn inverted, the others are not")
    void selectedRowIsMarkedAndInverted() {
        var lines = layout.render(screen(1, 3), 120, 12);
        var selected = lines.stream().filter(line -> line.toString().contains("row number 1"))
                            .findFirst().orElseThrow();
        var other = lines.stream().filter(line -> line.toString().contains("row number 2"))
                            .findFirst().orElseThrow();
        assertAll(
                            () -> assertTrue(selected.toString().startsWith("\u25b6 "), selected.toString()),
                            () -> assertTrue(selected.toAnsi().contains("[7m"), selected.toAnsi()),
                            () -> assertFalse(other.toAnsi().contains("[7m"), other.toAnsi())
        );
    }

    @Test
    @DisplayName("the footer comes from the key registry")
    void footerFromRegistry() {
        var footer = layout.render(screen(0, 2), 120, 10).getLast().toString();
        assertAll(
                            () -> assertTrue(footer.contains("move"), footer),
                            () -> assertTrue(footer.contains("open"), footer),
                            () -> assertTrue(footer.contains("3 reports"), footer)
        );
    }

    @Nested
    @DisplayName("width thresholds")
    class Widths {

        private boolean detailsVisible(int width) {
            return layout.render(screen(0, 2), width, 12).stream()
                                .anyMatch(line -> line.toString().contains("details of row 0"));
        }

        @Test
        @DisplayName("100 columns and more draw the details line")
        void wideDrawsDetails() {
            assertTrue(detailsVisible(120));
            assertTrue(detailsVisible(TuiLayout.DETAIL_COLUMNS));
        }

        @Test
        @DisplayName("below 100 columns the details line is left out")
        void narrowOmitsDetails() {
            assertFalse(detailsVisible(99));
            assertFalse(detailsVisible(TuiLayout.COMPACT_COLUMNS));
            assertFalse(detailsVisible(TuiLayout.MIN_COLUMNS));
        }
    }

    @Test
    @DisplayName("a body longer than the screen says how many rows are above and below")
    void scrollIndicators() {
        var lines = layout.render(screen(20, 40), 120, 12);
        var text = String.join("\n", lines.stream().map(Object::toString).toList());
        assertAll(
                            () -> assertTrue(text.contains("more"), text),
                            () -> assertTrue(text.contains("\u2191"), text),
                            () -> assertTrue(text.contains("\u2193"), text)
        );
    }

    @Test
    @DisplayName("the filter bar shows the filter text")
    void filterBar() {
        var filtered = new TuiScreen("Running work", TuiLevel.Kind.OVERVIEW,
                            List.of(TuiRow.leaf("a", "alpha")), 0, "alp", false, true, 0);
        var body = layout.render(filtered, 120, 12).get(2).toString();
        assertTrue(body.startsWith("/ alp"), body);
    }

    @Test
    @DisplayName("the help overlay shows the selected row's details and the keys")
    void helpOverlay() {
        var help = new TuiScreen("Running work", TuiLevel.Kind.OVERVIEW, screen(0, 3).rows(), 0, "",
                            true, true, 0);
        var text = String.join("\n", layout.render(help, 120, 12).stream().map(Object::toString).toList());
        assertAll(
                            () -> assertTrue(text.contains("details of row 0"), text),
                            () -> assertTrue(text.contains("help"), text)
        );
    }

    @Nested
    @DisplayName("truncation")
    class Truncation {

        @Test
        @DisplayName("cuts at the column budget and ends in an ellipsis")
        void cutsWithEllipsis() {
            assertAll(
                                () -> assertEquals("hello w\u2026", TuiLayout.truncate("hello world", 8)),
                                () -> assertEquals("abc", TuiLayout.truncate("abc", 10)),
                                () -> assertEquals("\u2026", TuiLayout.truncate("abc", 1)),
                                () -> assertEquals("", TuiLayout.truncate("abc", 0))
            );
        }

        @Test
        @DisplayName("never splits a surrogate pair")
        void keepsSurrogatePairs() {
            var truncated = TuiLayout.truncate("\uD83D\uDE00\uD83D\uDE00", 3);
            assertEquals("\uD83D\uDE00\u2026", truncated);
            assertEquals(3, TuiLayout.displayWidth(truncated));
        }

        @Test
        @DisplayName("counts a wide glyph as two columns and never splits it")
        void countsWideGlyphs() {
            var truncated = TuiLayout.truncate("\u6f22\u5b57\u6f22", 4);
            assertTrue(truncated.startsWith("\u6f22"), truncated);
            assertTrue(TuiLayout.displayWidth(truncated) <= 4, truncated);
            assertEquals(2, TuiLayout.displayWidth("\u6f22"));
            assertEquals(2, TuiLayout.displayWidth("\uD83D\uDE00"));
        }

        @Test
        @DisplayName("sanitizes control sequences and line breaks before it cuts")
        void sanitizesBeforeCutting() {
            assertAll(
                                () -> assertEquals("ab", TuiLayout.sanitizeCell("a\u001b[31mb")),
                                () -> assertEquals("a b", TuiLayout.sanitizeCell("a\nb")),
                                () -> assertEquals("ab  ", TuiLayout.pad("ab", 4))
            );
        }
    }
}
