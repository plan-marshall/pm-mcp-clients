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

import java.io.PrintWriter;

/**
 * The one sink of the answer dialogue: every text written through it has its control sequences
 * removed first ({@link TerminalSanitizer}). Wrapping the command's output once means a new print
 * site cannot forget the rule, and a text from a model or a repository never reaches the terminal
 * raw ({@code doc/design/shared/decisions.adoc#safety}).
 */
final class SanitizingPrintWriter extends PrintWriter {

    /**
     * @param delegate the writer the neutralized text is passed on to
     */
    SanitizingPrintWriter(PrintWriter delegate) {
        super(delegate);
    }

    @Override
    public void write(int c) {
        var clean = TerminalSanitizer.sanitize(String.valueOf((char) c));
        super.write(clean, 0, clean.length());
    }

    @Override
    public void write(char[] buffer, int offset, int length) {
        var clean = TerminalSanitizer.sanitize(new String(buffer, offset, length));
        super.write(clean, 0, clean.length());
    }

    @Override
    public void write(String text, int offset, int length) {
        var clean = TerminalSanitizer.sanitize(text.substring(offset, offset + length));
        super.write(clean, 0, clean.length());
    }
}
