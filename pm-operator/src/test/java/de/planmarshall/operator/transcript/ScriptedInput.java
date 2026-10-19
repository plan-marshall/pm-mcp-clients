/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The scripted keys of a transcript test: the reader consumes them in the order the test typed
 * them and waits for the next key when the script is exhausted. A read without further script
 * ends the stream after a bounded wait, so a dialogue that asks one question more than the test
 * answered fails like an end of file instead of hanging the build.
 */
final class ScriptedInput extends InputStream {

    /** The longest a dialogue may wait for a key the test did not script. */
    private static final long READ_TIMEOUT_MILLIS = 10_000L;

    private final Deque<byte[]> typed = new ArrayDeque<>();
    private int offset;
    private boolean endOfScript;

    /**
     * Appends keys to the script.
     *
     * @param keys the keys, in the encoding of the terminal
     */
    synchronized void type(String keys) {
        if (!keys.isEmpty()) {
            typed.add(keys.getBytes(StandardCharsets.UTF_8));
            notifyAll();
        }
    }

    /** Ends the script: a reader that looks for more keys sees an end of file. */
    synchronized void endOfScript() {
        endOfScript = true;
        notifyAll();
    }

    @Override
    public synchronized int read() {
        var deadline = System.nanoTime() + READ_TIMEOUT_MILLIS * 1_000_000L;
        while (typed.isEmpty() && !endOfScript) {
            var remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
                return -1;
            }
            try {
                wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
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
