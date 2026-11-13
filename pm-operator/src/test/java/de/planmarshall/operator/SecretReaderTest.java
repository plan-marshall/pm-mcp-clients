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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.impl.DumbTerminal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecretReader")
class SecretReaderTest {

    private final ByteArrayOutputStream rendered = new ByteArrayOutputStream();

    private DumbTerminal terminal(String input) throws Exception {
        return new DumbTerminal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), rendered);
    }

    @Test
    @DisplayName("reads the secret without echoing it and masks every character")
    void readsAndMasks() throws Exception {
        try (var terminal = terminal("s3cret\n")) {
            var secret = SecretReader.read(terminal, new PrintWriter(rendered, true), "en");
            try {
                assertArrayEquals("s3cret".toCharArray(), secret);
                var text = rendered.toString(StandardCharsets.UTF_8);
                assertTrue(text.contains("******"), text);
                assertFalse(text.contains("s3cret"), text);
            } finally {
                SecretReader.dispose(secret);
            }
        }
    }

    @Test
    @DisplayName("a backspace removes the last character of the buffer and of the mask")
    void backspace() throws Exception {
        try (var terminal = terminal("ab\u007Fc\n")) {
            var secret = SecretReader.read(terminal, new PrintWriter(rendered, true), "en");
            assertArrayEquals("ac".toCharArray(), secret);
            SecretReader.dispose(secret);
        }
    }

    @Test
    @DisplayName("ends at the end of the input")
    void endOfInput() throws Exception {
        try (var terminal = terminal("only")) {
            var secret = SecretReader.read(terminal, new PrintWriter(rendered, true), "en");
            assertArrayEquals("only".toCharArray(), secret);
            SecretReader.dispose(secret);
        }
    }

    @Test
    @DisplayName("Ctrl+C cancels the entry and clears the buffer")
    void ctrlC() throws Exception {
        try (var terminal = terminal("abc\u0003")) {
            var writer = new PrintWriter(rendered, true);
            assertThrows(UserInterruptException.class, () -> SecretReader.read(terminal, writer, "en"));
        }
    }

    @Test
    @DisplayName("Ctrl+C on a terminal that turns it into a signal is read as a key and the echo comes back")
    void ctrlCWithSignalsRestoresTheAttributes() throws Exception {
        // Every set of attributes the reader gives the terminal, in order.
        var applied = new ArrayList<Attributes>();
        var input = new ByteArrayInputStream("abc\u0003".getBytes(StandardCharsets.UTF_8));
        try (var terminal = new DumbTerminal(input, rendered) {
                 @Override
                 public void setAttributes(Attributes attributes) {
                     applied.add(new Attributes(attributes));
                     super.setAttributes(attributes);
                 }
             }) {
            // A real tty: the echo is on and Ctrl+C is the interrupt character that ISIG turns
            // into a signal, which would end the JVM before the attributes are restored.
            var tty = terminal.getAttributes();
            tty.setLocalFlag(Attributes.LocalFlag.ISIG, true);
            tty.setLocalFlag(Attributes.LocalFlag.ECHO, true);
            tty.setLocalFlag(Attributes.LocalFlag.ICANON, true);
            tty.setControlChar(Attributes.ControlChar.VINTR, 3);
            terminal.setAttributes(tty);
            var before = new Attributes(terminal.getAttributes());
            applied.clear();
            var writer = new PrintWriter(rendered, true);

            assertThrows(UserInterruptException.class, () -> SecretReader.read(terminal, writer, "en"));

            var after = terminal.getAttributes();
            assertAll(
                                () -> assertTrue(applied.stream().anyMatch(set -> !set.getLocalFlag(Attributes.LocalFlag.ECHO)
                                                                        && !set.getLocalFlag(Attributes.LocalFlag.ISIG)),
                                                    "the secret is read with the echo and the signals off, so Ctrl+C is a key"),
                                () -> assertEquals(before.getLocalFlags(), after.getLocalFlags(),
                                                    "the echo and the signals are restored"),
                                () -> assertEquals(before.getInputFlags(), after.getInputFlags()),
                                () -> assertTrue(after.getLocalFlag(Attributes.LocalFlag.ECHO), "the echo is back")
            );
        }
    }

    @Test
    @DisplayName("dispose clears the buffer and tolerates null")
    void dispose() {
        var secret = "secret".toCharArray();
        SecretReader.dispose(secret);
        assertEquals(6, secret.length);
        for (var c : secret) {
            assertEquals('\0', c);
        }
        SecretReader.dispose(null);
    }
}
