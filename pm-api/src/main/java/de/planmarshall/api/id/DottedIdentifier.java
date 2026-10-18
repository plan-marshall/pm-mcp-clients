/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.id;

import java.util.function.IntPredicate;

/**
 * The dotted identifier grammars of the local API (Identifier Registry): the event name, the state
 * id, and the message key. Each is a dot-separated sequence of segments with a per-segment alphabet.
 * <p>
 * The grammars are matched segment by segment in a single linear scan. A regular expression with a
 * quantified inner class inside a quantified group (for example {@code ^[a-z][a-z_]*(\.[a-z][a-z_]*)*$})
 * can backtrack super-linearly on a long malformed input; the scan here cannot, and rejects a leading,
 * trailing, or doubled dot, an empty segment, and a segment outside its length or alphabet.
 */
public final class DottedIdentifier {

    private static final int EVENT_NAME_MAX_LENGTH = 64;
    private static final int STATE_ID_MAX_LENGTH = 64;
    private static final int STATE_ID_MAX_SEGMENT_LENGTH = 32;
    private static final int MESSAGE_KEY_MAX_LENGTH = 128;

    private DottedIdentifier() {
        // Utility class
    }

    /**
     * @param value a candidate event name
     * @return {@code true} if the value satisfies the Event Name grammar
     *         ({@code ^[a-z][a-z_]*(\.[a-z][a-z_]*)*$}, at most 64 characters)
     */
    public static boolean eventName(String value) {
        return dotted(value, EVENT_NAME_MAX_LENGTH, 0, true, 1, DottedIdentifier::isLowerOrUnderscore);
    }

    /**
     * @param value a candidate state id
     * @return {@code true} if the value satisfies the State Id grammar
     *         ({@code ^[a-z][a-z0-9_-]{0,31}(\.[a-z][a-z0-9_-]{0,31})*$}, at most 64 characters)
     */
    public static boolean stateId(String value) {
        return dotted(value, STATE_ID_MAX_LENGTH, STATE_ID_MAX_SEGMENT_LENGTH, true, 1,
                DottedIdentifier::isStateIdChar);
    }

    /**
     * @param value a candidate message key
     * @return {@code true} if the value satisfies the Message Key grammar
     *         ({@code ^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$}, at most 128 characters)
     */
    public static boolean messageKey(String value) {
        return dotted(value, MESSAGE_KEY_MAX_LENGTH, 0, false, 2, DottedIdentifier::isKeyChar);
    }

    private static boolean dotted(String value, int maxLength, int maxSegmentLength,
                                  boolean letterAtEverySegmentStart, int minSegments, IntPredicate restAllowed) {
        if (value == null || value.isEmpty() || value.length() > maxLength) {
            return false;
        }
        var segments = 0;
        var start = 0;
        for (var i = 0; i <= value.length(); i++) {
            if (i != value.length() && value.charAt(i) != '.') {
                continue;
            }
            var length = i - start;
            if (length == 0 || maxSegmentLength > 0 && length > maxSegmentLength) {
                return false;
            }
            var first = value.charAt(start);
            if (segments == 0 || letterAtEverySegmentStart) {
                if (!isLowerLetter(first)) {
                    return false;
                }
            } else if (!restAllowed.test(first)) {
                return false;
            }
            for (var j = start + 1; j < i; j++) {
                if (!restAllowed.test(value.charAt(j))) {
                    return false;
                }
            }
            segments++;
            start = i + 1;
        }
        return segments >= minSegments;
    }

    private static boolean isLowerLetter(int c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isLowerOrUnderscore(int c) {
        return isLowerLetter(c) || c == '_';
    }

    private static boolean isStateIdChar(int c) {
        return isLowerLetter(c) || c >= '0' && c <= '9' || c == '_' || c == '-';
    }

    private static boolean isKeyChar(int c) {
        return isLowerLetter(c) || c >= '0' && c <= '9' || c == '_';
    }
}
