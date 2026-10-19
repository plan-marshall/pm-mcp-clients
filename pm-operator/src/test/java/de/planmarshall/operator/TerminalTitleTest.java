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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.jline.terminal.impl.DumbTerminal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TerminalTitle")
class TerminalTitleTest {

    @Test
    @DisplayName("writes OSC 0 with the title between the introducer and the bell")
    void writesOsc() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            TerminalTitle.set(terminal, "pm-operator");
        }
        assertTrue(rendered.toString(StandardCharsets.UTF_8).contains("\u001B]0;pm-operator\u0007"));
    }

    @Test
    @DisplayName("neutralizes a title that carries a sequence of its own")
    void neutralizes() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            TerminalTitle.set(terminal, "x\u001B]52;c;c2VjcmV0\u0007y\nz");
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        assertFalse(text.contains("c2VjcmV0"), text);
        assertFalse(text.contains("\u0007y"), text);
        assertTrue(text.endsWith("y z\u0007"), text);
    }
}
