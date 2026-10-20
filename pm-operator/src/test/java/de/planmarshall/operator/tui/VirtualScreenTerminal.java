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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.jline.utils.WCWidth;

/**
 * An in-memory virtual terminal for the full-screen tests: a JLine {@link ExternalTerminal} over a
 * scripted input and an ANSI parser that turns everything the view drew into screen lines.
 *
 * <p>It records what a test needs to assert about a full-screen view: the exact lines at a given
 * width and height, the rows drawn inverted (the selection), whether the alternate screen is
 * active and how often it was entered and left, whether the cursor is visible, and whether a resize
 * reached the drawing.
 *
 * <p>The parser understands the subset of ANSI that {@code Display} and the capabilities of an
 * {@code xterm} terminal use: cursor addressing and movement, erase in line and display, SGR
 * (inverse only), the private modes 1049/47/1047 (alternate screen) and 25 (cursor), OSC, and the
 * string introducers. Everything else is skipped.
 */
public final class VirtualScreenTerminal implements AutoCloseable {

    private final ScriptedInput input = new ScriptedInput();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ExternalTerminal terminal;
    private int columns;
    private int rows;
    private int mark;

    private VirtualScreenTerminal(int columns, int rows) throws IOException {
        this.columns = columns;
        this.rows = rows;
        terminal = new ExternalTerminal("pm-virtual", "xterm-256color", input, output, StandardCharsets.UTF_8);
        terminal.setSize(Size.of(columns, rows));
        // ExternalTerminal starts its pump thread on resume; without it no key ever reaches the reader.
        terminal.resume();
    }

    /**
     * @param columns the width in columns
     * @param rows    the height in lines
     * @return a terminal with an empty script and an empty screen
     * @throws IOException if the terminal cannot be created
     */
    public static VirtualScreenTerminal create(int columns, int rows) throws IOException {
        return new VirtualScreenTerminal(columns, rows);
    }

    /**
     * @param keys the keys to type verbatim
     * @return this terminal
     */
    public VirtualScreenTerminal type(String keys) {
        input.type(keys);
        return this;
    }

    /** Ends the script: a reader that looks for more keys sees an end of file. */
    public void endOfInput() {
        input.endOfScript();
    }

