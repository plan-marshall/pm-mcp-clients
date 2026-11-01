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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.jline.terminal.Terminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * {@code Ctrl+C} at a prompt of a dialogue the full-screen views open ({@code CLI-CC-S-01}): the
 * two handlers no verb reaches on its own leave with the interrupt status of the exit-code table and
 * send nothing to the runtime. The interrupt is the terminal's {@code INT} signal, which is what
 * {@code Ctrl+C} raises; everything a verb reaches is driven on a real pseudo-terminal in
 * {@code transcript.InterruptDialogueIT}, where the process exit status is the assertion.
 */
@DisplayName("Ctrl+C at a view-only dialogue leaves with 130 and sends nothing")
@EnabledOnOs({OS.MAC, OS.LINUX})
class InterruptExitCodeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(20);
    private static final String PAUSE_PROMPT = "Where does p hold?";
    private static final String HOLD_CAUSE = "the worker stopped";

    private static final String PLAN_ACTIONS = """
                        {"subject_id":"p","subject_kind":"plan","state":"running","epic_id":"e",
                         "actions":[{"action":"pause","available":true},{"action":"cancel","available":true}]}
                        """;

    private static final String STALLED_HOLD = """
                        [{"hold_id":"H-1","subject_id":"p","subject_kind":"plan","kind":"stalled",
                          "cause":"the worker stopped","attempts":2,"acts":["retry","recycle","end_plan"]}]
                        """;

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private StringWriter out;
    private OperatorContext context;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        virtual = VirtualScreenTerminal.create(160, 40);
        out = new StringWriter();
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        context = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        virtual.close();
        fixture.close();
    }

    @Test
    @DisplayName("the work action dialogue: the pause prompt interrupted leaves with 130")
    void workActionDialogueInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, PLAN_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt(PAUSE_PROMPT, () -> WorkActionDialogue.run(context, new PrintWriter(out),
                            "plan", "p", "pause", false, null, null));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the hold dialogue: the act prompt interrupted leaves with 130")
    void holdDialogueInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/holds")) {
                response.json(200, STALLED_HOLD);
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt(HOLD_CAUSE, () -> HoldDialogue.run(context, new PrintWriter(out), "p", null,
                            false));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    /**
     * Runs a dialogue and raises the terminal's interrupt as soon as its prompt is drawn, so that the
     * reading thread is interrupted where the operator would press {@code Ctrl+C}. The raise repeats
     * until the dialogue returns, because the reader installs its own handler only once it reads.
     *
     * @param marker   the text whose appearance means the dialogue is at its prompt
     * @param dialogue the dialogue to run
     * @return the exit code the dialogue returned
     * @throws Exception if the dialogue fails or never reaches its prompt
     */
    private int interruptedAt(String marker, Callable<Integer> dialogue) throws Exception {
        var code = new AtomicReference<Integer>();
        var failure = new AtomicReference<Throwable>();
        var reader = Thread.ofPlatform().name("dialogue-under-test").start(() -> {
            try {
                code.set(dialogue.call());
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        var deadline = System.nanoTime() + TIMEOUT.toNanos();
        var prompted = false;
        while (reader.isAlive() && System.nanoTime() < deadline) {
            if (out.toString().contains(marker)) {
                prompted = true;
                virtual.terminal().raise(Terminal.Signal.INT);
            }
            LockSupport.parkNanos(POLL.toNanos());
        }
        reader.join(TimeUnit.SECONDS.toMillis(1));
        assertFalse(reader.isAlive(), "the dialogue did not leave after the interrupt: " + out);
        assertNull(failure.get(), "the dialogue failed: " + failure.get());        if (!prompted) {
            fail(marker + " was never drawn, the interrupt was not sent: " + out);
        }
        return code.get();
    }

    /**
     * Asserts that nothing but a read reached the runtime: the dialogues ask the runtime what the
     * state offers, and the interrupt must leave before anything is sent.
     *
     * @param server the fake runtime
     */
    private static void assertNoAct(FakeRuntimeServer server) {
        var sent = server.requests().stream().filter(request -> !isRead(request.method())).toList();
        assertTrue(sent.isEmpty(), "expected no act at the runtime, got " + sent);
    }

    private static boolean isRead(String method) {
        var upper = method.toUpperCase(Locale.ROOT);
        return "GET".equals(upper) || "HEAD".equals(upper) || "OPTIONS".equals(upper);
    }
}
