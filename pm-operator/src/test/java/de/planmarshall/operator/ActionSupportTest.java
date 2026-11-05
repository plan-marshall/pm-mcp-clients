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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.terminal.Terminal;

import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@DisplayName("ActionSupport")
@EnableGeneratorController
@EnabledOnOs({OS.MAC, OS.LINUX})
class ActionSupportTest {

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private CommandSpec spec;
    private OperatorContext context;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
        var cmd = new CommandLine(CommandSpec.create());
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        spec = cmd.getCommandSpec();
        context = new OperatorContext(fixture.environment(), fixture.base().getParent());
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        return fixture.start(handler);
    }

    private ActionSupport.TerminationOptions terminationOptions(String id, String reason, boolean keepPr, boolean noInput) {
        var opts = new ActionSupport.TerminationOptions();
        opts.id = id;
        opts.reason = reason;
        opts.keepPr = keepPr;
        opts.noInput = noInput;
        return opts;
    }

    @Test
    @DisplayName("an interrupt inside a dialogue the view opened leaves back with 0, every other code stands")
    void theLeaveBackMapping() {
        var other = Generators.integers(0, 129).next();

        assertAll(
                            () -> assertEquals(0, ActionSupport.leaveBack(StartPrompt.EXIT_INTERRUPTED),
                                                "the interrupt is the view's own leave-back"),
                            () -> assertEquals(other, ActionSupport.leaveBack(other),
                                                "a code that is not an interrupt is kept")
        );
    }

    @Test
    @DisplayName("chat follow-up in console maps interrupt to 0 at its call site")
    void chatFollowUpInterruptLeavesBack() throws Exception {
        var entry = new EntryResource("ent-1", EntryKind.PLAN, "pm-mcp-clients", StartState.SOURCES,
                            null, null, List.of(), List.of(), true, true, null, null, List.of(), null);
        var startState = StartStateResource.of(entry, null, null, null, null, List.of()).toJson();

        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/chat".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"chat_id":"c-1","subject_id":"p-1","subject_kind":"plan",
                                     "role":"replan","scope":"archive","state":"open","turns":[],
                                     "proposal":{"summary":"Follow-up plan"}}
                                    """);
            } else if ("/api/v1/projects".equals(request.target())) {
                response.json(200, "{\"projects\":[{\"project_id\":\"pm-mcp-clients\"}]}");
            } else if ("/api/v1/projects/pm-mcp-clients/entries".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, startState);
            } else {
                response.send(404, null, "");
            }
        });

        var virtual = VirtualScreenTerminal.create(80, 24);
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var activeContext = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
        activeContext.setConsoleActive(true);

        var code = interruptedAt(virtual, "Proposed follow-up", "Sources",
                            () -> ActionSupport.chat(activeContext, spec, "plan", "p-1", null));
        assertEquals(0, code, "the follow-up interrupt leaves back with 0");
    }

    @Test
    @DisplayName("OperatorActions.chatAboutEnded maps interrupt to 0 at its call site")
    void chatAboutEndedInterruptLeavesBack() throws Exception {
        var entry = new EntryResource("ent-1", EntryKind.PLAN, "pm-mcp-clients", StartState.SOURCES,
                            null, null, List.of(), List.of(), true, true, null, null, List.of(), null);
        var startState = StartStateResource.of(entry, null, null, null, null, List.of()).toJson();

        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/chat".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"chat_id":"c-1","subject_id":"p-1","subject_kind":"plan",
                                     "role":"replan","scope":"archive","state":"open","turns":[],
                                     "proposal":{"summary":"Follow-up plan"}}
                                    """);
            } else if ("/api/v1/projects".equals(request.target())) {
                response.json(200, "{\"projects\":[{\"project_id\":\"pm-mcp-clients\"}]}");
            } else if ("/api/v1/projects/pm-mcp-clients/entries".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, startState);
            } else {
                response.send(404, null, "");
            }
        });

        var virtual = VirtualScreenTerminal.create(80, 24);
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var activeContext = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);

        var code = interruptedAt(virtual, "Proposed follow-up", "Sources",
                            () -> OperatorActions.chatAboutEnded(activeContext, new PrintWriter(out, true), "plan", "p-1",
                                                "pm-mcp-clients", "en"));
        assertEquals(0, code, "the ended work chat interrupt leaves back with 0");
    }

    private int interruptedAt(VirtualScreenTerminal virtual, String proposalMarker,
                        String interruptMarker, Handler handler) throws Exception {
        var code = new AtomicInteger(-1);
        var failure = new AtomicReference<Throwable>();
        var reader = Thread.ofPlatform().name("handler-under-test").start(() -> {
            try {
                code.set(handler.run());
            }
            /*TODO: Catch specific not Throwable. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (Throwable failed) {
                failure.set(failed);
            }
        });
        try {
            var deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            var proposalAccepted = false;
            var interrupted = false;
            while (reader.isAlive() && System.nanoTime() < deadline) {
                var screen = String.join("\n", virtual.screen().lines());
                var text = out.toString() + "\n" + screen;
                if (!proposalAccepted && text.contains(proposalMarker)) {
                    virtual.type("\n");
                    proposalAccepted = true;
                }
                if (proposalAccepted && !interrupted && text.contains(interruptMarker)) {
                    interrupted = true;
                    virtual.terminal().raise(Terminal.Signal.INT);
                    virtual.type("\u0003");
                }
                LockSupport.parkNanos(Duration.ofMillis(20).toNanos());
            }
            reader.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(reader.isAlive(), "the dialogue did not leave after the interrupt: " + out);
            assertNull(failure.get(), "the dialogue failed: " + failure.get());
            assertTrue(interrupted, interruptMarker + " was never reached: " + out);
            return code.get();
        } finally {
            virtual.close();
            reader.interrupt();
        }
    }

    @FunctionalInterface
    private interface Handler {
        int run() throws Exception;
    }

    @Test
    @DisplayName("endWork requires reason when noInput is true")
    void endWorkRequiresReason() throws Exception {
        var opts1 = terminationOptions("p-1", null, false, true);
        var code = ActionSupport.endWork(context, spec, "plan", opts1, ActionSupport.CANCEL);
        assertEquals(3, code);
        assertTrue(out.toString().contains("Cancellation reason required (--reason)"));

        out.getBuffer().setLength(0);
        var opts2 = terminationOptions("e-1", "", false, true);
        var abandonCode = ActionSupport.endWork(context, spec, "epic", opts2, "abandon");
        assertEquals(3, abandonCode);
        assertTrue(out.toString().contains("Abandon reason required (--reason)"));
    }

    @Test
    @DisplayName("endWork sends the operator's reason and close_pr=true on success")
    void endWorkDefaultReasonSuccess() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/cancel".equals(request.target()) && "POST".equals(request.method())) {
                assertTrue(request.bodyText().contains("\"reason\":\"the approach is replaced\""));
                assertTrue(request.bodyText().contains("\"close_pr\":true"));
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        var opts = terminationOptions("p-1", "the approach is replaced", false, false);
        var code = ActionSupport.endWork(context, spec, "plan", opts, ActionSupport.CANCEL);
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ p-1 cancelled and archived"));
    }

    @Test
    @DisplayName("endWork sends custom reason and keepPr")
    void endWorkCustomReasonAndKeepPr() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/epics/e-1/abandon".equals(request.target()) && "POST".equals(request.method())) {
                assertTrue(request.bodyText().contains("\"close_pr\":false"));
                assertTrue(request.bodyText().contains("\"reason\":\"no longer relevant\""));
                response.send(202, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        var opts = terminationOptions("e-1", "no longer relevant", true, false);
        var code = ActionSupport.endWork(context, spec, "epic", opts, "abandon");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ e-1 abandoned and archived"));
    }

    @Test
    @DisplayName("endWork throws OperatorRefusedException on HTTP error")
    void endWorkRefused() throws Exception {
        runtime((request, response) -> response.json(500, "{\"code\":\"server_error\"}"));

        var opts = terminationOptions("p-1", "test", false, false);
        assertThrows(OperatorRefusedException.class, () ->
                            ActionSupport.endWork(context, spec, "plan", opts, ActionSupport.CANCEL));
    }

    @Test
    @DisplayName("chat posts message when provided")
    void chatPostMessage() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/chat".equals(request.target()) && "POST".equals(request.method())) {
                assertTrue(request.bodyText().contains("\"message\":\"hello plan\""));
                response.json(200, "{\"status\":\"delivered\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = ActionSupport.chat(context, spec, "plan", "p-1", "hello plan");
        assertEquals(0, code);
        assertTrue(out.toString().contains("delivered"));
    }

    @Test
    @DisplayName("chat fetches history when message is null or blank")
    void chatGetHistory() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/epics/e-1/chat".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"chat_id":"C-1","subject_id":"e-1","subject_kind":"epic",
                                     "role":"decompose","scope":"epic","state":"turn",
                                     "turns":[{"role":"operator","text":"hi","at":"2026-10-16T10:00:00Z",
                                               "untrusted":false},
                                              {"role":"decompose","text":"hello from the epic",
                                               "at":"2026-10-16T10:00:01Z","untrusted":false}]}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var code = ActionSupport.chat(context, spec, "epic", "e-1", "   ");
        assertEquals(0, code);
        assertTrue(out.toString().contains("hello from the epic"), out.toString());
    }

    @Test
    @DisplayName("chat throws on failure")
    void chatThrowsOnFailure() throws Exception {
        runtime((request, response) -> response.json(404, "{\"code\":\"not_found\"}"));

        assertThrows(OperatorRefusedException.class, () ->
                            ActionSupport.chat(context, spec, "plan", "p-1", "hello"));
        assertThrows(OperatorRefusedException.class, () ->
                            ActionSupport.chat(context, spec, "plan", "p-1", null));
    }

    @Test
    @DisplayName("printWorkList formats empty and populated lists")
    void printWorkList() {
        var pw = new PrintWriter(out);
        ActionSupport.printWorkList(pw, "Plans", "phase", List.of());
        pw.flush();
        assertTrue(out.toString().contains("No plans running."));

        out.getBuffer().setLength(0);
        ActionSupport.printWorkList(pw, "Epics", "state", List.of(
                            Map.of("id", "epic-1", "state", "RUNNING", "title", "Refactor Auth"),
                            Map.of("id", "epic-2", "state", "PAUSED"),
                            "simple-string-item"
        ));
        pw.flush();
        var result = out.toString();
        assertTrue(result.contains("Epics"));
        assertTrue(result.contains("epic-1"));
        assertTrue(result.contains("Refactor Auth"));
        assertTrue(result.contains("epic-2"));
        assertTrue(result.contains("simple-string-item"));
    }
}
