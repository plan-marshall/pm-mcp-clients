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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The ages of {@code projects} and {@code status} in a German session, rendered through the one
 * display rule instead of the English-only inline formatters the client used to carry.
 */
@DisplayName("German ages of projects and status")
class GermanAgeDisplayTest {

    @Test
    @DisplayName("projects and status print a German session's ages from the display rule")
    void germanAges() {
        var now = Instant.now();

        assertEquals("gerade eben", ProjectsCommand.formatRelativeTime("de", now.minusSeconds(30)));
        assertEquals("vor 5 min", ProjectsCommand.formatRelativeTime("de", now.minusSeconds(300)));
        assertEquals("vor 3 h", ProjectsCommand.formatRelativeTime("de", now.minusSeconds(3 * 3600)));
        assertEquals("vor 9 T", ProjectsCommand.formatRelativeTime("de", now.minusSeconds(9 * 86400)));
        assertEquals("vor 2 Monaten", ProjectsCommand.formatRelativeTime("de", now.minusSeconds(60 * 86400)));

        assertEquals("gerade eben", StatusCommand.formatRelativeTime("de", now.minusSeconds(30)));
        assertEquals("vor 5 min", StatusCommand.formatRelativeTime("de", now.minusSeconds(300)));
        assertEquals("vor 9 T", StatusCommand.formatRelativeTime("de", now.minusSeconds(9 * 86400)));
    }
}
