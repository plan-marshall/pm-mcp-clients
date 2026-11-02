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
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.terminal.Terminal;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import de.planmarshall.api.host.HostDetection;
import de.planmarshall.api.resource.RoleDetailResource;
import de.planmarshall.api.resource.RoleQualificationState;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
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
import org.junit.jupiter.api.io.TempDir;

/**
 * The interrupt signal at the prompt of the handlers a verb can run on its own ({@code CLI-CC-S-01}):
 * the handler maps it to the exit-code table's {@code 130} and sends nothing. Each case raises the
 * terminal's {@code INT} signal — what {@code Ctrl+C} raises — at the prompt the handler draws; the
 * typed key itself is proven on a real pseudo-terminal in {@code transcript.InterruptDialogueIT},
 * {@code transcript.ViewDialogueInterruptIT} and {@code transcript.DirectDialogueInterruptIT}.
 *
 * <p>The view's other dialogues — the chat, a start, the settings and a confirmation — leave back to
 * the view as {@code q} does ({@code 04-console.adoc#console-dialogue-handoff}). A start and the
 * settings are cases here for their direct-verb path, where the interrupt is the exit-code table's
 * 130; the view's own mapping of them to "leave back" lives in {@code OverviewFullScreenApp} and
 * {@link WorkActionDialogue}, and the chat's and the confirmation's live there only.
 */
