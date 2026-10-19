/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * Reads a secret from the terminal with the echo off, character by character, into a {@code char[]}
 * that the caller clears after the request (04-console.adoc#line-editor). It never goes through
 * {@code LineReader.readLine}, whose result is a {@code String} that cannot be cleared and whose
 * history must never see a secret; nothing of what is typed is echoed, only one {@code *} per
 * character, and the buffer is filled with {@code '\0'} when the secret is discarded.
 */
public final class SecretReader {

    private static final int CTRL_C = 3;
    private static final int BACKSPACE = 8;
    private static final int DELETE = 127;

    private SecretReader() {
        // Utility class
    }

    /**
     * Reads a secret with the echo off.
     *
     * @param terminal the terminal the secret is read from
     * @param out      the writer the prompt and the mask are written to
     * @param locale   the interface locale, for the prompt
     * @return the secret in a fresh {@code char[]}; the caller must dispose of it with
     *         {@link #dispose(char[])}
     * @throws IOException           if the terminal cannot be read
     * @throws UserInterruptException if the operator presses {@code Ctrl+C} while typing
     */
    public static char[] read(Terminal terminal, PrintWriter out, String locale) throws IOException {
        out.print(MessageCatalogue.format(locale, "operator.credentials.secret_prompt", Map.of()));
        out.flush();
        var buffer = new Buffer();
        var originalAttributes = terminal.getAttributes();
        terminal.enterRawMode();
        try {
            fill(terminal.reader(), out, buffer);
        } catch (UserInterruptException e) {
            buffer.clear();
            throw e;
        } finally {
            terminal.setAttributes(originalAttributes);
        }
        out.println();
        out.flush();
        return buffer.take();
    }

    private static void fill(NonBlockingReader reader, PrintWriter out, Buffer buffer)
                        throws IOException {
        boolean done = false;
        while (!done) {
            int c = reader.read();
            if (c < 0 || c == '\r' || c == '\n') {
                done = true;
            } else if (c == CTRL_C) {
                throw new UserInterruptException("secret entry cancelled");
            } else if ((c == BACKSPACE || c == DELETE) && buffer.length() > 0) {
                buffer.backspace();
                out.print("\b \b");
                out.flush();
            } else if (c >= 32) {
                buffer.add((char) c);
                out.print('*');
                out.flush();
            }
        }
    }

    /** A growable secret buffer that is cleared after the request. */
    private static final class Buffer {

        private char[] data = new char[64];
        private int length;

        void add(char c) {
            if (length == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[length++] = c;
        }

        void backspace() {
            data[--length] = '\0';
        }

        int length() {
            return length;
        }

        char[] take() {
            var secret = Arrays.copyOf(data, length);
            clear();
            return secret;
        }

        void clear() {
            Arrays.fill(data, '\0');
            length = 0;
        }
    }

    /**
     * Clears a secret read by {@link #read}.
     *
     * @param secret the buffer to clear, may be {@code null}
     */
    public static void dispose(char @Nullable [] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }
}
