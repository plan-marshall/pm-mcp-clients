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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The terminal output safety boundary, fed control sequences of every kind
 * (04-console.adoc#output-safety). Each case is a crafted text a model, a repository, or a remote
 * device could carry; none of them may survive into what the operator sees.
 */
@DisplayName("TerminalSanitizer")
class TerminalSanitizerTest {

    @Test
    @DisplayName("leaves clean text and the notice layout untouched")
    void keepsCleanText() {
        assertEquals("Plain text with\nnewlines and\ttabs.", TerminalSanitizer.sanitize("Plain text with\nnewlines and\ttabs."));
        assertEquals("", TerminalSanitizer.sanitize(null));
        assertEquals("", TerminalSanitizer.sanitize(""));
    }

    @Test
    @DisplayName("removes SGR colour, cursor movement, and erase sequences")
    void removesCsi() {
        var raw = "\u001B[31;1mRed\u001B[0m\u001B[2J\u001B[1;1HAlert";
        assertEquals("RedAlert", TerminalSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("removes the alternate screen switch and the saved cursor")
    void removesAlternateScreen() {
        var raw = "\u001B[?1049h\u001B7scrollback\u001B[?1049l";
        assertEquals("scrollback", TerminalSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("removes the OSC 52 clipboard write, however it is terminated")
    void removesClipboardOsc() {
        assertAllEmpty(
                            TerminalSanitizer.sanitize("x\u001B]52;c;c2VjcmV0\u0007y"),
                            TerminalSanitizer.sanitize("x\u001B]52;c;c2VjcmV0\u001B\\y"),
                            TerminalSanitizer.sanitize("x\u009D52;c;c2VjcmV0\u0007y"));
    }

    @Test
    @DisplayName("removes the OSC 0 and OSC 2 window-title attacks")
    void removesWindowTitleOsc() {
        assertAllEmpty(
                            TerminalSanitizer.sanitize("\u001B]0;faked prompt\u0007"),
                            TerminalSanitizer.sanitize("\u001B]2;faked\u001B\\"),
                            TerminalSanitizer.sanitize("\u009D2;faked\u009C"));
    }

    @Test
    @DisplayName("removes DCS, SOS, PM, and APC strings")
    void removesStringIntroducers() {
        var raw = "\u001BPpayload\u001B\\\u001BXpayload\u001B\\\u001B^payload\u001B\\\u001B_payload\u001B\\ok";
        assertEquals("ok", TerminalSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("removes C0 controls, DEL, and the C1 controls, but keeps LF and TAB")
    void removesControls() {
        var raw = "a\u0000b\u0007c\u0008d\u000Ce\u000Bf\u007Fg\u0085h\nI\tJ";
        assertEquals("abcdefgh\nI\tJ", TerminalSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("removes a carriage return that would overwrite the line")
    void removesCarriageReturn() {
        // The carriage return is removed, never emulated: what followed it is not lost.
        assertEquals("pm › fake", TerminalSanitizer.sanitize("pm › \rfake"));
    }

    @Test
    @DisplayName("drops a lone escape and an unterminated sequence")
    void dropsMalformedSequences() {
        assertEquals("tail", TerminalSanitizer.sanitize("\u001B\u0007tail"));
        assertEquals("", TerminalSanitizer.sanitize("\u001B[31"));
        assertEquals("x", TerminalSanitizer.sanitize("x\u001B]0;never terminated"));
    }

    @Test
    @DisplayName("the line variant turns a line break and a tab into a space")
    void lineVariant() {
        assertEquals("a prompt b", TerminalSanitizer.sanitizeLine("a\nprompt\r\tb"));
    }

    @Test
    @DisplayName("the title variant caps the length and neutralizes the sequence")
    void titleVariant() {
        var title = TerminalSanitizer.sanitizeTitle("\u001B]0;x\u0007" + "a".repeat(TerminalSanitizer.TITLE_MAX + 50));
        assertFalse(title.contains("\u001B"));
        assertFalse(title.contains("\u0007"));
        assertEquals(TerminalSanitizer.TITLE_MAX, title.length());
    }

    @Test
    @DisplayName("a sequence cannot smuggle a second one through its payload")
    void noInjectionThroughPayload() {
        var raw = "\u001B]0;\u001B]52;c;c2VjcmV0\u0007\u0007after";
        var safe = TerminalSanitizer.sanitize(raw);
        assertFalse(safe.contains("\u001B"));
        assertTrue(safe.endsWith("after"));
    }

    private static void assertAllEmpty(String... values) {
        for (var value : values) {
            assertFalse(value.contains("\u001B"), "ESC survived: " + value);
            assertFalse(value.contains("\u0007"), "BEL survived: " + value);
            assertFalse(value.contains("c2VjcmV0"), "payload survived: " + value);
        }
    }
}
