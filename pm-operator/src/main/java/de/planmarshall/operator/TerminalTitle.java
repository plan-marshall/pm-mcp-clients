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
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

/**
 * Sets the terminal's window title through the operating system command {@code OSC 0}
 * (04-console.adoc#output-safety). The title is never trusted raw: it is neutralized by
 * {@link TerminalSanitizer}, so a text that comes from the runtime — a plan name, a question's
 * summary — cannot terminate the sequence early and inject a command of its own.
 */
public final class TerminalTitle {

    private static final String OSC_SET_TITLE = "\u001B]0;";
    private static final String BEL = "\u0007";

    private TerminalTitle() {
        // Utility class
    }

    /**
     * Sets the terminal window title to a neutralized text.
     *
     * @param terminal the terminal whose window title is set
     * @param title    the raw title, may be {@code null}
     * @throws IOException if the title cannot be written
     */
    public static void set(Terminal terminal, @Nullable String title) throws IOException {
        terminal.writer().write(OSC_SET_TITLE + TerminalSanitizer.sanitizeTitle(title) + BEL);
        terminal.writer().flush();
    }
}
