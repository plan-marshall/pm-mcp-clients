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
import java.util.Map;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.WCWidth;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.OutputSafety;

/**
 * Draws a {@link TuiScreen} as terminal lines: a header with the breadcrumb and the connection
 * badge, a scrollable body with the selected row marked {@code ▶} and inverted, the {@code ↑ n
 * more} and {@code ↓ n more} indicators, the {@code /} filter bar, the {@code ?} help and details
 * overlay, and a footer generated from {@link TuiKeyRegistry}.
 *
 * <p>Every text that comes from the runtime passes through {@link OutputSafety} before it is
 * drawn, and every cell is truncated to the terminal's column width without splitting a surrogate
 * pair or a wide glyph. The renderer knows nothing about terminals, keys, or the runtime; it turns
 * a screen description into lines, which is what makes it testable without a terminal.
 */
public final class TuiLayout {

    /** Below this width the view does not open; the caller prints the tree instead. */
    public static final int MIN_COLUMNS = 60;

    /** From this width the details line of a row is drawn below it. */
    public static final int DETAIL_COLUMNS = 100;

    /** Below this width only the row line is drawn. */
    public static final int COMPACT_COLUMNS = 80;

    private static final int HEADER_LINES = 2;
    private static final int FOOTER_LINES = 2;
    private static final int MIN_HEIGHT = HEADER_LINES + FOOTER_LINES;
    private static final String CURSOR = "\u25b6 ";
    private static final String INDENT = "  ";
    private static final String DETAIL_INDENT = "    ";
    private static final String ELLIPSIS = "\u2026";
    private static final String RULE = "\u2500";
    private static final String CONNECTED = "\u25cf ";
    private static final String DISCONNECTED = "\u25cb ";

    private final String locale;

    /**
     * @param locale the interface locale
     */
    public TuiLayout(String locale) {
        this.locale = MessageCatalogue.normalizeLocale(locale);
    }

    /**
     * @param screen  what to draw
     * @param columns the terminal's width in columns
     * @param rows    the terminal's height in lines
     * @return exactly {@code rows} lines (never fewer than four)
     */
    public List<AttributedString> render(TuiScreen screen, int columns, int rows) {
        // The last cell of a line stays empty: a terminal that wraps at the last column would
        // otherwise move the cursor to the next line and tear the layout.
        int width = Math.max(1, columns - 1);
        int height = Math.max(MIN_HEIGHT, rows);
        var lines = new ArrayList<AttributedString>(height);
        lines.add(header(screen, width));
        lines.add(rule(width));
        lines.addAll(body(screen, width, height - HEADER_LINES - FOOTER_LINES));
        lines.add(rule(width));
        lines.add(footer(screen, width));
        return List.copyOf(lines);
    }

    /**
     * @param text         the raw text
     * @param maxColumns   the columns the text may use
     * @return the sanitized text, cut at a code-point boundary with a trailing {@code …}
     */
    public static String truncate(String text, int maxColumns) {
        var safe = sanitizeCell(text);
        if (maxColumns <= 0) {
            return "";
        }
        if (displayWidth(safe) <= maxColumns) {
            return safe;
        }
        int budget = maxColumns - displayWidth(ELLIPSIS);
        if (budget <= 0) {
            return ELLIPSIS;
        }
        var sb = new StringBuilder();
        int used = 0;
        for (int index = 0; index < safe.length(); ) {
            int codePoint = safe.codePointAt(index);
            int cellWidth = Math.max(0, WCWidth.wcwidth(codePoint));
            if (used + cellWidth > budget) {
                break;
            }
            sb.appendCodePoint(codePoint);
            used += cellWidth;
            index += Character.charCount(codePoint);
        }
        return sb.append(ELLIPSIS).toString();
    }

    /**
     * @param text the raw text of a cell
     * @return the text without control sequences or line breaks
     */
    public static String sanitizeCell(String text) {
        return OutputSafety.stripControlSequences(text).replace('\n', ' ').replace('\t', ' ');
    }

