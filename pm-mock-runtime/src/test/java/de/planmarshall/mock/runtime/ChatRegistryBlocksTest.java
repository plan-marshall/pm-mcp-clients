/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The word a decision's chat line is selected by: the payload's own {@code blocks} value, and
 * {@code nothing} where the payload names none (`control.chat.line.decision_opened`).
 */
@DisplayName("ChatRegistryBlocksTest")
class ChatRegistryBlocksTest {

    @Test
    @DisplayName("falls back to nothing where the payload names no blocker")
    void fallsBackToNothing() {
        assertEquals("nothing", ChatRegistry.blocksOf(Map.of()));
        assertEquals("nothing", ChatRegistry.blocksOf(Map.of("blocks", "")));
    }

    @Test
    @DisplayName("passes the payload's own value through")
    void passesTheValueThrough() {
        assertEquals("plan", ChatRegistry.blocksOf(Map.of("blocks", "plan")));
        assertEquals("setup", ChatRegistry.blocksOf(Map.of("blocks", "setup")));
    }

    @Test
    @DisplayName("reads the decision the payload nests when the payload names none itself")
    void readsTheNestedDecision() {
        assertEquals("epic", ChatRegistry.blocksOf(Map.of("decision", Map.of("blocks", "epic"))));
    }
}
