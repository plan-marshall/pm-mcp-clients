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
 * The one sanitizer every text from outside this client passes through before it reaches the
 * terminal (04-console.adoc#output-safety). A notice, a dialogue state, a notification payload, a
 * device label, and the window title alike are neutralized here: no crafted text must move the
 * cursor, write the clipboard, fake a prompt, set the window title, or switch the alternate screen.
 *
 * <p>Neutralized are the C0 controls (except a line feed and a tab, which the notices use), the C1
 * controls, {@code DEL}, and every terminal sequence: {@code ESC} two-character sequences, the CSI
 * (control sequence introducer, {@code ESC [} and the 8-bit {@code 0x9B}), the OSC (operating
 * system command, {@code ESC ]} and the 8-bit {@code 0x9D}, terminated by {@code BEL} or
 * {@code ST} — this is where the clipboard OSC 52 and the window-title OSC 0/2 live), and the
 * string introducers DCS, SOS, PM, and APC ({@code ESC P}/{@code X}/{@code ^}/{@code _}).
 *
 * <p>Nothing here is a boundary by itself; it is the single place a boundary is applied, so that a
 * new rendering path cannot forget the rule.
 */
public final class TerminalSanitizer {

    private static final char ESC = '\u001B';
    private static final char BEL = '\u0007';
    private static final char CSI_8BIT = '\u009B';
    private static final char OSC_8BIT = '\u009D';
    private static final char ST_8BIT = '\u009C';
    private static final char LF = '\n';
    private static final char TAB = '\t';
    /** The longest window title kept; longer titles are cut, control sequences never survive. */
    static final int TITLE_MAX = 200;

    private TerminalSanitizer() {
        // Utility class
    }

    /**
     * Neutralizes a text for terminal rendering, keeping the line feed and the tab that the notice
     * blocks use for their layout and dropping every other control character and terminal sequence.
     *
     * @param text the raw text, may be {@code null}
     * @return the text without any control sequence or control character it could carry
     */
    public static String sanitize(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        var out = new StringBuilder(text.length());
        int i = 0;
        int length = text.length();
        while (i < length) {
            char c = text.charAt(i);
            if (c == ESC) {
                i = skipEscape(text, i);
            } else if (c == CSI_8BIT) {
                i = skipCsi(text, i + 1);
            } else if (c == OSC_8BIT) {
                i = skipOsc(text, i + 1);
            } else if (isStringIntroducer8Bit(c)) {
                i = skipString(text, i + 1);
            } else if (c == LF || c == TAB || !Character.isISOControl(c)) {
                out.append(c);
                i++;
            } else {
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Neutralizes a text for a single line, where a line feed or a tab could be used to fake a
     * prompt or overwrite a line: both become a space before the same neutralization.
     *
     * @param text the raw text, may be {@code null}
     * @return the neutralized single-line text
     */
    public static String sanitizeLine(@Nullable String text) {
        return sanitize(text).replace(LF, ' ').replace(TAB, ' ');
    }

    /**
     * Neutralizes a text for use as a terminal window title ({@code OSC 0}): every line break is a
     * space, every other control sequence is dropped, and the title is capped, so a crafted title
     * cannot terminate the sequence early and inject one of its own.
     *
     * @param title the raw title, may be {@code null}
     * @return a title that is safe to place between the {@code OSC 0;} introducer and the terminator
     */
    public static String sanitizeTitle(@Nullable String title) {
        var line = sanitizeLine(title);
        return line.length() <= TITLE_MAX ? line : line.substring(0, TITLE_MAX);
    }

    /**
     * Neutralizes a text used as a payload of a desktop notification ({@code OSC 9}/{@code 777}/99).
     *
     * @param text the raw payload, may be {@code null}
     * @return the neutralized payload
     */
    public static String sanitizeNotification(@Nullable String text) {
        return sanitizeTitle(text);
    }

    private static boolean isStringIntroducer8Bit(char c) {
        return c == '\u0090' || c == '\u0098' || c == '\u009E' || c == '\u009F';
    }

    private static int skipEscape(String text, int index) {
        int next = index + 1;
        if (next >= text.length()) {
            return next;
        }
        char c = text.charAt(next);
        return switch (c) {
            case '[' -> skipCsi(text, next + 1);
            case ']' -> skipOsc(text, next + 1);
            case 'P', 'X', '^', '_' -> skipString(text, next + 1);
            // A two-character escape (including a doubled ESC) is one sequence.
            default -> next + 1;
        };
    }

    private static int skipCsi(String text, int index) {
        int i = index;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c >= 0x40 && c <= 0x7E) {
                return i + 1;
            }
            if (c < 0x20 || c > 0x3F) {
                // Malformed sequence: stop at the offending character, not beyond it.
                return i;
            }
            i++;
        }
        return i;
    }

    private static int skipOsc(String text, int index) {
        int i = index;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == BEL || c == ST_8BIT) {
                return i + 1;
            }
            if (c == ESC) {
                return endOfString(text, i);
            }
            i++;
        }
        return i;
    }

    private static int skipString(String text, int index) {
        int i = index;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ST_8BIT) {
                return i + 1;
            }
            if (c == ESC) {
                return endOfString(text, i);
            }
            i++;
        }
        return i;
    }

    private static int endOfString(String text, int escapeIndex) {
        if (escapeIndex + 1 < text.length() && text.charAt(escapeIndex + 1) == '\\') {
            return escapeIndex + 2;
        }
        return escapeIndex + 1;
    }
}