    /**
     * Resizes the terminal and raises the resize signal, as a real terminal does.
     *
     * @param columns the new width
     * @param rows    the new height
     */
    public void resize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.mark = output.size();
        terminal.setSize(Size.of(columns, rows));
        terminal.raise(Terminal.Signal.WINCH);
    }

    /** @return the JLine terminal the view draws on */
    public Terminal terminal() {
        return terminal;
    }

    /** @return the current width */
    public int columns() {
        return columns;
    }

    /** @return the current height */
    public int rows() {
        return rows;
    }

    /**
     * @return the raw bytes the view wrote so far
     */
    public String rawOutput() {
        return output.toString(StandardCharsets.UTF_8);
    }

    /**
     * @return the screen as it stands now
     */
    public Screen screen() {
        return Screen.parse(rawOutput(), columns, rows);
    }

    /**
     * @return the screen the view drew since the last {@link #resize(int, int)} or {@link #mark()}
     */
    public Screen screenSinceMark() {
        var bytes = output.toByteArray();
        return Screen.parse(new String(bytes, Math.min(mark, bytes.length), bytes.length - Math.min(mark, bytes.length),
                            StandardCharsets.UTF_8), columns, rows);
    }

    /** Marks the current end of the output, so that {@link #screenSinceMark()} starts here. */
    public void mark() {
        this.mark = output.size();
    }

    /**
     * @return the plain, right-trimmed lines of the screen
     */
    public List<String> lines() {
        return screen().lines();
    }

    /**
     * @return the indices of the rows with at least one inverted cell
     */
    public Set<Integer> invertedRows() {
        return screen().invertedRows();
    }

    /**
     * @return {@code true} while the alternate screen is active
     */
    public boolean alternateScreenActive() {
        return screen().alternateScreenActive();
    }

    /** @return how often the view entered and left the alternate screen */
    public Screen.Alternation alternation() {
        return screen().alternation();
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }

    /**
     * One parsed screen.
     *
     * @param lines                  the right-trimmed lines
     * @param invertedRows           the rows with at least one inverted cell
     * @param alternateScreenActive  whether the alternate screen is active
     * @param alternation            how often the alternate screen was entered and left
     * @param cursorVisible          whether the cursor is visible
     */
    public record Screen(List<String> lines, Set<Integer> invertedRows, boolean alternateScreenActive,
    Alternation alternation, boolean cursorVisible) {

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
        public static Screen parse(String ansi, int columns, int rows) {
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
            private final boolean[] continuation;
            private final Set<Integer> invertedRows = new TreeSet<>();
            private final StringBuilder csi = new StringBuilder();
            private final StringBuilder osc = new StringBuilder();
            private int row;
            private int column;
            private int state = GROUND;
            private boolean inverseStyle;
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
                continuation[cell] = false;
                if (inverseStyle) {
                    invertedRows.add(row);
                }
                if (width == 2 && column + 1 < columns) {
                    cells[cell + 1] = null;
                    inverse[cell + 1] = inverseStyle;
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
                System.arraycopy(continuation, columns, continuation, 0, columns * (rows - 1));
                for (int cell = columns * (rows - 1); cell < columns * rows; cell++) {
                    cells[cell] = null;
                    inverse[cell] = false;
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
                    inverseStyle = false;
                    return;
                }
                for (var part : raw.split(";")) {
                    switch (part) {
                        case "0", "" -> inverseStyle = false;
                        case "7" -> inverseStyle = true;
                        case "27" -> inverseStyle = false;
                        default -> {
                            // Colour is not part of the screen text.
                        }
                    }
                }
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
                continuation[cell] = false;
            }

            private Screen snapshot() {
                var lines = new ArrayList<String>(rows);
                for (int r = 0; r < rows; r++) {
                    var line = new StringBuilder();
                    for (int c = 0; c < columns; c++) {
                        int cell = r * columns + c;
                        if (continuation[cell]) {
                            continue;
                        }
                        line.append(cells[cell] == null ? " " : cells[cell]);
                    }
                    lines.add(line.toString().stripTrailing());
                }
                return new Screen(List.copyOf(lines), Set.copyOf(invertedRows), alternate,
                                    new Alternation(entries, exits), cursorVisible);
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

    /**
     * The scripted keys: the terminal reads them in order and waits for more when the script is
     * exhausted.
     */
    private static final class ScriptedInput extends InputStream {

        private final Deque<byte[]> typed = new ArrayDeque<>();
        private int offset;
        private boolean endOfScript;

        private synchronized void type(String keys) {
            if (!keys.isEmpty()) {
                typed.add(keys.getBytes(StandardCharsets.UTF_8));
                notifyAll();
            }
        }

        private synchronized void endOfScript() {
            endOfScript = true;
            notifyAll();
        }

        @Override
        public synchronized int read() {
            while (typed.isEmpty() && !endOfScript) {
                try {
                    wait(100L);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
            if (typed.isEmpty()) {
                return -1;
            }
            var chunk = typed.peek();
            var next = chunk[offset++] & 0xff;
            if (offset == chunk.length) {
                typed.poll();
                offset = 0;
            }
            return next;
        }

        @Override
        public int read(byte[] buffer, int start, int length) {
            if (length == 0) {
                return 0;
            }
            var first = read();
            if (first < 0) {
                return -1;
            }
            buffer[start] = (byte) first;
            var count = 1;
            while (count < length && available() > 0) {
                var next = read();
                if (next < 0) {
                    break;
                }
                buffer[start + count++] = (byte) next;
            }
            return count;
        }

        @Override
        public synchronized int available() {
            var queued = 0;
            for (var chunk : typed) {
                queued += chunk.length;
            }
            return queued - offset;
        }
    }
}
