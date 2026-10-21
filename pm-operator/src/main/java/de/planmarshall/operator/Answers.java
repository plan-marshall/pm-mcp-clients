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

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The answers a dialogue accepts for the yes/no questions of the catalogue.
 *
 * <p>The catalogue's questions print the mnemonic of the interface language ({@code y) yes} in
 * English, {@code j) ja} in German). The dialogues accept both, so an operator who switches
 * languages, or types the other language's letter out of habit, is not silently declined.
 */
final class Answers {

    private Answers() {
    }

    /**
     * @param answer the operator's answer
     * @return {@code true} for yes in either language
     */
    static boolean isYes(String answer) {
        return is(answer, "y", "j", "yes", "ja");
    }

    /**
     * @param answer the operator's answer
     * @return {@code true} for no in either language
     */
    static boolean isNo(String answer) {
        return is(answer, "n", "no", "nein");
    }

    private static boolean is(@Nullable String answer, String... accepted) {
        if (answer == null) {
            return false;
        }
        var clean = answer.trim().toLowerCase(Locale.ROOT);
        for (var candidate : accepted) {
            if (candidate.equals(clean)) {
                return true;
            }
        }
        return false;
    }
}
