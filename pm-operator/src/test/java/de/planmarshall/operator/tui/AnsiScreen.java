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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.jline.utils.WCWidth;

/**
 * One parsed terminal screen: the visible lines, the rows drawn inverted, whether the alternate
 * screen is active, how often it was entered and left, and whether the cursor is visible.
 *
 * <p>The parser understands the subset of ANSI that {@code Display}, an {@code xterm} terminal,
 * and the console hand-off use: cursor addressing and movement, erase in line and display, SGR
 * (inverse, bold, dim, and the foreground colour index), the private modes 1049/47/1047 (alternate
 * screen) and 25 (cursor), OSC, and the string introducers. Everything else is skipped.
 *
 * <p>The same parser serves the in-memory {@link VirtualScreenTerminal} and the real
 * {@code PtySession}: both feed it the bytes the process wrote and assert the visible result the
 * same way.
 *
 * @param lines                 the right-trimmed lines
 * @param invertedRows          the rows with at least one inverted cell
 * @param alternateScreenActive whether the alternate screen is active
 * @param alternation           how often the alternate screen was entered and left
 * @param cursorVisible         whether the cursor is visible
 * @param cellStyles            per row, the attributes of every cell
 */
public record AnsiScreen(List<String> lines, Set<Integer> invertedRows, boolean alternateScreenActive,
Alternation alternation, boolean cursorVisible, List<List<CellStyle>> cellStyles) {

    /**
     * The attributes of one cell, as the screen capture records them.
     *
     * @param bold    whether the cell is bold
     * @param dim     whether the cell is dim
     * @param reverse whether the cell is drawn in reverse
     * @param colour  the foreground colour index, or {@code -1} for the terminal's default
     */
    public record CellStyle(boolean bold, boolean dim, boolean reverse, int colour) {

        /** The style of a cell the terminal drew without attributes. */
        public static final CellStyle PLAIN = new CellStyle(false, false, false, -1);

        /**
         * @return the compact run text, for example {@code -}, {@code b}, {@code br4}
         */
        public String text() {
            var text = new StringBuilder();
            if (bold) {
                text.append('b');
            }
            if (dim) {
                text.append('d');
            }
            if (reverse) {
                text.append('r');
            }
            if (colour >= 0) {
                text.append('c').append(colour);
            }
            return text.isEmpty() ? "-" : text.toString();
        }
    }

    /**
     * @param entries the number of alternate-screen entries
     * @param exits   the number of alternate-screen exits
     */
    public record Alternation(int entries, int exits) {
    }

    /**
     * @param ansi    everything written to the terminal
     * @param columns the width
     * @param rows    the height
     * @return the parsed screen
     */
    public static AnsiScreen parse(String ansi, int columns, int rows) {
        var parser = new Parser(columns, rows);
        parser.feed(ansi);
        return parser.snapshot();
    }

    private static final class Parser {

        private static final int GROUND = 0;
        private static final int ESCAPE = 1;
        private static final int CSI = 2;
        private static final int OSC = 3;
        private static final int STRING = 4;
        private static final int CHARSET = 5;

        private final int columns;
        private final int rows;
        private final String[] cells;
        private final boolean[] inverse;
        private final boolean[] bold;
        private final boolean[] dim;
        private final int[] colour;
        private final boolean[] continuation;
        private final Set<Integer> invertedRows = new TreeSet<>();
        private final StringBuilder csi = new StringBuilder();
        private final StringBuilder osc = new StringBuilder();
        private int row;
        private int column;
        private int state = GROUND;
        private boolean inverseStyle;
        private boolean boldStyle;
        private boolean dimStyle;
        private int colourIndex = -1;
        private boolean alternateCharacterSet;
        private boolean cursorVisible = true;
        private boolean alternate;
        private int entries;
        private int exits;

        private Parser(int columns, int rows) {
            this.columns = Math.max(1, columns);
            this.rows = Math.max(1, rows);
            cells = new String[this.columns * this.rows];
            inverse = new boolean[this.columns * this.rows];
            bold = new boolean[this.columns * this.rows];
            dim = new boolean[this.columns * this.rows];
            colour = new int[this.columns * this.rows];
            Arrays.fill(colour, -1);
            continuation = new boolean[this.columns * this.rows];
        }

        private void feed(String text) {
            for (int index = 0; index < text.length(); ) {
                int codePoint = text.codePointAt(index);
                index += Character.charCount(codePoint);
                process(codePoint);
            }
        }

        private void process(int codePoint) {
            if (state == OSC) {
                if (codePoint == 7) {
                    state = GROUND;
                } else if (codePoint == 27) {
                    state = ESCAPE;
                } else {
                    osc.appendCodePoint(codePoint);
                }
                return;
            }
            if (state == STRING) {
                if (codePoint == 0x9c) {
                    state = GROUND;
                } else if (codePoint == 27) {
                    state = ESCAPE;
                }
                return;
            }
            if (state == CHARSET) {
                if (codePoint == '0') {
                    alternateCharacterSet = true;
                } else if (codePoint == 'B') {
                    alternateCharacterSet = false;
                }
                state = GROUND;
                return;
            }
            if (state == ESCAPE) {
                if (codePoint == '(' || codePoint == ')' || codePoint == '*' || codePoint == '+'
                                    || codePoint == '-' || codePoint == '.' || codePoint == '/'
                                    || codePoint == '#' || codePoint == '%') {
                    state = CHARSET;
                    return;
                }
                state = switch (codePoint) {
                    case '[' -> {
                        csi.setLength(0);
                        yield CSI;
                    }
                    case ']' -> {
                        osc.setLength(0);
                        yield OSC;
                    }
                    case 'P', 'X', '^', '_' -> STRING;
                    default -> GROUND;
                };
                return;
            }
            if (state == CSI) {
                if (codePoint >= 0x40 && codePoint <= 0x7e) {
                    csiCommand((char) codePoint, csi.toString());
                    state = GROUND;
                } else {
                    csi.appendCodePoint(codePoint);
                }
                return;
            }
            if (codePoint == 27) {
                state = ESCAPE;
                return;
            }
            if (codePoint == '\r') {
                column = 0;
            } else if (codePoint == '\n') {
                lineFeed();
            } else if (codePoint == '\b') {
                column = Math.max(0, column - 1);
            } else if (codePoint == '\t') {
                column = Math.min(columns - 1, column + 8 - column % 8);
            } else if (codePoint >= 0x20) {
                write(codePoint);
            }
        }

        private void write(int codePoint) {
            if (alternateCharacterSet) {
                codePoint = decodeAlternate(codePoint);
            }
            int width = Math.max(1, WCWidth.wcwidth(codePoint));
            if (column >= columns) {
                lineFeed();
                column = 0;
            }
            int cell = row * columns + column;
            cells[cell] = new String(Character.toChars(codePoint));
            inverse[cell] = inverseStyle;
            bold[cell] = boldStyle;
            dim[cell] = dimStyle;
            colour[cell] = colourIndex;
            continuation[cell] = false;
            if (inverseStyle) {
                invertedRows.add(row);
            }
            if (width == 2 && column + 1 < columns) {
                cells[cell + 1] = null;
                inverse[cell + 1] = inverseStyle;
                bold[cell + 1] = boldStyle;
                dim[cell + 1] = dimStyle;
                colour[cell + 1] = colourIndex;
                continuation[cell + 1] = true;
            }
            column = Math.min(columns, column + width);
        }

        private static int decodeAlternate(int codePoint) {
            return switch (codePoint) {
                case 'q' -> '\u2500';
                case 'x' -> '\u2502';
                case 'l' -> '\u250c';
                case 'k' -> '\u2510';
                case 'm' -> '\u2514';
                case 'j' -> '\u2518';
                case 't' -> '\u251c';
                case 'u' -> '\u2524';
                case 'n' -> '\u253c';
                default -> codePoint;
            };
        }

        private void lineFeed() {
            if (row + 1 < rows) {
                row++;
            } else {
                scrollUp();
            }
        }

        private void scrollUp() {
            System.arraycopy(cells, columns, cells, 0, columns * (rows - 1));
            System.arraycopy(inverse, columns, inverse, 0, columns * (rows - 1));
            System.arraycopy(bold, columns, bold, 0, columns * (rows - 1));
            System.arraycopy(dim, columns, dim, 0, columns * (rows - 1));
            System.arraycopy(colour, columns, colour, 0, columns * (rows - 1));
            System.arraycopy(continuation, columns, continuation, 0, columns * (rows - 1));
            for (int cell = columns * (rows - 1); cell < columns * rows; cell++) {
                cells[cell] = null;
                inverse[cell] = false;
                bold[cell] = false;
                dim[cell] = false;
                colour[cell] = -1;
                continuation[cell] = false;
            }
            recomputeInvertedRows();
        }

        private void recomputeInvertedRows() {
            invertedRows.clear();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    if (inverse[r * columns + c]) {
                        invertedRows.add(r);
                        break;
                    }
                }
            }
        }

        private void csiCommand(char command, String parameters) {
            boolean privateMode = parameters.startsWith("?");
            var raw = privateMode ? parameters.substring(1) : parameters;
            switch (command) {
                case 'H', 'f' -> position(raw);
                case 'A' -> row = Math.max(0, row - number(raw, 1));
                case 'B' -> row = Math.min(rows - 1, row + number(raw, 1));
                case 'C' -> column = Math.min(columns - 1, column + number(raw, 1));
                case 'D' -> column = Math.max(0, column - number(raw, 1));
                case 'G' -> column = Math.clamp(number(raw, 1) - 1, 0, columns - 1);
                case 'd' -> row = Math.clamp(number(raw, 1) - 1, 0, rows - 1);
                case 'J' -> eraseDisplay(number(raw, 0));
                case 'K' -> eraseLine(number(raw, 0));
                case 'm' -> style(raw);
                case 'h' -> setMode(privateMode, raw, true);
                case 'l' -> setMode(privateMode, raw, false);
                default -> {
                    // Scroll regions, tabs, and the rest do not change the text of a screen.
                }
            }
        }

        private void position(String raw) {
            var parts = raw.isEmpty() ? new String[0] : raw.split(";", -1);
            int targetRow = parts.length > 0 ? Math.max(1, parseInt(parts[0], 1)) : 1;
            int targetColumn = parts.length > 1 ? Math.max(1, parseInt(parts[1], 1)) : 1;
            row = Math.clamp(targetRow - 1, 0, rows - 1);
            column = Math.clamp(targetColumn - 1, 0, columns - 1);
        }

        private void style(String raw) {
            if (raw.isEmpty()) {
                resetStyle();
                return;
            }
            var parts = raw.split(";", -1);
            for (int index = 0; index < parts.length; index++) {
                var part = parts[index];
                if ("38".equals(part) || "48".equals(part)) {
                    index += extendedColourOperands(parts, index);
                    continue;
                }
                switch (part) {
                    case "0", "" -> resetStyle();
                    case "1" -> boldStyle = true;
                    case "2" -> dimStyle = true;
                    case "22" -> {
                        boldStyle = false;
                        dimStyle = false;
                    }
                    case "7" -> inverseStyle = true;
                    case "27" -> inverseStyle = false;
                    case "39" -> colourIndex = -1;
                    default -> colourIndex = colourOf(part, colourIndex);
                }
            }
        }

        private void resetStyle() {
            inverseStyle = false;
            boldStyle = false;
            dimStyle = false;
            colourIndex = -1;
        }

        /**
         * @param parts the SGR parameters
         * @param index the position of the {@code 38} or {@code 48} introducer
         * @return how many following parameters the extended colour consumes
         */
        private static int extendedColourOperands(String[] parts, int index) {
            if (index + 1 >= parts.length) {
                return 0;
            }
            return switch (parts[index + 1]) {
                case "5" -> Math.min(2, parts.length - index - 1);
                case "2" -> Math.min(4, parts.length - index - 1);
                default -> 0;
            };
        }

        private static int colourOf(String part, int current) {
            try {
                int code = Integer.parseInt(part);
                if (code >= 30 && code <= 37) {
                    return code - 30;
                }
                if (code >= 90 && code <= 97) {
                    return code - 90 + 8;
                }
            } catch (NumberFormatException _) {
                // A parameter the screen text does not carry.
            }
            return current;
        }

        private void setMode(boolean privateMode, String raw, boolean enabled) {
            if (!privateMode) {
                return;
            }
            for (var part : raw.split(";")) {
                switch (part) {
                    case "1049", "1047", "47" -> {
                        alternate = enabled;
                        if (enabled) {
                            entries++;
                            clearAll();
                        } else {
                            exits++;
                        }
                    }
                    case "25" -> cursorVisible = enabled;
                    default -> {
                        // No other private mode changes what a test asserts.
                    }
                }
            }
        }

        private void eraseDisplay(int mode) {
            if (mode == 2 || mode == 3) {
                clearAll();
            } else if (mode == 0) {
                for (int cell = row * columns + column; cell < cells.length; cell++) {
                    clear(cell);
                }
            }
        }

        private void eraseLine(int mode) {
            int start = switch (mode) {
                case 1 -> row * columns;
                case 2 -> row * columns;
                default -> row * columns + column;
            };
            int end = mode == 1 ? row * columns + column + 1 : row * columns + columns;
            for (int cell = start; cell < Math.min(end, cells.length); cell++) {
                clear(cell);
            }
        }

        private void clearAll() {
            for (int cell = 0; cell < cells.length; cell++) {
                clear(cell);
            }
            invertedRows.clear();
        }

        private void clear(int cell) {
            cells[cell] = null;
            inverse[cell] = false;
            bold[cell] = false;
            dim[cell] = false;
            colour[cell] = -1;
            continuation[cell] = false;
        }

        private AnsiScreen snapshot() {
            var lines = new ArrayList<String>(rows);
            var styles = new ArrayList<List<CellStyle>>(rows);
            for (int r = 0; r < rows; r++) {
                var line = new StringBuilder();
                var rowStyles = new ArrayList<CellStyle>(columns);
                for (int c = 0; c < columns; c++) {
                    int cell = r * columns + c;
                    rowStyles.add(new CellStyle(bold[cell], dim[cell], inverse[cell], colour[cell]));
                    if (continuation[cell]) {
                        continue;
                    }
                    line.append(cells[cell] == null ? " " : cells[cell]);
                }
                lines.add(line.toString().stripTrailing());
                styles.add(List.copyOf(rowStyles));
            }
            return new AnsiScreen(List.copyOf(lines), Set.copyOf(invertedRows), alternate,
                                new Alternation(entries, exits), cursorVisible, List.copyOf(styles));
        }

        private static int number(String raw, int fallback) {
            if (raw == null || raw.isEmpty()) {
                return fallback;
            }
            var first = raw.split(";", -1)[0];
            return parseInt(first, fallback);
        }

        private static int parseInt(String text, int fallback) {
            if (text == null || text.isEmpty()) {
                return fallback;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException _) {
                return fallback;
            }
        }
    }
}
