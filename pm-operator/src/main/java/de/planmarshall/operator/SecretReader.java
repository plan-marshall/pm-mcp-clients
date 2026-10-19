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
        var prompt = MessageCatalogue.format(locale, "operator.credentials.secret_prompt", Map.of());
        out.print(prompt);
        out.flush();
        var buffer = new char[64];
        int length = 0;
        var originalAttributes = terminal.getAttributes();
        terminal.enterRawMode();
        try {
            var reader = terminal.reader();
            while (true) {
                int c = reader.read();
                if (c < 0) {
                    break;
                }
                if (c == '\r' || c == '\n') {
                    break;
                }
                if (c == CTRL_C) {
                    Arrays.fill(buffer, '\0');
                    throw new UserInterruptException("secret entry cancelled");
                }
                if (c == BACKSPACE || c == DELETE) {
                    if (length > 0) {
                        buffer[--length] = '\0';
                        out.print("\b \b");
                        out.flush();
                    }
                    continue;
                }
                if (c >= 32) {
                    if (length == buffer.length) {
                        buffer = Arrays.copyOf(buffer, buffer.length * 2);
                    }
                    buffer[length++] = (char) c;
                    out.print('*');
                    out.flush();
                }
            }
        } finally {
            terminal.setAttributes(originalAttributes);
        }
        out.println();
        out.flush();
        var secret = Arrays.copyOf(buffer, length);
        Arrays.fill(buffer, '\0');
        return secret;
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
