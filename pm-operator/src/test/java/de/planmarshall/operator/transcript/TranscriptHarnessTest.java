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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The pieces of the transcript harness on their own: the normalizer that keeps the diff readable,
 * the unified diff that shows a drift, and the scripted terminal that JLine reads from.
 */
@DisplayName("transcript harness")
class TranscriptHarnessTest {

    @Nested
    @DisplayName("normalizer")
    class Normalizer {

        private final TranscriptNormalizer normalizer = new TranscriptNormalizer()
                            .replacing("/tmp/pm-1234/b", "<machine-root>");

        @Test
        @DisplayName("strips ANSI and OSC escapes")
        void stripsEscapes() {
            var buffer = "\u001B[1mbold\u001B[0m \u001B]0;title\u0007plain";
            assertEquals("bold plain", normalizer.normalize(buffer));
        }

        @Test
        @DisplayName("replaces instants, clock times, durations, process ids, and the machine root")
        void replacesTransientParts() {
            var buffer = "started 2026-10-05T10:00:00Z at 14:32 pid 42 from /tmp/pm-1234/b"
                                + " after 53 h 49 min, 12 min, and 3 d ago";
            assertEquals("started <timestamp> at <time> pid <pid> from <machine-root>"
                                + " after <duration>, <duration>, and <ago>",
                                normalizer.normalize(buffer));
        }

        @Test
        @DisplayName("normalises line endings and trailing whitespace but keeps the layout")
        void keepsLayout() {
            var buffer = "first  \r\n  second\t\r\n";
            assertEquals("first\n  second\n", normalizer.normalize(buffer));
        }
    }

    @Nested
    @DisplayName("unified diff")
    class Diff {

        @Test
        @DisplayName("is empty for equal lines")
        void equal() {
            assertEquals("", UnifiedDiff.between("expected", List.of("a", "b"), "rendered", List.of("a", "b")));
        }

        @Test
        @DisplayName("shows the removed and the added line with its place")
        void changed() {
            var diff = UnifiedDiff.between("expected", List.of("one", "two", "three"),
                                "rendered", List.of("one", "changed", "three"));
            assertAll(
                                () -> assertTrue(diff.startsWith("--- expected\n+++ rendered\n@@ -1,3 +1,3 @@\n"), diff),
                                () -> assertFalse(diff.contains("@@ @@"), diff),
                                () -> assertTrue(diff.contains("-two\n"), diff),
                                () -> assertTrue(diff.contains("+changed\n"), diff),
                                () -> assertTrue(diff.contains(" one\n"), diff));
        }
    }

    @Nested
    @DisplayName("recorded transcript")
    class RecordedTranscript {

        @Test
        @DisplayName("fails with a readable diff when the rendered buffer drifts")
        void failsWithDiff() {
            Assumptions.assumeFalse(Boolean.getBoolean(TranscriptAssert.UPDATE_PROPERTY),
                                "recording mode is on");
            var error = assertThrows(AssertionError.class,
                                () -> TranscriptAssert.assertMatches("status", "runtime   not running\n"));
            assertAll(
                                () -> assertTrue(error.getMessage().contains("--- status.transcript"), error.getMessage()),
                                () -> assertTrue(error.getMessage().contains("-Runtime pm-mcpd"), error.getMessage()),
                                () -> assertTrue(error.getMessage().contains("+runtime   not running"), error.getMessage()));
        }
    }

    @Nested
    @DisplayName("scripted terminal")
    class ScriptedTerminal {

        @Test
        @DisplayName("keeps the pinned grid when a dialogue resizes the terminal")
        void keepsPinnedSize() throws Exception {
            try (var driver = TerminalTestDriver.create()) {
                driver.terminal().setSize(Size.of(120, 40));
                assertEquals(Size.of(80, 24), driver.terminal().getSize());
            }
        }

        @Test
        @DisplayName("feeds a typed line to a JLine line reader and echoes the prompt")
        void feedsLine() throws Exception {
            try (var driver = TerminalTestDriver.create()) {
                LineReader reader = LineReaderBuilder.builder().terminal(driver.terminal()).build();
                driver.typeLine("status");

                assertEquals("status", reader.readLine("pm › "));
                assertTrue(driver.render().contains("pm › status"), driver.render());
            }
        }

        @Test
        @DisplayName("ends the stream when the dialogue reads past the script")
        void endsAfterScript() throws Exception {
            try (var driver = TerminalTestDriver.create()) {
                LineReader reader = LineReaderBuilder.builder().terminal(driver.terminal()).build();
                driver.endOfInput();

                assertThrows(EndOfFileException.class, () -> reader.readLine("pm › "));
            }
        }
    }
}
