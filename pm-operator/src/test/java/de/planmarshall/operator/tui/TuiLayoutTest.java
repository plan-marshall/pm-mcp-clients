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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    @DisplayName("a banner stands below the header without changing the height")
    void bannerBelowHeader() {
        var withBanner = new TuiScreen("Running work", TuiLevel.Kind.OVERVIEW, screen(0, 3).rows(), 0, "",
                            false, true, 0, null, null, "Stopping when what runs is done \u00b7 2 left");
        var lines = layout.render(withBanner, 120, 12);
        assertAll(
                            () -> assertEquals(12, lines.size()),
                            () -> assertTrue(lines.get(0).toString().contains("Running work")),
                            () -> assertTrue(lines.get(1).toString().contains("\u2500")),
                            () -> assertTrue(lines.get(2).toString().contains("Stopping when what runs is done"))
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

        @Test
        @DisplayName("the system view draws its details from 80 columns on")
        void systemDrawsDetailsFromEighty() {
            var row = new TuiRow("sys:runtime", "Runtime   running", "memory 412 MB", false, true);
            var system = new TuiScreen("System \u00b7 pm-mcpd 0.9.1", TuiLevel.Kind.SYSTEM, List.of(row), 0, "",
                                false, true, 0);
            var wide = layout.render(system, TuiLayout.COMPACT_COLUMNS, 12).stream()
                                .map(Object::toString).toList();
            var narrow = layout.render(system, TuiLayout.COMPACT_COLUMNS - 1, 12).stream()
                                .map(Object::toString).toList();
            assertAll(
                                () -> assertTrue(wide.stream().anyMatch(line -> line.contains("memory 412 MB")),
                                                    wide.toString()),
                                () -> assertFalse(narrow.stream().anyMatch(line -> line.contains("memory 412 MB")),
                                                    narrow.toString())
            );
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
    @DisplayName("the scroll indicators never hide the selected row")
    void indicatorsKeepSelectionVisible() {
        for (int selected : List.of(0, 25, 39)) {
            var text = String.join("\n", layout.render(screen(selected, 40), 80, 12).stream()
                                .map(Object::toString).toList());
            assertTrue(text.contains("\u25b6 row number " + selected), text);
        }
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
    @DisplayName("the help overlay is a box with the grouped keys, the row's details and an Esc close footer")
    void helpOverlay() {
        var help = new TuiScreen("Running work", TuiLevel.Kind.OVERVIEW, screen(0, 3).rows(), 0, "",
                            true, true, 0);
        var lines = layout.render(help, 120, 20).stream().map(Object::toString).toList();
        var text = String.join("\n", lines);
        var box = lines.stream().filter(line -> line.startsWith("\u250c") || line.startsWith("\u2502")
                            || line.startsWith("\u2514")).toList();
        assertAll(
                            () -> assertTrue(text.contains("\u250c keys "), text),
                            () -> assertTrue(text.contains("\u2502 move"), text),
                            () -> assertTrue(text.contains("\u2502 open"), text),
                            () -> assertTrue(text.contains("\u2502 actions"), text),
                            () -> assertTrue(text.contains("\u2502 views"), text),
                            () -> assertTrue(text.contains("\u2502 leave"), text),
                            () -> assertTrue(text.contains("details of row 0"), text),
                            () -> assertTrue(box.size() >= 7, text),
                            () -> box.forEach(line -> assertEquals(119, TuiLayout.displayWidth(line), line)),
                            () -> assertEquals(TuiKeyRegistry.helpFooter("en"), lines.getLast().strip())
        );
    }

    @Test
    @DisplayName("at 60 columns the help overlay wraps its groups instead of cutting a key")
    void helpOverlayAtSixtyColumns() {
        var help = new TuiScreen("Running work", TuiLevel.Kind.PROJECT_BY_PLAN, screen(0, 3).rows(),
                            0, "", true, true, 0);
        var lines = layout.render(help, 60, 24).stream().map(Object::toString).toList();
        var box = lines.stream().filter(line -> line.startsWith("\u2502")).toList();
        var tokens = box.stream()
                            .flatMap(line -> Arrays.stream(line.replace("\u2502", " ").split("\\s+")))
                            .filter(token -> !token.isEmpty())
                            .collect(Collectors.toSet());

        assertAll(
                            () -> assertTrue(box.size() > 7, String.join("\n", box)),
                            () -> assertTrue(box.stream().noneMatch(line -> line.contains("\u2026")),
                                                String.join("\n", box)),
                            () -> TuiKeyRegistry.bindings(TuiLevel.Kind.PROJECT_BY_PLAN).stream()
                                                .map(binding -> TuiKeyRegistry.displayOf(binding.trigger()))
                                                .forEach(display -> assertTrue(tokens.contains(display),
                                                                    "the overlay does not name " + display))
        );
    }

    @Test
    @DisplayName("at 80 columns every level's footer keeps its priority keys and names ? help or ? more")
    void footersAtEightyColumns() {
        var overview = layout.render(new TuiScreen("Running work", TuiLevel.Kind.OVERVIEW,
                            screen(0, 3).rows(), 0, "", false, true, 5), 80, 24).getLast().toString().strip();
        var activity = layout.render(new TuiScreen("fix-relay", TuiLevel.Kind.PLAN_ACTIVITY,
                            screen(0, 3).rows(), 0, "", false, true, 0), 80, 24).getLast().toString().strip();
        var plan = layout.render(new TuiScreen("fix-relay", TuiLevel.Kind.PLAN, screen(0, 3).rows(),
                                                0, "", false, true, 0, TuiKeyRegistry.footer(TuiLevel.Kind.PLAN, "en", 0)),
                            80, 24).getLast().toString().strip();

        assertAll(
                            () -> assertTrue(overview.contains("? help"), overview),
                            () -> assertTrue(overview.contains("q leave"), overview),
                            () -> assertTrue(activity.contains("Enter open"), activity),
                            () -> assertTrue(activity.contains("a actions"), activity),
                            () -> assertTrue(activity.contains("? help"), activity),
                            () -> assertTrue(plan.contains("Enter open"), plan),
                            () -> assertTrue(plan.contains("Esc back"), plan),
                            () -> assertTrue(plan.contains("a actions"), plan),
                            () -> assertTrue(plan.contains("q leave"), plan),
                            () -> assertTrue(plan.endsWith("? more"), plan),
                            () -> assertFalse(plan.contains("\u2026"), plan),
                            () -> assertFalse(overview.contains("\u2026"), overview),
                            () -> assertFalse(activity.contains("\u2026"), activity)
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