    /**
     * @param text the text
     * @return the number of terminal columns the text occupies
     */
    public static int displayWidth(String text) {
        int width = 0;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            width += Math.max(0, WCWidth.wcwidth(codePoint));
            index += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * @param text    the text
     * @param columns the target width
     * @return the text padded with spaces to exactly {@code columns} display columns
     */
    public static String pad(String text, int columns) {
        var safe = truncate(text, columns);
        int padding = columns - displayWidth(safe);
        return padding <= 0 ? safe : safe + " ".repeat(padding);
    }

    private AttributedString header(TuiScreen screen, int width) {
        var badgeText = screen.connected()
                            ? CONNECTED + MessageCatalogue.format(locale, "operator.overview.connected", Map.of())
                            : DISCONNECTED + MessageCatalogue.format(locale, "operator.console.badge.reconnecting",
                            Map.of());
        var left = truncate(screen.breadcrumb(), Math.max(0, width - displayWidth(badgeText) - 1));
        int gap = Math.max(1, width - displayWidth(left) - displayWidth(badgeText));
        return new AttributedString(left + " ".repeat(gap) + badgeText);
    }

    private AttributedString rule(int width) {
        return new AttributedString(RULE.repeat(Math.max(0, width)));
    }

    private AttributedString footer(TuiScreen screen, int width) {
        var text = TuiKeyRegistry.footer(screen.kind(), locale, screen.reportsCount());
        return new AttributedString(truncate(text, width));
    }

    private List<AttributedString> body(TuiScreen screen, int width, int height) {
        var lines = screen.help() ? helpLines(screen, width) : rowLines(screen, width);
        return viewport(lines, screen, width, height);
    }

    private static final class BodyLine {
        private final AttributedString text;
        private final int rowIndex;

        private BodyLine(AttributedString text, int rowIndex) {
            this.text = text;
            this.rowIndex = rowIndex;
        }
    }

    private List<BodyLine> helpLines(TuiScreen screen, int width) {
        var lines = new ArrayList<BodyLine>();
        var selected = screen.selected();
        if (selected != null) {
            lines.add(new BodyLine(new AttributedString(truncate(INDENT + selected.line(), width)), 0));
            if (selected.details() != null) {
                lines.add(new BodyLine(new AttributedString(truncate(DETAIL_INDENT + selected.details(), width)), 0));
            }
        }
        lines.add(new BodyLine(new AttributedString(truncate(
                            TuiKeyRegistry.footer(screen.kind(), locale, screen.reportsCount()), width)), 0));
        return lines;
    }

    private List<BodyLine> rowLines(TuiScreen screen, int width) {
        var lines = new ArrayList<BodyLine>();
        if (!screen.filter().isEmpty()) {
            lines.add(new BodyLine(new AttributedString(truncate("/ " + screen.filter(), width)), -1));
        }
        var rows = screen.rows();
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            boolean selected = index == screen.selectedIndex();
            var text = cursor(selected) + row.line();
            if (selected) {
                lines.add(new BodyLine(new AttributedString(pad(text, width), AttributedStyle.DEFAULT.inverse()), index));
            } else {
                lines.add(new BodyLine(new AttributedString(truncate(text, width)), index));
            }
            if (width + 1 >= DETAIL_COLUMNS && row.details() != null && !row.details().isBlank()) {
                lines.add(new BodyLine(new AttributedString(truncate(DETAIL_INDENT + row.details(), width)), index));
            }
        }
        return lines;
    }

    private static String cursor(boolean selected) {
        return selected ? CURSOR : INDENT;
    }

    private List<AttributedString> viewport(List<BodyLine> lines, TuiScreen screen, int width, int height) {
        int total = lines.size();
        if (total <= height) {
            var filled = new ArrayList<AttributedString>(height);
            for (var line : lines) {
                filled.add(line.text);
            }
            while (filled.size() < height) {
                filled.add(AttributedString.EMPTY);
            }
            return filled;
        }
        int selected = Math.max(0, screen.selectedIndex());
        int start = 0;
        for (int i = 0; i < total; i++) {
            if (lines.get(i).rowIndex == selected) {
                start = i;
                break;
            }
        }
        int end = start;
        for (int i = start; i < total && lines.get(i).rowIndex == selected; i++) {
            end = i;
        }
        int offset = 0;
        if (end >= height) {
            offset = end - height + 1;
        }
        if (start < offset) {
            offset = start;
        }
        offset = Math.max(0, Math.min(offset, total - height));
        var view = new ArrayList<AttributedString>(height);
        for (int i = 0; i < height; i++) {
            view.add(lines.get(offset + i).text);
        }
        int above = rowsAbove(lines, offset);
        int below = rowsBelow(lines, offset + height, total, screen.rows().size());
        if (above > 0) {
            view.set(0, new AttributedString(truncate(indicator("operator.overview.more_above", above), width)));
        }
        if (below > 0) {
            view.set(height - 1, new AttributedString(truncate(indicator("operator.overview.more_below", below), width)));
        }
        return view;
    }

    private String indicator(String key, int count) {
        return MessageCatalogue.format(locale, key, Map.of("count", count));
    }

    private static int rowsAbove(List<BodyLine> lines, int offset) {
        int first = lines.get(offset).rowIndex;
        if (first < 0) {
            return 0;
        }
        int rows = 0;
        for (int i = 0; i < offset; i++) {
            if (lines.get(i).rowIndex >= 0 && (i == 0 || lines.get(i - 1).rowIndex != lines.get(i).rowIndex)) {
                rows++;
            }
        }
        return rows;
    }

    private static int rowsBelow(List<BodyLine> lines, int offset, int total, int rowCount) {
        if (offset >= total) {
            return 0;
        }
        int last = lines.get(total - 1).rowIndex;
        if (last < 0) {
            return 0;
        }
        int rows = 0;
        int lastCounted = -1;
        for (int i = offset; i < total; i++) {
            int rowIndex = lines.get(i).rowIndex;
            if (rowIndex >= 0 && rowIndex != lastCounted) {
                rows++;
                lastCounted = rowIndex;
            }
        }
        int hidden = rowCount - rows;
        return Math.max(0, hidden);
    }
}
