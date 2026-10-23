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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The terminal ownership seam: a scoped terminal is returned while it is set, the provider is not
 * asked, and only a terminal the context created counts as owned.
 */
@DisplayName("OperatorContext terminal ownership")
class OperatorContextTerminalTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("a scoped terminal wins over the provider until it is cleared")
    void scopedTerminalWinsUntilCleared() throws Exception {
        try (var borrowed = VirtualScreenTerminal.create(80, 24);
             var provided = VirtualScreenTerminal.create(80, 24)) {
            var creations = new AtomicInteger();
            var context = new OperatorContext(Map.of(), tempDir, true,
                                ClientEnvironment.capture(Map.of()), () -> {
                                    creations.incrementAndGet();
                                    return provided.terminal();
                                }, null);

            assertSame(provided.terminal(), context.createTerminal());
            assertEquals(1, creations.get());

            context.useTerminal(borrowed.terminal());
            assertSame(borrowed.terminal(), context.createTerminal());
            assertEquals(1, creations.get(), "the provider is not asked while a scope is set");

            context.clearTerminal();
            assertSame(provided.terminal(), context.createTerminal());
            assertEquals(2, creations.get());
        }
    }

    @Test
    @DisplayName("a scoped terminal is borrowed, a provider terminal is owned")
    void borrowedAndOwned() throws Exception {
        try (var borrowed = VirtualScreenTerminal.create(80, 24);
             var provided = VirtualScreenTerminal.create(80, 24)) {
            var context = new OperatorContext(Map.of(), tempDir, true,
                                ClientEnvironment.capture(Map.of()), provided::terminal, null);

            context.useTerminal(borrowed.terminal());
            assertFalse(context.ownsTerminal(borrowed.terminal()));
            assertTrue(context.ownsTerminal(provided.terminal()));
            assertFalse(context.ownsTerminal(null));

            context.clearTerminal();
            assertTrue(context.ownsTerminal(provided.terminal()));
        }
    }
}
