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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

            context.clearTerminal(borrowed.terminal());
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

            context.clearTerminal(borrowed.terminal());
            assertTrue(context.ownsTerminal(provided.terminal()));
        }
    }

    @Test
    @DisplayName("scopes nest, and clearing one returns the view's terminal to its outer scope")
    void scopesNest() throws Exception {
        try (var view = VirtualScreenTerminal.create(80, 24);
             var nested = VirtualScreenTerminal.create(80, 24);
             var provided = VirtualScreenTerminal.create(80, 24)) {
            var context = new OperatorContext(Map.of(), tempDir, true,
                                ClientEnvironment.capture(Map.of()), provided::terminal, null);

            context.useTerminal(view.terminal());
            context.useTerminal(nested.terminal());
            assertSame(nested.terminal(), context.createTerminal());
            assertFalse(context.ownsTerminal(view.terminal()),
                                "the outer scope's terminal is lent, not owned");

            context.clearTerminal(nested.terminal());
            assertSame(view.terminal(), context.createTerminal(),
                                "the view's scope is still set after the nested one is cleared");

            context.clearTerminal(view.terminal());
            assertSame(provided.terminal(), context.createTerminal(),
                                "the provider supplies the terminal again once every scope is cleared");
        }
    }

    @Test
    @DisplayName("clearing a terminal that is not the innermost scope is refused")
    void refusesToClearTheWrongScope() throws Exception {
        try (var view = VirtualScreenTerminal.create(80, 24);
             var nested = VirtualScreenTerminal.create(80, 24)) {
            var context = new OperatorContext(Map.of(), tempDir, true,
                                ClientEnvironment.capture(Map.of()), view::terminal, null);

            context.useTerminal(view.terminal());
            context.useTerminal(nested.terminal());
            var outer = view.terminal();

            assertThrows(IllegalStateException.class, () -> context.clearTerminal(outer),
                                "popping the outer scope would hand the nested one away");
            assertSame(nested.terminal(), context.createTerminal(), "the scopes are untouched");
        }
    }

    @Test
    @DisplayName("a lease closes the terminal only when the context owns it")
    void leaseClosesOnlyOwnedTerminals() throws Exception {
        try (var borrowed = VirtualScreenTerminal.create(80, 24);
             var provided = VirtualScreenTerminal.create(80, 24)) {
            var context = new OperatorContext(Map.of(), tempDir, true,
                                ClientEnvironment.capture(Map.of()), provided::terminal, null);

            context.useTerminal(borrowed.terminal());
            try (var lease = context.openTerminal()) {
                assertSame(borrowed.terminal(), lease.terminal());
            }
            assertFalse(borrowed.isClosed(), "a borrowed terminal is not the lease's to close");

            context.clearTerminal(borrowed.terminal());
            try (var lease = context.openTerminal()) {
                assertSame(provided.terminal(), lease.terminal());
            }
            assertTrue(provided.isClosed(), "a terminal the context created is closed with its lease");
        }
    }
}
