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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("dialogue answers")
class AnswersTest {

    @Test
    @DisplayName("yes is accepted in English and German, and nothing else")
    void yes() {
        assertTrue(Answers.isYes("y"));
        assertTrue(Answers.isYes("Y"));
        assertTrue(Answers.isYes("j"));
        assertTrue(Answers.isYes("yes"));
        assertTrue(Answers.isYes("ja"));
        assertFalse(Answers.isYes("n"));
        assertFalse(Answers.isYes(""));
    }

    @Test
    @DisplayName("no is accepted in English and German, and nothing else")
    void no() {
        assertTrue(Answers.isNo("n"));
        assertTrue(Answers.isNo("no"));
        assertTrue(Answers.isNo("nein"));
        assertFalse(Answers.isNo("y"));
        assertFalse(Answers.isNo(""));
    }
}