@DisplayName("an interrupt signal at a handler a verb runs leaves with 130 and sends nothing")
@EnableGeneratorController
@EnabledOnOs({OS.MAC, OS.LINUX})
class HandlerInterruptExitCodeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(20);
    /** The project the mock's {@code start-sources} scenario serves; a fixture fact, not input. */
    private static final String SCENARIO_PROJECT = "pm-mcp-clients";

    private final String plan = Generators.letterStrings(4, 10).next();

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private StringWriter out;
    private StringWriter err;
    private OperatorContext context;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        virtual = VirtualScreenTerminal.create(160, 40);
        out = new StringWriter();
        err = new StringWriter();
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "en_US.UTF-8");
        environment.put("PATH", tempDir.toString());
        context = new OperatorContext(environment, fixture.base().getParent(), true,
                            ClientEnvironment.capture(environment), virtual::terminal, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        virtual.close();
        fixture.close();
    }

    @Test
    @DisplayName("the starting dialogue: its sources prompt interrupted leaves with 130")
    void startingDialogueInterrupt() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.START_SOURCES).build())) {
            recordLiveRuntime(runtime);
            var environment = new HashMap<>(runtime.environment());
            environment.put("LANG", "en_US.UTF-8");
            environment.put("USER", "operator");
            var started = new OperatorContext(environment, runtime.base().getParent(), true,
                                ClientEnvironment.capture(environment), virtual::terminal, null);

            var code = interruptedAt("What does the plan start from?",
                                () -> PmOperator.execute(started, new PrintWriter(out),
                                                    new PrintWriter(err), "plan", "add", "--project", SCENARIO_PROJECT));

            assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
            assertTrue(startedNothing(runtime), "the interrupt must not start the plan: " + out);
        }
    }

    @Test
    @DisplayName("the settings dialogue: its group prompt interrupted leaves with 130")
    void settingsDialogueInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/settings")) {
                response.json(200, "{\"plan_id\":\"" + plan + "\",\"revision\":1,\"values\":[]}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt("Which settings of " + plan,
                            () -> PlanSettingsDialogue.run(context, new PrintWriter(out), plan, null, false));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the project selector: its prompt interrupted leaves with 130")
    void projectSelectorInterrupt() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, "{\"projects\":[{\"project_id\":\"alpha\"},{\"project_id\":\"beta\"}]}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt("Which project?",
                            () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                "plan", "pause", plan, "--project", "?"));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the automatic onboarding's confirmation interrupted leaves with 130")
    void onboardAutomaticConfirmationInterrupt() throws Exception {
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        var code = interruptedAt("Decide everything for me?",
                            () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                "onboard", "--auto"));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the credential confirmation interrupted leaves with 130")
    void credentialsConfirmationInterrupt() throws Exception {
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        var code = interruptedAtAfter("Store the new token?", "s3cret\n",
                            () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                "credentials", "set", "sonar"));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the role qualification's confirmation interrupted leaves with 130")
    void rolesQualificationConfirmationInterrupt() throws Exception {
        var detail = new RoleDetailResource("implementer", "implementer", "implementer role",
                            List.of("pm_wait", "pm_do", "edit"),
                            new RoleDetailResource.NarrowedEffort("high", "the harness narrows high to medium"),
                            new RoleDetailResource.Qualification(RoleQualificationState.QUALIFIED,
                                                "record valid", List.of("correctness")),
                            Map.of("source_set", "shipped", "class", "writer", "form", "fresh",
                                                "harness", "claude", "model", "opus-4.5", "effort", "high"));
        var server = fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/roles/implementer")) {
                response.json(200, detail.toJson());
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt("Run the qualification now?",
                            () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                "roles", "qualify", "implementer"));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the harness enrolment's confirmation interrupted leaves with 130")
    void enrolmentConfirmationInterrupt() throws Exception {
        var client = supportedClientWithProbe();
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        var code = interruptedAt("Record this harness enrolment?",
                            () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                "enrol-harness", client));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    @Test
    @DisplayName("the settings inside a work action leave back to the view, not with 130")
    void settingsInsideTheActionLeaveBack() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, "{\"subject_id\":\"" + plan + "\",\"subject_kind\":\"plan\","
                                    + "\"state\":\"running\",\"actions\":[{\"action\":\"settings\","
                                    + "\"available\":true}]}");
            } else if (request.target().endsWith("/settings")) {
                response.json(200, "{\"plan_id\":\"" + plan + "\",\"revision\":1,\"values\":[]}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = interruptedAt("Which settings of " + plan,
                            () -> WorkActionDialogue.run(context, new PrintWriter(out), "plan", plan,
                                                "settings", false, null, null));

        assertEquals(0, code, "the view's other dialogues leave back: " + out);
        assertNoAct(server);
    }

    @Test
    @DisplayName("the device revoke confirmation interrupted leaves with 130 and deletes nothing")
    void revokeConfirmationInterrupt() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{}"));

        var code = interruptedAt("Revoke?",
                            () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                "web", "devices", "revoke", "dev-1", "--interactive"));

        assertEquals(StartPrompt.EXIT_INTERRUPTED, code, out.toString());
        assertNoAct(server);
    }

    private static void assertNoAct(FakeRuntimeServer server) {
        var sent = server.requests().stream()
                            .filter(request -> !isRead(request.method()))
                            .toList();
        assertTrue(sent.isEmpty(), "expected no act at the runtime, got " + sent);
    }

    private static boolean isRead(String method) {
        var upper = method.toUpperCase(java.util.Locale.ROOT);
        return "GET".equals(upper) || "HEAD".equals(upper) || "OPTIONS".equals(upper);
    }

    private String supportedClientWithProbe() throws IOException {
        for (var client : HostDetection.clientIds()) {
            var host = HostDetection.host(client).orElse(null);
            if (host == null) {
                continue;
            }
            var probe = host.versionProbe();
            var executable = probe.isEmpty() ? client : probe.getFirst();
            var script = tempDir.resolve(executable);
            Files.writeString(script, "#!/bin/sh\necho '1.0.0'\n");
            assertTrue(script.toFile().setExecutable(true), "cannot make " + script + " executable");
            return client;
        }
        fail("no supported harness client carries a profile");
        return "";
    }

    /**
     * Runs a handler and raises the terminal's interrupt as soon as its prompt is drawn, repeating
     * the raise until the handler returns because the reader installs its handler only once it reads.
     *
     * @param marker  the text whose appearance means the handler is at its prompt
     * @param handler the handler to run
     * @return the exit code the handler returned
     * @throws Exception if the handler fails or never reaches its prompt
     */
    private int interruptedAt(String marker, Handler handler) throws Exception {
        return interruptedAtAfter(marker, "", handler);
    }

    private int interruptedAtAfter(String marker, String typedBefore, Handler handler) throws Exception {
        return interruptedAt(marker, typedBefore, handler, virtual.terminal());
    }

    private int interruptedAt(String marker, String typedBefore, Handler handler, Terminal target)
                        throws Exception {
        var code = new AtomicInteger(-1);
        var failure = new AtomicReference<IOException>();
        var reader = Thread.ofPlatform().name("handler-under-test").start(() -> {
            try {
                code.set(handler.run());
            } catch (IOException failed) {
                failure.set(failed);
            }
        });
        var deadline = System.nanoTime() + TIMEOUT.toNanos();
        var prompted = false;
        var typed = false;
        while (reader.isAlive() && System.nanoTime() < deadline) {
            if (!typed && !typedBefore.isEmpty() && out.toString().contains("Token (not shown")) {
                virtual.type(typedBefore);
                typed = true;
            }
            if (drawn(marker)) {
                prompted = true;
                // The prompt is on the terminal: the operator's Ctrl+C is both the signal and the
                // byte, and the reader answers whichever of the two its terminal forwards.
                target.raise(Terminal.Signal.INT);
                if (target == virtual.terminal()) {
                    virtual.type("\u0003");
                }
            }
            LockSupport.parkNanos(POLL.toNanos());
        }
        reader.join(TimeUnit.SECONDS.toMillis(1));
        assertFalse(reader.isAlive(), "the handler did not leave after the interrupt: " + out
                            + "\n" + String.join("\n", virtual.screen().lines()));
        assertNull(failure.get(), "the handler failed: " + failure.get());
        if (!prompted) {
            fail(marker + " was never drawn, the interrupt was not sent: " + out
                                + "\n" + String.join("\n", virtual.screen().lines()));
        }
        assertTrue(code.get() >= 0, "the handler returned no exit code: " + out);
        return code.get();
    }

    private boolean drawn(String marker) {
        return out.toString().contains(marker) || String.join("\n", virtual.screen().lines()).contains(marker);
    }

    /** A handler a verb runs, which leaves with its exit code. */
    @FunctionalInterface
    private interface Handler {
        /**
         * @return the exit code the handler leaves with
         * @throws IOException on transport failure
         */
        int run() throws IOException;
    }

    private static boolean startedNothing(MockRuntime runtime) throws IOException {
        var response = runtime.client().send(de.planmarshall.api.http.HttpRequest.get("/mock/acts"));
        var body = response.bodyText();
        // Opening the entry is the verb's own first act; starting the plan would be a second one.
        return !body.contains("/start");
    }

    private static void recordLiveRuntime(MockRuntime runtime) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow();
        Files.createDirectories(runtime.paths().stateDir());
        Files.writeString(runtime.paths().runtimeRecord(),
                            "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}");
    }
}
