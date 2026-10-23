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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The console hand-off, in memory: a full-screen verb the console dispatches reuses the console's
 * terminal, never opens a second one, and never closes the borrowed terminal. A direct verb still
 * creates and closes its own terminal.
 */
@DisplayName("PmConsole full-screen hand-off")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PmConsoleHandoffTest {

    private static final long AWAIT_MILLIS = 15_000L;
    private static final String PROMPT = "pm \u203a";

    private MockRuntime runtime;

    @BeforeEach
    void startRuntime() throws IOException {
        runtime = MockRuntime.start(MockRuntimeConfig.builder()
                            .scenario(StandardScenarios.FULLY_ONBOARDED).build());
        recordLiveRuntime(runtime);
    }

    @AfterEach
    void stopRuntime() {
        if (runtime != null) {
            runtime.close();
        }
    }

    @Test
    @DisplayName("plans uses the console's terminal once, holds notices, and returns to the prompt")
    void consolePlansUsesTheConsoleTerminal() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var creations = new AtomicInteger();
            var context = context(virtual, creations);
            var console = new PmConsole(context);
            var exitCode = new AtomicInteger(-1);
            var thread = Thread.ofPlatform().start(() -> exitCode.set(runConsole(console)));

            await(() -> shows(virtual, PROMPT));
            virtual.type("plans\n");
            await(() -> virtual.alternateScreenActive() && shows(virtual, "Running work"));

            assertEquals(1, creations.get(), "the console's terminal is the only one created");
            assertFalse(context.ownsTerminal(virtual.terminal()),
                                "the console's terminal is borrowed while the view is open");
            assertTrue(console.noticeBuffer().isActive(), "the view holds the notices that arrive");

            console.noticeBuffer().hold(ConsoleNoticeBuffer.Category.REPORT);
            virtual.type("q");
            await(() -> !virtual.alternateScreenActive() && shows(virtual, PROMPT));

            assertFalse(console.noticeBuffer().isActive(), "the buffer closes when the view leaves");
            assertTrue(virtual.rawOutput().contains("While the overview was open: 1 new report."),
                                virtual.rawOutput());
            assertFalse(virtual.isClosed(), "the nested verb must not close the console's terminal");
            assertTrue(context.ownsTerminal(virtual.terminal()),
                                "the scope is cleared when the dispatch returns");

            virtual.type("exit\n");
            thread.join(AWAIT_MILLIS);
            assertEquals(0, exitCode.get());
        }
    }

    @Test
    @DisplayName("a direct plans run creates one terminal and closes it when the view leaves")
    void directPlansClosesItsOwnTerminal() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var creations = new AtomicInteger();
            var context = context(virtual, creations);
            var out = new StringWriter();
            var thread = Thread.ofPlatform().start(() -> {
                try {
                    PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()), "plans");
                } catch (RuntimeException _) {
                    // The assertion below reports the state of the view.
                }
            });

            await(() -> virtual.alternateScreenActive() && shows(virtual, "Running work"));
            assertEquals(1, creations.get());
            assertTrue(context.ownsTerminal(virtual.terminal()));
            assertFalse(virtual.isClosed());

            virtual.type("q");
            thread.join(AWAIT_MILLIS);
            assertTrue(virtual.isClosed(), "a direct run closes the terminal it created");
        }
    }

    private int runConsole(PmConsole console) {
        try {
            return console.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private OperatorContext context(VirtualScreenTerminal virtual, AtomicInteger creations) {
        var environment = new HashMap<>(runtime.environment());
        environment.put("LANG", "en_US.UTF-8");
        return new OperatorContext(
                            environment,
                            runtime.base().getParent(),
                            true,
                            ClientEnvironment.capture(environment),
                            () -> {
                                creations.incrementAndGet();
                                return virtual.terminal();
                            },
                            null);
    }

    private static void recordLiveRuntime(MockRuntime runtime) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow();
        Files.writeString(runtime.paths().runtimeRecord(),
                            "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}",
                            StandardCharsets.UTF_8);
    }

    private static boolean shows(VirtualScreenTerminal virtual, String text) {
        return virtual.screen().lines().stream().anyMatch(line -> line.contains(text));
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + AWAIT_MILLIS * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("the console did not reach the expected state in time");
            }
            LockSupport.parkNanos(20_000_000L);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }
}
