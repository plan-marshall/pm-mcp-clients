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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.terminal.Terminal;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * {@code Ctrl+C} at a prompt of a dialogue the full-screen views open ({@code CLI-CC-S-01}): the
 * handlers a verb does not reach on its own leave with the interrupt status of the exit-code table
 * and send nothing to the runtime. The interrupt is the terminal's {@code INT} signal, which is what
 * {@code Ctrl+C} raises; the same cases are driven on a real pseudo-terminal in
 * {@code transcript.InterruptDialogueIT}, where the process exit status is the assertion.
 */
@EnableGeneratorController
@DisplayName("Ctrl+C at a dialogue leaves with 130 and sends nothing")
@EnabledOnOs({OS.MAC, OS.LINUX})
class InterruptExitCodeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(20);

    private final String subject = Generators.letterStrings(2, 8).next();
    private final String epic = Generators.letterStrings(2, 8).next();
    private final String hold = Generators.letterStrings(2, 8).next();
    private final String holdCause = Generators.letterStrings(8, 20).next();
    private final String onboarding =
                        Generators.letterStrings(8, 20).next();
    private final String statement = Generators.letterStrings(8, 20).next();
    private final String project = Generators.letterStrings(4, 10).next();
    private final String setupSummary = Generators.letterStrings(8, 20).next();

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
        fixture.writeToken(Generators.letterStrings(16, 32).next());
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

    private String pausePrompt() {
        return "Where does " + subject + " hold?";
    }

    private String planActions() {
        return "{\"subject_id\":\"" + subject + "\",\"subject_kind\":\"plan\",\"state\":\"running\","
                            + "\"epic_id\":\"" + epic + "\",\"actions\":[{\"action\":\"pause\","
                            + "\"available\":true},{\"action\":\"cancel\",\"available\":true}]}";
    }

    private String stalledHold() {
        return "[{\"hold_id\":\"" + hold + "\",\"subject_id\":\"" + subject + "\","
                            + "\"subject_kind\":\"plan\",\"kind\":\"stalled\",\"cause\":\"" + holdCause + "\","
                            + "\"attempts\":2,\"acts\":[\"retry\",\"recycle\",\"end_plan\"]}]";
    }

    private String review() {
        return "{\"review_id\":\"R-1\",\"epic_id\":\"" + epic + "\",\"register\":\"findings\","
                            + "\"state\":\"open\",\"decided\":0,\"total\":1,\"items\":[{\"item_id\":\"F-1\","
                            + "\"register\":\"findings\",\"statement\":\"" + statement + "\",\"rules\":[],"
                            + "\"origin\":\"landing\",\"trust\":\"trusted\",\"state\":\"open\"}]}";
    }

    private String onboardingConsent() {
        return "{\"scope\":\"machine\",\"state\":\"harness.consent\",\"summary\":\"" + onboarding + "\","
                            + "\"completed_steps\":1,\"auto\":false,\"links\":{\"yes\":{\"href\":\""
                            + "/api/v1/onboarding/harness/scan\",\"method\":\"POST\",\"preselected\":true}}}";
    }

    private String projectSetup() {
        return "{\"scope\":\"project\",\"state\":\"project.zzz\",\"kind\":\"operator_wait\",\"step\":3,"
                            + "\"total_steps\":10,\"summary\":\"" + setupSummary + "\",\"links\":{\"record\":"
                            + "{\"href\":\"/api/v1/projects/" + project + "/setup\",\"method\":\"POST\","
                            + "\"preselected\":true}}}";
    }

    @Test
    @DisplayName("the work action dialogue: the pause prompt interrupted leaves with 130")
    void workActionDialogueInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, planActions());
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt(pausePrompt(), () -> WorkActionDialogue.run(context, new PrintWriter(out),
                            "plan", subject, "pause", false, null, null));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the hold dialogue: the act prompt interrupted leaves with 130")
    void holdDialogueInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/holds")) {
                response.json(200, stalledHold());
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt(holdCause, () -> HoldDialogue.run(context, new PrintWriter(out), subject, null,
                            false));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the review dialogue: the decision prompt interrupted leaves with 130")
    void reviewDialogueInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().contains("/review")) {
                response.json(200, review());
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt(statement,
                            () -> ReviewDialogue.run(context, new PrintWriter(out), epic, "findings", false));

        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("plan pause: the point prompt interrupted leaves with 130")
    void planPauseInterrupt() throws Exception {
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        var code = interruptedAt(pausePrompt(), () -> PmOperator.execute(context, new PrintWriter(out),
                            new PrintWriter(new StringWriter()), "plan", "pause", subject));

        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("plan cancel: the end reason interrupted leaves with 130")
    void planCancelReasonInterrupt() throws Exception {
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        var code = interruptedAt("Reason", () -> PmOperator.execute(context, new PrintWriter(out),
                            new PrintWriter(new StringWriter()), "plan", "cancel", subject));

        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("onboard: the first state prompt interrupted leaves with 130")
    void onboardInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (OnboardCommand.STATE_PATH.equals(request.target())) {
                response.json(200, onboardingConsent());
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt("Look for harnesses on this machine?", () -> PmOperator.execute(context,
                            new PrintWriter(out), new PrintWriter(new StringWriter()), "onboard"));

        assertEquals(130, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("project setup: the state prompt interrupted leaves with 130")
    void projectSetupInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/setup")) {
                response.json(200, projectSetup());
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt(setupSummary, () -> PmOperator.execute(context, new PrintWriter(out),
                            new PrintWriter(new StringWriter()), "project", "setup", project));

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
    private int interruptedAt(String marker, Dialogue dialogue) throws Exception {
        var code = new AtomicInteger(-1);
        var failure = new AtomicReference<IOException>();
        var reader = Thread.ofPlatform().name("dialogue-under-test").start(() -> {
            try {
                code.set(dialogue.run());
            } catch (IOException failed) {
                failure.set(failed);
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
        assertNull(failure.get(), "the dialogue failed: " + failure.get());
        if (!prompted) {
            fail(marker + " was never drawn, the interrupt was not sent: " + out);
        }
        assertTrue(code.get() >= 0, "the dialogue returned no exit code: " + out);
        return code.get();
    }

    /** A dialogue of the view, which reads the runtime and leaves with its exit code. */
    @FunctionalInterface
    private interface Dialogue {
        /**
         * @return the exit code the dialogue leaves with
         * @throws IOException on transport failure
         */
        int run() throws IOException;
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
