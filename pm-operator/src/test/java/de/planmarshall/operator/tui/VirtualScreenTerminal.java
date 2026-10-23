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
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;

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
    private volatile boolean closed;

    private VirtualScreenTerminal(int columns, int rows) throws IOException {
        this.columns = columns;
        this.rows = rows;
        terminal = new ExternalTerminal("pm-virtual", "xterm-256color", input, output, StandardCharsets.UTF_8);
        terminal.setOnClose(() -> closed = true);
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
    public AnsiScreen screen() {
        return AnsiScreen.parse(rawOutput(), columns, rows);
    }

    /**
     * @return the screen the view drew since the last {@link #resize(int, int)} or {@link #mark()}
     */
    public AnsiScreen screenSinceMark() {
        var bytes = output.toByteArray();
        return AnsiScreen.parse(new String(bytes, Math.min(mark, bytes.length), bytes.length - Math.min(mark, bytes.length),
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
    public AnsiScreen.Alternation alternation() {
        return screen().alternation();
    }

    /** @return {@code true} once {@link #close()} has closed the JLine terminal */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        terminal.close();
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
