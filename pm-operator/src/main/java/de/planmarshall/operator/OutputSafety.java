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

import org.jspecify.annotations.Nullable;

/**
 * Output safety utility stripping ANSI control sequences and non-printable characters
 * to prevent terminal injection.
 */
public final class OutputSafety {

    private OutputSafety() {
        // Utility class
    }

    /**
     * Sanitizes untrusted strings before printing to the terminal.
     *
     * @param text the raw string
     * @return sanitized string safe for terminal rendering
     */
    public static String stripControlSequences(@Nullable String text) {
        return ConsoleNotice.stripControlSequences(text);
    }
}
