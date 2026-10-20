/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.operator.OperatorContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("TuiScreenDriver")
class TuiScreenDriverTest {

    @TempDir
    Path tempDir;

    private static TuiViewStack stack(int rows) {
        var stack = new TuiViewStack();
        stack.replaceRoot(TuiLevel.root(TuiLevel.Kind.OVERVIEW, "Running work"), viewRows(rows), null);
        return stack;
    }

    private static List<TuiRow> viewRows(int count) {
        var rows = new ArrayList<TuiRow>();
        for (int index = 0; index < count; index++) {
            rows.add(new TuiRow("row-" + index, "row number " + index,
                                "details of row " + index, index == 0, true));
        }
        return rows;
    }

    private static TuiScreen screen(TuiViewStack stack, boolean connected) {
        return new TuiScreen("Running work \u00b7 projects", stack.level().kind(), stack.visibleRows(),
                            stack.selectedIndex(), stack.filter(), stack.helpVisible(), connected, 4);
    }

    private static TuiScreenDriver.KeyHandler handler(TuiViewStack stack) {
        return action -> {
            switch (action) {
                case MOVE_UP -> stack.move(-1);
                case MOVE_DOWN -> stack.move(1);
                case PAGE_UP -> stack.page(-1);
                case PAGE_DOWN -> stack.page(1);
                case FIRST -> stack.first();
                case LAST -> stack.last();
                case HELP -> stack.toggleHelp();
                case BACK -> stack.back();
                case LEAVE -> {
                    return false;
                }
                default -> {
                    // Open, actions, reports, and the other levels belong to later plans.
                }
            }
            return true;
        };
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("the view did not reach the expected state in time");
            }
            Thread.sleep(20L);
        }
    }

    private static void join(Thread thread) throws InterruptedException {
        thread.join(5_000L);
        assertFalse(thread.isAlive(), "the driver did not leave the view");
    }

    @Test
    @DisplayName("draws the view on the alternate screen and restores the terminal on q")
    void drawsAndRestores() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var stack = stack(5);
            virtual.type("j");
            var result = new AtomicReference<TuiScreenDriver.Result>();
            var thread = new Thread(() -> {
                try {
                    result.set(new TuiScreenDriver(virtual.terminal(), "en")
                                        .run(() -> screen(stack, true), handler(stack)));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(line -> line.contains("▶ row number 1")));
            var active = virtual.screen();
            var text = String.join("\n", active.lines());

            assertAll(
                                () -> assertTrue(active.alternateScreenActive()),
                                () -> assertTrue(active.lines().getFirst().contains("\u25cf connected"),
                                                    active.lines().getFirst()),
                                () -> assertTrue(text.contains("row number 1"), text),
                                () -> assertTrue(text.contains("details of row 1"), text),
                                () -> assertTrue(active.lines().getLast().contains("move"),
                                                    active.lines().getLast())
            );
            var selectedLine = indexOf(active.lines(), "row number 1");
            assertAll(
                                () -> assertTrue(active.lines().get(selectedLine).contains("\u25b6"),
                                                    active.lines().get(selectedLine)),
                                () -> assertTrue(active.invertedRows().contains(selectedLine))
            );

            virtual.type("q");
            join(thread);
            var after = virtual.screen();
            assertAll(
                                () -> assertEquals(TuiScreenDriver.Result.LEFT, result.get()),
                                () -> assertFalse(after.alternateScreenActive()),
                                () -> assertEquals(after.alternation().entries(), after.alternation().exits()),
                                () -> assertTrue(after.cursorVisible())
            );
        }
    }

    @Test
    @DisplayName("leaves the details line out at 80 columns")
    void compactAt80Columns() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(80, 24)) {
            var stack = stack(3);
            var thread = new Thread(() -> {
                try {
                    new TuiScreenDriver(virtual.terminal(), "en")
                                        .run(() -> screen(stack, true), handler(stack));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(line -> line.contains("row number 0")));
            var text = String.join("\n", virtual.screen().lines());
            assertAll(
                                () -> assertTrue(text.contains("row number 0"), text),
                                () -> assertFalse(text.contains("details of row 0"), text)
            );
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("a key the level does not bind never reaches the handler")
    void unboundKeyIsDropped() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var stack = stack(3);
            var seen = new CopyOnWriteArrayList<TuiKeyAction>();
            virtual.type("eq");
            new TuiScreenDriver(virtual.terminal(), "en").run(() -> screen(stack, true), action -> {
                seen.add(action);
                return action != TuiKeyAction.LEAVE;
            });
            assertAll(
                                () -> assertFalse(seen.contains(TuiKeyAction.EPIC), seen.toString()),
                                () -> assertTrue(seen.contains(TuiKeyAction.LEAVE), seen.toString())
            );
        }
    }

    @Test
    @DisplayName("a lone Esc is resolved after a short ambiguity wait")
    void escapeHasShortAmbiguityTimeout() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var driver = new TuiScreenDriver(virtual.terminal(), "en");
            assertEquals(TuiScreenDriver.AMBIGUOUS_TIMEOUT_MILLIS, driver.keyMap().getAmbiguousTimeout());
        }
    }

    @Test
    @DisplayName("a terminal below 60 columns does not open and prints the too-narrow text")
    void tooNarrowFallsBack() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(50, 20)) {
            var stack = stack(3);
            var result = new TuiScreenDriver(virtual.terminal(), "en")
                                .run(() -> screen(stack, true), handler(stack));
            assertAll(
                                () -> assertEquals(TuiScreenDriver.Result.TOO_NARROW, result),
                                () -> assertTrue(virtual.rawOutput().contains("too narrow"), virtual.rawOutput()),
                                () -> assertFalse(virtual.alternateScreenActive()),
                                () -> assertEquals(0, virtual.alternation().entries())
            );
        }
    }

    @Test
    @DisplayName("restores the terminal when the renderer throws")
    void restoresOnException() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var driver = new TuiScreenDriver(virtual.terminal(), "en");
            assertThrows(IllegalStateException.class,
                                () -> driver.run(() -> {
                                    throw new IllegalStateException("rendering failed");
                                }, handler(stack(2))));
            var after = virtual.screen();
            assertAll(
                                () -> assertFalse(after.alternateScreenActive()),
                                () -> assertEquals(1, after.alternation().entries()),
                                () -> assertEquals(1, after.alternation().exits()),
                                () -> assertTrue(after.cursorVisible())
            );
        }
    }

    @Test
    @DisplayName("a resize redraws at the new width")
    void resizeRedraws() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var stack = stack(4);
            var thread = new Thread(() -> {
                try {
                    new TuiScreenDriver(virtual.terminal(), "en")
                                        .run(() -> screen(stack, false), handler(stack));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(line -> line.contains("details of row 0")));

            int before = virtual.rawOutput().length();
            virtual.resize(80, 24);
            await(() -> virtual.rawOutput().length() > before);
            await(() -> {
                var current = virtual.screen();
                return current.lines().stream().anyMatch(line -> line.contains("row number 0"))
                                    && current.lines().stream().noneMatch(line -> line.contains("details of row 0"));
            });
            var resized = virtual.screen();
            assertAll(
                                () -> assertTrue(resized.lines().stream().noneMatch(line -> line.contains("details"))),
                                () -> assertTrue(resized.lines().stream()
                                                    .allMatch(line -> TuiLayout.displayWidth(line) <= 80)),
                                () -> assertTrue(resized.lines().getFirst().contains("reconnecting"),
                                                    resized.lines().getFirst())
            );
            virtual.type("q");
            join(thread);
        }
    }

    @Nested
    @DisplayName("canOpenFullScreen")
    class Gate {

        private OperatorContext context(boolean interactive, Map<String, String> extra) throws IOException {
            var environment = new HashMap<String, String>();
            environment.put("PM_MCP_BASE", tempDir.resolve("machine").toString());
            environment.putAll(extra);
            return new OperatorContext(environment, tempDir, interactive);
        }

        private void writeMachineConfig(String json) throws IOException {
            var base = tempDir.resolve("machine");
            Files.createDirectories(base);
            Files.writeString(base.resolve("machine-config.json"), json, StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("opens on an interactive xterm that is wide enough")
        void opensOnWideInteractiveTerminal() throws Exception {
            try (var virtual = VirtualScreenTerminal.create(120, 30)) {
                assertTrue(TuiScreenDriver.canOpenFullScreen(context(true, Map.of()), virtual.terminal(),
                                    TuiLayout.MIN_COLUMNS));
            }
        }

        @Test
        @DisplayName("does not open below the minimum width")
        void refusesNarrowTerminal() throws Exception {
            try (var virtual = VirtualScreenTerminal.create(50, 20)) {
                assertFalse(TuiScreenDriver.canOpenFullScreen(context(true, Map.of()), virtual.terminal(),
                                    TuiLayout.MIN_COLUMNS));
            }
        }

        @Test
        @DisplayName("does not open on a non-interactive process")
        void refusesNonInteractive() throws Exception {
            try (var virtual = VirtualScreenTerminal.create(120, 30)) {
                assertFalse(TuiScreenDriver.canOpenFullScreen(context(false, Map.of()), virtual.terminal(),
                                    TuiLayout.MIN_COLUMNS));
            }
        }

        @Test
        @DisplayName("does not open on a dumb terminal")
        void refusesDumbTerminal() throws Exception {
            try (var dumb = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())) {
                assertFalse(TuiScreenDriver.canOpenFullScreen(context(true, Map.of()), dumb,
                                    TuiLayout.MIN_COLUMNS));
            }
        }

        @Test
        @DisplayName("honours console.full_screen false and the default true")
        void honoursMachineConfiguration() throws Exception {
            try (var virtual = VirtualScreenTerminal.create(120, 30)) {
                var context = context(true, Map.of());
                assertTrue(TuiScreenDriver.canOpenFullScreen(context, virtual.terminal(), TuiLayout.MIN_COLUMNS));
                writeMachineConfig("{\"format_version\":1,\"console\":{\"full_screen\":false}}");
                assertFalse(TuiScreenDriver.canOpenFullScreen(context, virtual.terminal(), TuiLayout.MIN_COLUMNS));
                writeMachineConfig("{\"format_version\":1,\"console\":{\"full_screen\":true}}");
                assertTrue(TuiScreenDriver.canOpenFullScreen(context, virtual.terminal(), TuiLayout.MIN_COLUMNS));
                writeMachineConfig("{not json");
                assertTrue(TuiScreenDriver.canOpenFullScreen(context, virtual.terminal(), TuiLayout.MIN_COLUMNS));
                writeMachineConfig("{\"console\":\"not an object\"}");
                assertTrue(TuiScreenDriver.canOpenFullScreen(context, virtual.terminal(), TuiLayout.MIN_COLUMNS));
            }
        }

        @Test
        @DisplayName("NO_COLOR does not keep the view closed")
        void noColorStillOpens() throws Exception {
            try (var virtual = VirtualScreenTerminal.create(120, 30)) {
                assertTrue(TuiScreenDriver.canOpenFullScreen(context(true, Map.of("NO_COLOR", "1")),
                                    virtual.terminal(), TuiLayout.MIN_COLUMNS));
            }
        }
    }

    private static int indexOf(List<String> lines, String needle) {
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).contains(needle)) {
                return index;
            }
        }
        fail("no line contains " + needle);
        return -1;
    }
}
